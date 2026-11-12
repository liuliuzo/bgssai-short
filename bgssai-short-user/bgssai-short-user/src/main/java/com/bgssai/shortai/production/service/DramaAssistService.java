package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.aimodel.PipelineReadiness;
import com.bgssai.shortai.aimodel.ProductModelAlias;
import com.bgssai.shortai.aiprovider.AiProvider;
import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.aiprovider.AiProviderRequest;
import com.bgssai.shortai.aiprovider.AiProviderResult;
import com.bgssai.shortai.aiprovider.TokenHubClientSupport;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.ProductionDtos;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.production.support.TaskPrices;
import com.bgssai.shortai.quota.service.QuotaConsumeService;

/**
 * Sync multi-expert drama assist over the existing CN TEXT channel.
 * Advice only writes back after an explicit confirm call.
 */
@Service
public class DramaAssistService {

    private static final Logger log = LoggerFactory.getLogger(DramaAssistService.class);

    private static final Set<String> ROLES = Set.of("SCRIPT", "DIRECTOR", "VISUAL", "EDITOR");
    private static final Set<String> APPLY_TARGETS = Set.of("shot_desc", "dialogue_text", "extra_prompt", "none");

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private AiModelResolveService aiModelResolveService;
    @Autowired
    private PipelineReadiness pipelineReadiness;
    @Autowired
    private AiProviderRegistry aiProviderRegistry;
    @Autowired
    private QuotaConsumeService quotaConsumeService;
    @Autowired
    private ShortCharacterMapper shortCharacterMapper;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShotService shotService;

