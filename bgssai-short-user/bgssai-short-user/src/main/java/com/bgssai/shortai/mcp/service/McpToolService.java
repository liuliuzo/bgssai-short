package com.bgssai.shortai.mcp.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.common.exception.UnauthorizedException;
import com.bgssai.shortai.health.dto.LivenessReport;
import com.bgssai.shortai.health.dto.ReadinessComponent;
import com.bgssai.shortai.health.dto.ReadinessReport;
import com.bgssai.shortai.health.service.QueryLivenessService;
import com.bgssai.shortai.health.service.QueryReadinessService;
import com.bgssai.shortai.loginlog.dto.LoginLogItem;
import com.bgssai.shortai.loginlog.service.ListLoginLogsService;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortScriptMapper;
import com.bgssai.shortai.mapper.ShortScriptVersionMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortScript;
import com.bgssai.shortai.model.ShortScriptExample;
import com.bgssai.shortai.model.ShortScriptVersion;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;
import com.bgssai.shortai.production.dto.ProductionDtos.FilmPublishToMediaReq;
import com.bgssai.shortai.production.service.FilmPublishToMediaService;
import com.bgssai.shortai.production.support.RhythmChecker;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class McpToolService {

    private static final Logger log = LoggerFactory.getLogger(McpToolService.class);

    public static final class ToolResult {
        private final String text;
        private final boolean error;

        private ToolResult(String text, boolean error) {
            this.text = text;
            this.error = error;
        }

        static ToolResult ok(String text) {
            return new ToolResult(text, false);
        }

        static ToolResult error(String text) {
            return new ToolResult(text, true);
        }

        public String text() {
            return text;
        }

        public boolean isError() {
            return error;
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final ListLoginLogsService listLoginLogsService;
    private final QueryLivenessService queryLivenessService;
    private final QueryReadinessService queryReadinessService;
    private final ShortWorkMapper shortWorkMapper;
    private final ShortEpisodeMapper shortEpisodeMapper;
    private final ShortScriptMapper shortScriptMapper;
    private final ShortScriptVersionMapper shortScriptVersionMapper;
    private final ShortShotMapper shortShotMapper;
    @Autowired
    private McpProductionService production;
    private final FilmPublishToMediaService filmPublishToMediaService;

    public McpToolService(ListLoginLogsService listLoginLogsService,
            QueryLivenessService queryLivenessService,
            QueryReadinessService queryReadinessService) {
        this(listLoginLogsService, queryLivenessService, queryReadinessService,
                null, null, null, null, null, null, null);
    }

    @Autowired
    public McpToolService(ListLoginLogsService listLoginLogsService,
            QueryLivenessService queryLivenessService,
            QueryReadinessService queryReadinessService,
            ShortWorkMapper shortWorkMapper,
            ShortEpisodeMapper shortEpisodeMapper,
            ShortScriptMapper shortScriptMapper,
            ShortScriptVersionMapper shortScriptVersionMapper,
            ShortShotMapper shortShotMapper,
            ShortAiTaskMapper shortAiTaskMapper,
            FilmPublishToMediaService filmPublishToMediaService) {
        this.listLoginLogsService = listLoginLogsService;
        this.queryLivenessService = queryLivenessService;
        this.queryReadinessService = queryReadinessService;
        this.shortWorkMapper = shortWorkMapper;
        this.shortEpisodeMapper = shortEpisodeMapper;
        this.shortScriptMapper = shortScriptMapper;
        this.shortScriptVersionMapper = shortScriptVersionMapper;
        this.shortShotMapper = shortShotMapper;
        this.filmPublishToMediaService = filmPublishToMediaService;
    }

    public boolean hasTool(String name) {
        if (name == null) {
            return false;
        }
        return switch (name) {
            case "list_login_logs", "list_my_login_logs",
                    "get_liveness", "health_live",
                    "get_readiness", "health_ready",
                    "list_works", "get_work", "create_work",
                    "list_episodes", "save_episode_script",
                    "list_shots", "submit_render_task",
                    "publish_episode_to_media" -> true;
            default -> McpProductionService.hasTool(name);
        };
    }

    public ArrayNode listToolDefinitions() {
        ArrayNode tools = mapper.createArrayNode();
        tools.add(tool("list_login_logs", "列出当前用户本人登录日志（分页）。身份取自令牌，勿传 user_id。",
                schema(List.of(),
                        prop("page_num", "integer", "页码，从 1 开始，默认 1"),
                        prop("page_size", "integer", "每页条数，默认 20，最大 50"))));
        tools.add(tool("get_liveness", "进程存活探针（不触碰数据库）。", schema(List.of())));
        tools.add(tool("get_readiness", "就绪探针（critical 组件）。", schema(List.of())));
        tools.add(tool("list_works", "列出当前用户的短剧作品列表（分页）。身份取自令牌，仅返回本人作品。",
                schema(List.of(),
                        prop("page_num", "integer", "页码，从 1 开始，默认 1"),
                        prop("page_size", "integer", "每页条数，默认 20，最大 50"),
                        prop("work_status", "string", "可选作品状态过滤（DRAFT/PRODUCING/COMPLETED/ARCHIVED）"),
                        prop("genre", "string", "可选题材编码过滤"),
                        prop("keyword", "string", "可选剧名关键词过滤"))));
        tools.add(tool("get_work", "查询当前用户指定短剧作品详情及分集概览。仅允许访问本人作品。",
                schema(List.of("work_id"),
                        prop("work_id", "string", "短剧作品 ID（必填）"))));
        tools.add(tool("create_work", "为当前用户创建一部短剧作品并初始化分集草稿。",
                schema(List.of("work_title", "one_line_pitch"),
                        prop("work_title", "string", "短剧名称（必填）"),
                        prop("one_line_pitch", "string", "一句话核心卖点/故事梗概（必填）"),
                        prop("genre", "string", "题材编码，默认 REVENGE"),
                        prop("episode_count", "integer", "总集数（1-80），默认 24"),
                        prop("episode_duration_sec", "integer", "单集目标时长秒数（60/90/120），默认 90"),
                        prop("visual_style", "string", "视觉风格编码，默认 MODERN_URBAN"))));
        tools.add(tool("list_episodes", "列出当前用户指定短剧作品下的分集列表（分页）。仅允许访问本人作品。",
                schema(List.of("work_id"),
                        prop("work_id", "string", "短剧作品 ID（必填）"),
                        prop("page_num", "integer", "页码，从 1 开始，默认 1"),
                        prop("page_size", "integer", "每页条数，默认 20，最大 50"))));
        tools.add(tool("save_episode_script", "创建或更新当前用户指定分集的剧本内容与分集概要。仅允许操作本人分集。",
                schema(List.of("episode_id"),
                        prop("episode_id", "string", "分集 ID（必填）"),
                        prop("work_id", "string", "可选作品 ID（传入时校验与分集归属一致）"),
                        prop("episode_title", "string", "可选分集标题"),
                        prop("episode_synopsis", "string", "可选本集剧情梗概"),
                        prop("ending_hook", "string", "可选结尾悬念钩子"),
                        prop("script_content", "string", "可选剧本文本内容（未传 scene_list 时自动转为标准场次结构）"),
                        McpProductionSchemas.sceneList())));
        tools.add(tool("list_shots", "列出当前用户指定分集下的分镜列表（分页）。仅允许访问本人分集。",
                schema(List.of("episode_id"),
                        prop("episode_id", "string", "分集 ID（必填）"),
                        prop("page_num", "integer", "页码，从 1 开始，默认 1"),
                        prop("page_size", "integer", "每页条数，默认 50，最大 100"),
                        prop("shot_status", "string", "可选分镜状态过滤（PENDING/IMAGE_READY/VIDEO_READY/LOCKED）"))));
        tools.add(tool("submit_render_task", "通过正式制作服务提交 IMAGE/VIDEO/VOICE/MUSIC/COMPOSE 任务，保留权限、素材、余额、队列与预检。需 confirm_cost=true；返回真实 task_id 或 task_id_list。",
                McpProductionSchemas.renderSchema()));
        tools.add(tool("publish_episode_to_media", "将当前用户已合成且审核通过的分集成片发布到 bgssai-tube。必须显式传入 confirm_publish=true。",
                schema(List.of("episode_id", "confirm_publish"),
                        prop("episode_id", "string", "分集 ID（必填）"),
                        prop("film_id", "string", "可选成片 ID"),
                        prop("force", "boolean", "可选是否强制重推，默认 false"),
                        prop("confirm_publish", "boolean", "对外发布确认开关，必须显式传 true 才允许发布"))));
        tools.addAll(McpProductionSchemas.definitions());
        return tools;
    }

    public ToolResult callTool(String name, JsonNode args) {
        try {
            return switch (name) {
                case "list_login_logs", "list_my_login_logs" -> ToolResult.ok(loginLogsJson(
                        listLoginLogsService.list(intArg(args, "page_num", 1), intArg(args, "page_size", 20))));
                case "get_liveness", "health_live" -> ToolResult.ok(livenessJson(queryLivenessService.liveness()));
                case "get_readiness", "health_ready" -> ToolResult.ok(readinessJson(queryReadinessService.readiness()));
                case "list_works" -> ToolResult.ok(listWorksJson(args));
                case "get_work" -> ToolResult.ok(getWorkJson(args));
                case "create_work" -> ToolResult.ok(createWorkJson(args));
                case "list_episodes" -> ToolResult.ok(listEpisodesJson(args));
                case "save_episode_script" -> ToolResult.ok(saveEpisodeScriptJson(args));
                case "list_shots" -> ToolResult.ok(listShotsJson(args));
                case "submit_render_task" -> ToolResult.ok(submitRenderTaskJson(args));
                case "publish_episode_to_media" -> ToolResult.ok(publishEpisodeToMediaJson(args));
                default -> McpProductionService.hasTool(name)
                        ? ToolResult.ok(toJson(production.execute(name, args)))
                        : ToolResult.error("未知工具：" + name);
            };
        } catch (IllegalArgumentException | BizException e) {
            log.info("mcp.tool business fail name={} msg={}", name, e.getMessage());
            return ToolResult.error(e.getMessage());
        } catch (Exception e) {
            log.warn("mcp.tool error name={} err={}", name, e.toString());
            return ToolResult.error("工具执行失败");
        }
    }

    private String listWorksJson(JsonNode args) {
        String userId = requireCallerUserId();
        int pageNum = clampPageNum(intArg(args, "page_num", 1));
        int pageSize = clampPageSize(intArg(args, "page_size", 20), 20, 50);
        String workStatus = textArg(args, "work_status");
        String genre = textArg(args, "genre");
        String keyword = textArg(args, "keyword");

        ShortWorkExample ex = new ShortWorkExample();
        ShortWorkExample.Criteria c = ex.createCriteria().andUserIdEqualTo(userId).andDeletedFlagEqualTo(0);
        if (workStatus != null) {
            c.andWorkStatusEqualTo(workStatus);
        }
        if (genre != null) {
            c.andGenreEqualTo(genre);
        }
        if (keyword != null) {
            c.andWorkTitleLike("%" + keyword + "%");
        }
        ex.setOrderByClause("updated_time desc, id desc");
        List<ShortWork> allRows = shortWorkMapper.selectByExample(ex);
        List<ShortWork> pagedRows = slicePage(allRows, pageNum, pageSize);

        List<Map<String, Object>> works = new ArrayList<>();
        for (ShortWork row : pagedRows) {
            works.add(workSummaryMap(row));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("page_num", pageNum);
        out.put("page_size", pageSize);
        out.put("total_size", allRows == null ? 0L : (long) allRows.size());
        out.put("works", works);
        return toJson(out);
    }

    private String getWorkJson(JsonNode args) {
        String userId = requireCallerUserId();
        String workId = requireTextArg(args, "work_id");
        ShortWork work = requireOwnedWork(userId, workId);

        ShortEpisodeExample epEx = new ShortEpisodeExample();
        epEx.createCriteria().andWorkIdEqualTo(work.getWorkId()).andUserIdEqualTo(userId).andDeletedFlagEqualTo(0);
        epEx.setOrderByClause("episode_no asc, id asc");
        List<ShortEpisode> episodeRows = shortEpisodeMapper.selectByExample(epEx);
        List<Map<String, Object>> episodes = new ArrayList<>();
        if (episodeRows != null) {
            for (ShortEpisode ep : episodeRows) {
                episodes.add(episodeSummaryMap(ep));
            }
        }

        Map<String, Object> out = workSummaryMap(work);
        out.put("episodes", episodes);
        return toJson(out);
    }

    private String createWorkJson(JsonNode args) {
        String userId = requireCallerUserId();
        String workTitle = requireTextArg(args, "work_title");
        String oneLinePitch = requireTextArg(args, "one_line_pitch");
        String genre = textArgOrDefault(args, "genre", "REVENGE");
        String visualStyle = textArgOrDefault(args, "visual_style", "MODERN_URBAN");
        int episodeCount = intArg(args, "episode_count", 24);
        if (episodeCount < 1 || episodeCount > 80) {
            throw new IllegalArgumentException("episode_count 必须在 1 到 80 之间");
        }
        int durationSec = intArg(args, "episode_duration_sec", 90);
        if (durationSec != 60 && durationSec != 90 && durationSec != 120) {
            throw new IllegalArgumentException("episode_duration_sec 仅支持 60、90 或 120");
        }

        Date now = new Date();
        ShortWork work = new ShortWork();
        work.setWorkId(ShortIds.id("w-"));
        work.setUserId(userId);
        work.setWorkTitle(workTitle);
        work.setOneLinePitch(oneLinePitch);
        work.setGenre(genre);
        work.setWorkStatus("DRAFT");
        work.setEpisodeCount(episodeCount);
        work.setEpisodeDurationSec(durationSec);
        work.setVisualStyle(visualStyle);
        work.setAspectRatio("VERTICAL_9_16");
        work.setCostSpentCent(0);
        work.setSkipLevelCheckFlag(0);
        work.setArchivedFlag(0);
        work.setDeletedFlag(0);
        work.setCreatedTime(now);
        work.setUpdatedTime(now);
        shortWorkMapper.insertSelective(work);

        if (shortEpisodeMapper != null) {
            for (int i = 1; i <= episodeCount; i++) {
                ShortEpisode ep = new ShortEpisode();
                ep.setEpisodeId(ShortIds.id("e-"));
                ep.setWorkId(work.getWorkId());
                ep.setUserId(userId);
                ep.setEpisodeNo(i);
                ep.setEpisodeTitle("第" + i + "集");
                ep.setScriptStatus("NONE");
                ep.setStoryboardStatus("NONE");
                ep.setDubbingStatus("NONE");
                ep.setComposeStatus("NONE");
                ep.setReviewStatus("NOT_SUBMITTED");
                ep.setDeletedFlag(0);
                ep.setCreatedTime(now);
                ep.setUpdatedTime(now);
                shortEpisodeMapper.insertSelective(ep);
            }
        }

        return toJson(workSummaryMap(work));
    }

    private String listEpisodesJson(JsonNode args) {
        String userId = requireCallerUserId();
        String workId = requireTextArg(args, "work_id");
        ShortWork work = requireOwnedWork(userId, workId);
        int pageNum = clampPageNum(intArg(args, "page_num", 1));
        int pageSize = clampPageSize(intArg(args, "page_size", 20), 20, 50);

        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andWorkIdEqualTo(work.getWorkId()).andUserIdEqualTo(userId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("episode_no asc, id asc");
        List<ShortEpisode> allRows = shortEpisodeMapper.selectByExample(ex);
        List<ShortEpisode> pagedRows = slicePage(allRows, pageNum, pageSize);

        List<Map<String, Object>> episodes = new ArrayList<>();
        for (ShortEpisode ep : pagedRows) {
            episodes.add(episodeSummaryMap(ep));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("work_id", work.getWorkId());
        out.put("page_num", pageNum);
        out.put("page_size", pageSize);
        out.put("total_size", allRows == null ? 0L : (long) allRows.size());
        out.put("episodes", episodes);
        return toJson(out);
    }

    private String saveEpisodeScriptJson(JsonNode args) {
        String userId = requireCallerUserId();
        String episodeId = requireTextArg(args, "episode_id");
        ShortEpisode episode = requireOwnedEpisode(userId, episodeId);
        String workIdArg = textArg(args, "work_id");
        if (workIdArg != null && !workIdArg.equals(episode.getWorkId())) {
            throw new IllegalArgumentException("work_id 与分集所属作品不一致");
        }

        List<Map<String, Object>> scenes = resolveScenes(args, episode);
        if (scenes.isEmpty()) {
            throw new IllegalArgumentException("save_episode_script 需提供非空的 scene_list 或 script_content");
        }

        ShortWork work = requireOwnedWork(userId, episode.getWorkId());
        int targetDuration = work.getEpisodeDurationSec() == null ? 90 : work.getEpisodeDurationSec();
        Map<String, Object> rhythm = RhythmChecker.check(scenes, targetDuration);
        int totalScore = ShortJson.integer(rhythm.get("total_score"), 0);
        int estimatedSec = ShortJson.integer(rhythm.get("estimated_total_sec"), 0);
        String sceneJson = ShortJson.toJson(scenes);
        String rhythmJson = ShortJson.toJson(rhythm);

        Date now = new Date();
        ShortScriptExample sx = new ShortScriptExample();
        sx.createCriteria().andEpisodeIdEqualTo(episode.getEpisodeId());
        ShortScript existing = shortScriptMapper.selectOneByExample(sx);
        String scriptId;
        int nextVersion;
        if (existing == null) {
            scriptId = ShortIds.id("s-");
            nextVersion = 1;
            ShortScript created = new ShortScript();
            created.setScriptId(scriptId);
            created.setEpisodeId(episode.getEpisodeId());
            created.setWorkId(episode.getWorkId());
            created.setUserId(userId);
            created.setScriptVersion(nextVersion);
            created.setScriptStatus("DRAFT");
            created.setSceneJson(sceneJson);
            created.setRhythmJson(rhythmJson);
            created.setTotalScore(totalScore);
            created.setEstimatedTotalSec(estimatedSec);
            created.setForceReadyFlag(0);
            created.setCreatedTime(now);
            created.setUpdatedTime(now);
            shortScriptMapper.insertSelective(created);
        } else {
            scriptId = existing.getScriptId();
            nextVersion = (existing.getScriptVersion() == null ? 0 : existing.getScriptVersion()) + 1;
            ShortScript patch = new ShortScript();
            patch.setScriptVersion(nextVersion);
            patch.setScriptStatus("DRAFT");
            patch.setSceneJson(sceneJson);
            patch.setRhythmJson(rhythmJson);
            patch.setTotalScore(totalScore);
            patch.setEstimatedTotalSec(estimatedSec);
            patch.setUpdatedTime(now);
            ShortScriptExample up = new ShortScriptExample();
            up.createCriteria().andIdEqualTo(existing.getId());
            shortScriptMapper.updateByExampleSelective(patch, up);
        }

        if (shortScriptVersionMapper != null) {
            ShortScriptVersion ver = new ShortScriptVersion();
            ver.setVersionId(ShortIds.id("sv-"));
            ver.setScriptId(scriptId);
            ver.setScriptVersion(nextVersion);
            ver.setSceneJson(sceneJson);
            ver.setTotalScore(totalScore);
            ver.setChangeSummary("mcp_save");
            ver.setCreatedTime(now);
            shortScriptVersionMapper.insertSelective(ver);
        }

        ShortEpisode epPatch = new ShortEpisode();
        String epTitle = textArg(args, "episode_title");
        String epSynopsis = textArg(args, "episode_synopsis");
        String endingHook = textArg(args, "ending_hook");
        if (epTitle != null) {
            epPatch.setEpisodeTitle(epTitle);
        }
        if (epSynopsis != null) {
            epPatch.setEpisodeSynopsis(epSynopsis);
        }
        if (endingHook != null) {
            epPatch.setEndingHook(endingHook);
        }
        epPatch.setScriptStatus("DRAFT");
        epPatch.setUpdatedTime(now);
        ShortEpisodeExample epUp = new ShortEpisodeExample();
        epUp.createCriteria().andIdEqualTo(episode.getId());
        shortEpisodeMapper.updateByExampleSelective(epPatch, epUp);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("episode_id", episode.getEpisodeId());
        out.put("work_id", episode.getWorkId());
        out.put("script_id", scriptId);
        out.put("script_version", nextVersion);
        out.put("script_status", "DRAFT");
        out.put("scene_count", scenes.size());
        out.put("estimated_total_sec", estimatedSec);
        out.put("total_score", totalScore);
        return toJson(out);
    }

    private String listShotsJson(JsonNode args) {
        String userId = requireCallerUserId();
        String episodeId = requireTextArg(args, "episode_id");
        ShortEpisode episode = requireOwnedEpisode(userId, episodeId);
        int pageNum = clampPageNum(intArg(args, "page_num", 1));
        int pageSize = clampPageSize(intArg(args, "page_size", 50), 50, 100);
        String shotStatus = textArg(args, "shot_status");

        ShortShotExample ex = new ShortShotExample();
        ShortShotExample.Criteria c = ex.createCriteria()
                .andEpisodeIdEqualTo(episode.getEpisodeId())
                .andUserIdEqualTo(userId)
                .andDeletedFlagEqualTo(0);
        if (shotStatus != null) {
            c.andShotStatusEqualTo(shotStatus);
        }
        ex.setOrderByClause("shot_no asc, id asc");
        List<ShortShot> allRows = shortShotMapper.selectByExample(ex);
        List<ShortShot> pagedRows = slicePage(allRows, pageNum, pageSize);

        List<Map<String, Object>> shots = new ArrayList<>();
        for (ShortShot shot : pagedRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("shot_id", shot.getShotId());
            item.put("episode_id", shot.getEpisodeId());
            item.put("work_id", shot.getWorkId());
            item.put("shot_no", shot.getShotNo());
            item.put("scene_no", shot.getSceneNo());
            item.put("shot_status", shot.getShotStatus());
            item.put("shot_desc", shot.getShotDesc());
            item.put("dialogue_text", shot.getDialogueText());
            item.put("shot_size", shot.getShotSize());
            item.put("camera_move", shot.getCameraMove());
            item.put("shot_duration_sec", shot.getShotDurationSec());
            item.put("image_prompt", shot.getImagePrompt());
            item.put("first_frame_asset_id", shot.getFirstFrameAssetId());
            item.put("video_asset_id", shot.getVideoAssetId());
            shots.add(item);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("episode_id", episode.getEpisodeId());
        out.put("work_id", episode.getWorkId());
        out.put("page_num", pageNum);
        out.put("page_size", pageSize);
        out.put("total_size", allRows == null ? 0L : (long) allRows.size());
        out.put("shots", shots);
        return toJson(out);
    }

    private String submitRenderTaskJson(JsonNode args) {
        if (!boolArg(args, "confirm_cost")) {
            throw new IllegalArgumentException("提交渲染/合成任务会消耗账户额度，必须显式传入 confirm_cost=true 确认计费");
        }
        return toJson(production.execute("submit_render_task", args));
    }

    private String publishEpisodeToMediaJson(JsonNode args) {
        if (!boolArg(args, "confirm_publish")) {
            throw new IllegalArgumentException("发布成片到 Tube 属于对外公开操作，必须显式传入 confirm_publish=true 确认发布");
        }
        String userId = requireCallerUserId();
        String episodeId = requireTextArg(args, "episode_id");
        ShortEpisode episode = requireOwnedEpisode(userId, episodeId);
        if (!"APPROVED".equals(episode.getReviewStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "剧集审核未通过（review_status=" + episode.getReviewStatus() + "，需 APPROVED）后才能发布到 Tube");
        }

        FilmPublishToMediaReq req = new FilmPublishToMediaReq();
        req.setEpisodeId(episode.getEpisodeId());
        String filmId = textArg(args, "film_id");
        if (filmId != null) {
            req.setFilmId(filmId);
        }
        req.setForce(boolArg(args, "force"));

        ApiResponse resp = filmPublishToMediaService.publish(req);
        Object result = resp == null ? Map.of("episode_id", episode.getEpisodeId(), "status", "PUBLISHED") : resp.getResult();
        return toJson(result);
    }

    private String requireCallerUserId() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("未获取到当前令牌关联的用户身份");
        }
        return userId.trim();
    }

    private ShortWork requireOwnedWork(String userId, String workId) {
        ShortWorkExample ex = new ShortWorkExample();
        ex.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(ex);
        if (work == null || !userId.equals(work.getUserId())) {
            throw new ForbiddenException("作品不存在或无权访问");
        }
        return work;
    }

    private ShortEpisode requireOwnedEpisode(String userId, String episodeId) {
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        ShortEpisode episode = shortEpisodeMapper.selectOneByExample(ex);
        if (episode == null || !userId.equals(episode.getUserId())) {
            throw new ForbiddenException("分集不存在或无权访问");
        }
        return episode;
    }

    private List<Map<String, Object>> resolveScenes(JsonNode args, ShortEpisode episode) {
        if (args != null && args.hasNonNull("scene_list") && args.get("scene_list").isArray()) {
            List<Map<String, Object>> scenes = ShortJson.list(args.get("scene_list").toString());
            if (!scenes.isEmpty()) {
                return scenes;
            }
        }
        String scriptContent = textArg(args, "script_content");
        if (scriptContent == null || scriptContent.isBlank()) {
            return List.of();
        }
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("line_no", 1);
        line.put("character_name", "旁白");
        line.put("line_text", scriptContent);
        line.put("emotion", "NEUTRAL");
        line.put("estimated_sec", Math.max(3, Math.min(60, scriptContent.length() / 4)));

        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("scene_no", 1);
        scene.put("scene_location", "默认场景");
        scene.put("scene_time", "DAY");
        String title = textArg(args, "episode_title");
        scene.put("scene_summary", title != null ? title : (episode.getEpisodeTitle() == null ? "第一场" : episode.getEpisodeTitle()));
        scene.put("line_list", List.of(line));
        return List.of(scene);
    }

    private static Map<String, Object> workSummaryMap(ShortWork work) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("work_id", work.getWorkId());
        m.put("user_id", work.getUserId());
        m.put("work_title", work.getWorkTitle());
        m.put("one_line_pitch", work.getOneLinePitch());
        m.put("genre", work.getGenre());
        m.put("work_status", work.getWorkStatus());
        m.put("episode_count", work.getEpisodeCount());
        m.put("episode_duration_sec", work.getEpisodeDurationSec());
        m.put("visual_style", work.getVisualStyle());
        m.put("aspect_ratio", work.getAspectRatio());
        m.put("cost_spent_cent", work.getCostSpentCent());
        m.put("updated_time", work.getUpdatedTime());
        return m;
    }

    private static Map<String, Object> episodeSummaryMap(ShortEpisode ep) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("episode_id", ep.getEpisodeId());
        m.put("work_id", ep.getWorkId());
        m.put("episode_no", ep.getEpisodeNo());
        m.put("episode_title", ep.getEpisodeTitle());
        m.put("episode_synopsis", ep.getEpisodeSynopsis());
        m.put("ending_hook", ep.getEndingHook());
        m.put("script_status", ep.getScriptStatus());
        m.put("storyboard_status", ep.getStoryboardStatus());
        m.put("dubbing_status", ep.getDubbingStatus());
        m.put("compose_status", ep.getComposeStatus());
        m.put("review_status", ep.getReviewStatus());
        m.put("film_duration_sec", ep.getFilmDurationSec());
        return m;
    }

    private static <T> List<T> slicePage(List<T> rows, int pageNum, int pageSize) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        int fromIndex = (pageNum - 1) * pageSize;
        if (fromIndex >= rows.size()) {
            return List.of();
        }
        int toIndex = Math.min(rows.size(), fromIndex + pageSize);
        return rows.subList(fromIndex, toIndex);
    }

    private static int clampPageNum(int pageNum) {
        return Math.max(1, pageNum);
    }

    private static int clampPageSize(int pageSize, int defaultSize, int maxSize) {
        if (pageSize < 1) {
            return defaultSize;
        }
        return Math.min(pageSize, maxSize);
    }

    private String loginLogsJson(PageResponse page) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("page_num", page.getPageNum());
        out.put("page_size", page.getPageSize());
        out.put("total_size", page.getTotalSize());
        List<Map<String, Object>> logs = new ArrayList<>();
        if (page.getResult() instanceof List<?> rows) {
            for (Object row : rows) {
                if (row instanceof LoginLogItem item) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", item.getId());
                    m.put("user_id", item.getUserId());
                    m.put("action", item.getAction());
                    m.put("ip_address", item.getIpAddress());
                    m.put("ip_location", item.getIpLocation());
                    m.put("user_agent", item.getUserAgent());
                    m.put("create_time", item.getCreateTime());
                    logs.add(m);
                }
            }
        }
        out.put("logs", logs);
        return toJson(out);
    }

    private String livenessJson(LivenessReport report) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", report.getStatus() == null ? null : report.getStatus().name());
        out.put("app", report.getApp());
        out.put("profile", report.getProfile());
        out.put("uptime_seconds", report.getUptimeSeconds());
        out.put("checked_at", report.getCheckedAt());
        return toJson(out);
    }

    private String readinessJson(ReadinessReport report) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", report.getStatus() == null ? null : report.getStatus().name());
        out.put("app", report.getApp());
        out.put("checked_at", report.getCheckedAt());
        List<Map<String, Object>> components = new ArrayList<>();
        if (report.getComponents() != null) {
            for (ReadinessComponent c : report.getComponents()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", c.getName());
                m.put("status", c.getStatus() == null ? null : c.getStatus().name());
                components.add(m);
            }
        }
        out.put("components", components);
        return toJson(out);
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    private ObjectNode tool(String name, String description, ObjectNode inputSchema) {
        ObjectNode t = mapper.createObjectNode();
        t.put("name", name);
        t.put("description", description);
        t.set("inputSchema", inputSchema);
        return t;
    }

    private ObjectNode schema(List<String> required, ObjectNode... props) {
        ObjectNode s = mapper.createObjectNode();
        s.put("type", "object");
        ObjectNode properties = s.putObject("properties");
        for (ObjectNode p : props) {
            String n = p.remove("__name").asText();
            properties.set(n, p);
        }
        if (!required.isEmpty()) {
            ArrayNode req = s.putArray("required");
            required.forEach(req::add);
        }
        return s;
    }

    private ObjectNode prop(String name, String type, String description) {
        ObjectNode p = mapper.createObjectNode();
        p.put("__name", name);
        p.put("type", type);
        p.put("description", description);
        return p;
    }

    private static int intArg(JsonNode args, String name, int def) {
        if (args == null || !args.hasNonNull(name)) {
            return def;
        }
        return args.get(name).asInt(def);
    }

    private static boolean boolArg(JsonNode args, String name) {
        return args != null && args.hasNonNull(name) && args.get(name).isBoolean() && args.get(name).asBoolean();
    }

    private static String textArg(JsonNode args, String name) {
        if (args == null || !args.hasNonNull(name)) {
            return null;
        }
        String val = args.get(name).asText(null);
        return val == null || val.isBlank() ? null : val.trim();
    }

    private static String textArgOrDefault(JsonNode args, String name, String def) {
        String val = textArg(args, name);
        return val == null ? def : val;
    }

    private static String requireTextArg(JsonNode args, String name) {
        String val = textArg(args, name);
        if (val == null) {
            throw new IllegalArgumentException("缺少必填参数: " + name);
        }
        return val;
    }
}