    public ApiResponse advise(ProductionDtos.DramaAssistReq req) {
        if (req == null || req.getWorkId() == null || req.getWorkId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "work_id required");
        }
        String role = normalizeRole(req.getRole());
        String question = req.getQuestion() == null ? "" : req.getQuestion().trim();
        if (question.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "question required");
        }
        if (question.length() > 2000) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "question too long");
        }

        ShortWork work = workAccessService.requireWork(req.getWorkId());
        ShortEpisode episode = null;
        if (req.getEpisodeId() != null && !req.getEpisodeId().isBlank()) {
            episode = workAccessService.requireEpisode(req.getEpisodeId());
            if (!work.getWorkId().equals(episode.getWorkId())) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "episode_id not in work");
            }
        }
        ShortShot shot = null;
        if (req.getShotId() != null && !req.getShotId().isBlank()) {
            shot = requireOwnedShot(req.getShotId(), work.getWorkId());
        }

        pipelineReadiness.assertReady("TEXT", req.getModelCode());
        AiModelConfig model = aiModelResolveService.resolve("TEXT", req.getModelCode());
        int estimate = aiModelResolveService.costCent(model, 1);
        if (estimate <= 0) {
            estimate = TaskPrices.cost("TEXT");
        }
        quotaConsumeService.assertAffordable(work.getUserId(), estimate);

        Map<String, Object> context = buildContext(work, episode, shot);
        String prompt = buildPrompt(role, question, context);

        AiProviderRequest request = new AiProviderRequest();
        request.setAbilityType("TEXT");
        request.setOperation("DRAMA_ASSIST");
        request.setModel(model);
        request.setPrompt(prompt);
        request.getParams().put("role", role);
        request.getParams().put("work_id", work.getWorkId());

        AiProviderResult result;
        try {
            AiProvider provider = aiProviderRegistry.resolve(model);
            result = provider.execute(request);
        } catch (BizException ex) {
            throw new BizException(ex.getCode(), TokenHubClientSupport.userFacingMessage(ex.getMessage()));
        } catch (Exception ex) {
            log.warn("drama assist model call failed workId={} role={}", work.getWorkId(), role, ex);
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    TokenHubClientSupport.userFacingMessage(ex.getMessage()));
        }

        String raw = result == null ? "" : (result.getTextContent() == null ? "" : result.getTextContent().trim());
        if (raw.isBlank()) {
            throw new BizException(CommonErrorCode.UPSTREAM_ERROR,
                    TokenHubClientSupport.userFacingMessage("empty chat content"));
        }

        int usage = result.getUsageAmount() == null ? 1 : Math.max(1, result.getUsageAmount());
        int cost = aiModelResolveService.costCent(model, usage);
        if (cost <= 0) {
            cost = TaskPrices.cost("TEXT") * usage;
        }
        quotaConsumeService.consume(work.getUserId(), cost,
                "drama-assist:" + work.getWorkId() + ":" + System.currentTimeMillis());

        Map<String, Object> structured = parseStructured(raw, role);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("role", role);
        out.put("role_name", roleName(role));
        out.put("advice_text", ShortJson.text(structured.get("advice_text")));
        out.put("summary", ShortJson.text(structured.get("summary")));
        out.put("action_list", structured.get("action_list"));
        out.put("apply_target", ShortJson.text(structured.get("apply_target")));
        out.put("apply_text", ShortJson.text(structured.get("apply_text")));
        out.put("context_used", contextSummary(context));
        out.put("cost_cent", cost);
        ProductModelAlias.hideStoredModel(out, "TEXT", model.getModelCode());
        return Responses.ok(out);
    }

    public ApiResponse apply(ProductionDtos.DramaAssistApplyReq req) {
        if (req == null || req.getShotId() == null || req.getShotId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "shot_id required");
        }
        String target = req.getApplyTarget() == null ? "" : req.getApplyTarget().trim().toLowerCase(Locale.ROOT);
        if (!APPLY_TARGETS.contains(target) || "none".equals(target)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "apply_target must be shot_desc/dialogue_text/extra_prompt");
        }
        String text = req.getApplyText() == null ? "" : req.getApplyText().trim();
        if (text.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "apply_text required");
        }
        ShortWork work = workAccessService.requireWork(req.getWorkId());
        requireOwnedShot(req.getShotId(), work.getWorkId());

        ProductionDtos.ShotReq shotReq = new ProductionDtos.ShotReq();
        shotReq.setShotId(req.getShotId());
        if ("shot_desc".equals(target)) {
            shotReq.setShotDesc(text);
        } else if ("dialogue_text".equals(target)) {
            shotReq.setDialogueText(text);
        } else if ("extra_prompt".equals(target)) {
            shotReq.setExtraPrompt(text);
        }
        ApiResponse updated = shotService.update(shotReq);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shot_id", req.getShotId());
        result.put("apply_target", target);
        result.put("applied_flag", 1);
        result.put("update_result", updated == null ? null : updated.getResult());
        return Responses.ok(result);
    }

    static String normalizeRole(String role) {
        String value = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!ROLES.contains(value)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "role must be SCRIPT/DIRECTOR/VISUAL/EDITOR");
        }
        return value;
    }

    static String roleName(String role) {
        return switch (role) {
            case "SCRIPT" -> "编剧专家";
            case "DIRECTOR" -> "分镜导演";
            case "VISUAL" -> "视觉专家";
            case "EDITOR" -> "剪辑顾问";
            default -> role;
        };
    }

    static String roleDuty(String role) {
        return switch (role) {
            case "SCRIPT" -> "负责对白张力、反转、集尾钩子与去废话，输出可直接替换的台词建议。";
            case "DIRECTOR" -> "负责景别节奏、机位与相邻镜头衔接，输出可执行的分镜调整。";
            case "VISUAL" -> "负责光影、构图与画面提示词参数，输出可写入镜头额外提示的关键词。";
            case "EDITOR" -> "负责时长卡点、BGM/音效进出与导出工程建议，不编造已生成素材。";
            default -> "";
        };
    }

    Map<String, Object> buildContext(ShortWork work, ShortEpisode episode, ShortShot shot) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("work_id", work.getWorkId());
        ctx.put("work_title", work.getWorkTitle());
        ctx.put("one_line_pitch", work.getOneLinePitch());
        ctx.put("genre", work.getGenre());
        ctx.put("episode_count", work.getEpisodeCount());
        if (episode != null) {
            ctx.put("episode_id", episode.getEpisodeId());
            ctx.put("episode_no", episode.getEpisodeNo());
            ctx.put("episode_title", episode.getEpisodeTitle());
            ctx.put("episode_synopsis", episode.getEpisodeSynopsis());
        }
        List<String> characters = new ArrayList<>();
        ShortCharacterExample cx = new ShortCharacterExample();
        cx.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        cx.setOrderByClause("id asc");
        List<ShortCharacter> rows = shortCharacterMapper.selectByExample(cx);
        int limit = Math.min(12, rows == null ? 0 : rows.size());
        for (int i = 0; i < limit; i++) {
            ShortCharacter c = rows.get(i);
            characters.add(c.getCharacterName() == null ? c.getCharacterId() : c.getCharacterName());
        }
        ctx.put("character_names", characters);
        if (shot != null) {
            Map<String, Object> shotMap = new LinkedHashMap<>();
            shotMap.put("shot_id", shot.getShotId());
            shotMap.put("shot_no", shot.getShotNo());
            shotMap.put("shot_desc", shot.getShotDesc());
            shotMap.put("dialogue_text", shot.getDialogueText());
            shotMap.put("shot_size", shot.getShotSize());
            shotMap.put("camera_move", shot.getCameraMove());
            shotMap.put("image_prompt", shot.getImagePrompt());
            shotMap.put("extra_prompt", shot.getExtraPrompt());
            ctx.put("current_shot", shotMap);
            if (episode != null) {
                ctx.put("nearby_shots", nearbyShots(episode.getEpisodeId(), shot.getShotNo()));
            }
        } else if (episode != null) {
            ctx.put("nearby_shots", nearbyShots(episode.getEpisodeId(), null));
        }
        return ctx;
    }

    private List<Map<String, Object>> nearbyShots(String episodeId, Integer centerNo) {
        ShortShotExample sx = new ShortShotExample();
        sx.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        sx.setOrderByClause("shot_no asc");
        List<ShortShot> shots = shortShotMapper.selectByExample(sx);
        List<Map<String, Object>> out = new ArrayList<>();
        if (shots == null) {
            return out;
        }
        for (ShortShot s : shots) {
            if (centerNo != null && s.getShotNo() != null
                    && Math.abs(s.getShotNo() - centerNo) > 2) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("shot_no", s.getShotNo());
            item.put("shot_desc", trim(s.getShotDesc(), 120));
            item.put("dialogue_text", trim(s.getDialogueText(), 80));
            out.add(item);
            if (centerNo == null && out.size() >= 6) {
                break;
            }
        }
        return out;
    }

    static String buildPrompt(String role, String question, Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是短剧制作平台的").append(roleName(role)).append("。职责：").append(roleDuty(role)).append('\n');
        sb.append("只依据给定作品上下文回答，禁止编造不存在的成片/素材状态。\n");
        sb.append("必须只返回 JSON 对象，不要 Markdown。字段：\n");
        sb.append("advice_text(string 完整建议)、summary(string 一句话)、action_list(string 数组，1-5 条可执行动作)、");
        sb.append("apply_target(shot_desc|dialogue_text|extra_prompt|none)、apply_text(string，可写回正文；无写回则空字符串)。\n");
        sb.append("作品上下文 JSON：").append(ShortJson.toJson(context)).append('\n');
        sb.append("用户问题：").append(question);
        return sb.toString();
    }

    static Map<String, Object> parseStructured(String raw, String role) {
        Map<String, Object> parsed = ShortJson.map(raw);
        if (parsed.isEmpty()) {
            String stripped = raw.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
            parsed = ShortJson.map(stripped);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        String advice = ShortJson.text(parsed.get("advice_text"));
        if (advice.isBlank()) {
            advice = raw;
        }
        out.put("advice_text", advice);
        out.put("summary", firstNonBlank(ShortJson.text(parsed.get("summary")), trim(advice, 80)));
        List<Object> actions = new ArrayList<>();
        Object list = parsed.get("action_list");
        if (list instanceof List<?> rawList) {
            for (Object item : rawList) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    actions.add(String.valueOf(item).trim());
                }
                if (actions.size() >= 5) {
                    break;
                }
            }
        }
        out.put("action_list", actions);
        String target = ShortJson.text(parsed.get("apply_target")).trim().toLowerCase(Locale.ROOT);
        if (!APPLY_TARGETS.contains(target)) {
            target = defaultApplyTarget(role);
        }
        String applyText = ShortJson.text(parsed.get("apply_text")).trim();
        if (applyText.isBlank() && !"none".equals(target)) {
            applyText = advice;
        }
        if ("none".equals(target)) {
            applyText = "";
        }
        out.put("apply_target", target);
        out.put("apply_text", applyText);
        return out;
    }

    private static String defaultApplyTarget(String role) {
        return switch (role) {
            case "SCRIPT" -> "dialogue_text";
            case "DIRECTOR" -> "shot_desc";
            case "VISUAL" -> "extra_prompt";
            default -> "none";
        };
    }

    private static Map<String, Object> contextSummary(Map<String, Object> context) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("work_id", context.get("work_id"));
        summary.put("episode_id", context.get("episode_id"));
        Object shot = context.get("current_shot");
        if (shot instanceof Map<?, ?> shotMap) {
            summary.put("shot_id", shotMap.get("shot_id"));
        }
        Object names = context.get("character_names");
        summary.put("character_count", names instanceof List<?> list ? list.size() : 0);
        return summary;
    }

    private ShortShot requireOwnedShot(String shotId, String workId) {
        String userId = workAccessService.requireUser();
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(shotId.trim()).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(ex);
        if (shot == null || !workId.equals(shot.getWorkId()) || !userId.equals(shot.getUserId())) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "shot_id not in work");
        }
        return shot;
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b == null ? "" : b;
    }
}
