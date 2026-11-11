package com.bgssai.shortai.mcp.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.production.dto.ProductionDtos.AiTaskReq;
import com.bgssai.shortai.production.dto.ProductionDtos.CharacterReq;
import com.bgssai.shortai.production.dto.ProductionDtos.FilmReq;
import com.bgssai.shortai.production.dto.ProductionDtos.ScriptGenerateReq;
import com.bgssai.shortai.production.dto.ProductionDtos.ShotReq;
import com.bgssai.shortai.production.dto.FeatureDtos.ApplyVoiceReq;
import com.bgssai.shortai.production.dto.FeatureDtos.BgmGenerateReq;
import com.bgssai.shortai.production.dto.FeatureDtos.DubbingAlignReq;
import com.bgssai.shortai.production.dto.FeatureDtos.DubbingSubmitReq;
import com.bgssai.shortai.production.dto.FeatureDtos.FilmSettingsSaveReq;
import com.bgssai.shortai.production.service.AiTaskService;
import com.bgssai.shortai.production.service.AssetService;
import com.bgssai.shortai.production.service.CharacterService;
import com.bgssai.shortai.production.service.DubbingService;
import com.bgssai.shortai.production.service.FilmService;
import com.bgssai.shortai.production.service.ScriptService;
import com.bgssai.shortai.production.service.ShotService;
import com.bgssai.shortai.production.service.WorkAccessService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** MCP adapts the same production services used by the web application. */
@Service
public class McpProductionService {
    private final WorkAccessService access;
    private final ScriptService scripts;
    private final ShotService shots;
    private final AiTaskService tasks;
    private final DubbingService dubbing;
    private final FilmService films;
    private final CharacterService characters;
    private final AiModelResolveService models;
    private final AssetService assets;
    private final ObjectMapper json = new ObjectMapper();

    public McpProductionService(WorkAccessService access, ScriptService scripts, ShotService shots,
            AiTaskService tasks, DubbingService dubbing, FilmService films,
            CharacterService characters, AiModelResolveService models, AssetService assets) {
        this.access = access;
        this.scripts = scripts;
        this.shots = shots;
        this.tasks = tasks;
        this.dubbing = dubbing;
        this.films = films;
        this.characters = characters;
        this.models = models;
        this.assets = assets;
    }

    public static boolean hasTool(String name) {
        return name != null && Set.of("list_models", "ready_episode_script", "split_storyboard",
                "save_storyboard", "get_render_task", "get_episode_film", "save_film_settings",
                "list_reference_cards", "generate_reference_image", "lock_reference_card", "align_dubbing",
                "list_voices", "set_character_voice", "get_asset_download", "lock_shot")
                .contains(name);
    }

    @Transactional
    public Object execute(String name, JsonNode args) {
        access.requireUser();
        return switch (name) {
            case "list_models" -> result(models.listEnabled(optionalText(args, "ability_type")));
            case "list_voices" -> result(dubbing.voices());
            case "set_character_voice" -> setCharacterVoice(args);
            case "ready_episode_script" -> ready(args);
            case "split_storyboard" -> split(args);
            case "save_storyboard" -> saveStoryboard(args);
            case "get_render_task" -> result(tasks.detail(text(args, "task_id")));
            case "get_asset_download" -> assetDownload(args);
            case "lock_shot" -> lockShot(args);
            case "get_episode_film" -> film(args);
            case "save_film_settings" -> settings(args);
            case "list_reference_cards" -> result(characters.consistencyPanel(text(args, "work_id")));
            case "generate_reference_image" -> reference(args, true);
            case "lock_reference_card" -> reference(args, false);
            case "align_dubbing" -> align(args);
            case "submit_render_task" -> submit(args);
            default -> throw new IllegalArgumentException("未知制作工具: " + name);
        };
    }

    private Object ready(JsonNode args) {
        ScriptGenerateReq req = new ScriptGenerateReq();
        req.setEpisodeId(text(args, "episode_id"));
        req.setForceReadyFlag(flag(args, "force_ready", false) ? 1 : 0);
        return result(scripts.ready(req));
    }

    private Object assetDownload(JsonNode args) {
        onlyArguments(args, Set.of("asset_id"));
        return result(assets.getDownload(text(args, "asset_id")));
    }

    private Object lockShot(JsonNode args) {
        onlyArguments(args, Set.of("episode_id", "shot_id", "confirm_review"));
        if (!flag(args, "confirm_review", false)) {
            throw new IllegalArgumentException("请先检查镜头视频，再显式传入 confirm_review=true");
        }
        ShortEpisode episode = access.requireEpisode(text(args, "episode_id"));
        ShortShot shot = requireEpisodeShot(episode, text(args, "shot_id"));
        if (shot.getVideoAssetId() == null || shot.getVideoAssetId().isBlank()
                || !("VIDEO_READY".equals(shot.getShotStatus()) || "LOCKED".equals(shot.getShotStatus()))) {
            throw new IllegalArgumentException("镜头须已有视频且状态为 VIDEO_READY 或 LOCKED 才能确认锁定");
        }
        ShotReq req = new ShotReq();
        req.setEpisodeId(episode.getEpisodeId());
        req.setShotId(shot.getShotId());
        return result(shots.lock(req, true));
    }

    private static void onlyArguments(JsonNode args, Set<String> allowed) {
        if (args == null || !args.isObject()) throw new IllegalArgumentException("工具参数必须为 object");
        args.fieldNames().forEachRemaining(key -> {
            if (!allowed.contains(key)) throw new IllegalArgumentException("不支持的参数: " + key);
        });
    }

    private Object split(JsonNode args) {
        // Splitting replaces unlocked shots. Require an explicit destructive-action acknowledgement.
        if (!flag(args, "confirm_replace", false)) {
            throw new IllegalArgumentException("split_storyboard 会替换未锁定分镜，需 confirm_replace=true");
        }
        ShotReq req = new ShotReq();
        req.setEpisodeId(text(args, "episode_id"));
        return result(shots.split(req));
    }

    private Object saveStoryboard(JsonNode args) {
        ShortEpisode ep = access.requireEpisode(text(args, "episode_id"));
        JsonNode list = args.path("shots");
        if (!list.isArray() || list.isEmpty() || list.size() > 100) {
            throw new IllegalArgumentException("shots 必须为 1-100 个分镜的数组");
        }
        List<ShotReq> requests = new ArrayList<>();
        Set<String> seen = new java.util.LinkedHashSet<>();
        for (JsonNode item : list) {
            String id = text(item, "shot_id");
            requireEpisodeShot(ep, id);
            if (!seen.add(id)) throw new IllegalArgumentException("shots 不可重复 shot_id");
            ShotReq req = new ShotReq();
            req.setEpisodeId(ep.getEpisodeId());
            req.setShotId(id);
            req.setShotDesc(optionalText(item, "shot_desc"));
            req.setDialogueText(optionalText(item, "dialogue_text"));
            req.setShotSize(optionalText(item, "shot_size"));
            req.setCameraMove(optionalText(item, "camera_move"));
            req.setExtraPrompt(optionalText(item, "extra_prompt"));
            if (item.hasNonNull("shot_duration_sec")) {
                req.setShotDurationSec(integer(item, "shot_duration_sec", 1, 120));
            }
            requests.add(req);
        }
        for (ShotReq req : requests) shots.update(req);
        return Map.of("episode_id", ep.getEpisodeId(), "saved_shot_ids", List.copyOf(seen),
                "saved_count", requests.size());
    }

    private Object submit(JsonNode args) {
        confirmCost(args);
        ShortEpisode ep = access.requireEpisode(text(args, "episode_id"));
        String type = optionalText(args, "task_type");
        type = type == null ? "COMPOSE" : type.toUpperCase(Locale.ROOT);
        if (!"MUSIC".equals(type) && (args.hasNonNull("prompt") || args.hasNonNull("lyrics") || args.has("is_instrumental"))) {
            throw new IllegalArgumentException("prompt/lyrics/is_instrumental 仅用于 MUSIC；画面和配音请先 save_storyboard");
        }
        String shotId = optionalText(args, "shot_id");
        if (shotId != null) requireEpisodeShot(ep, shotId);
        String model = optionalText(args, "model_code");
        ApiResponse response;
        switch (type) {
            case "IMAGE", "VIDEO" -> {
                AiTaskReq req = new AiTaskReq();
                req.setEpisodeId(ep.getEpisodeId());
                req.setTaskType(type);
                req.setShotIds(shotId == null ? null : List.of(shotId));
                req.setModelCode(model);
                req.setAutoNextFlag(0);
                response = tasks.submit(req);
            }
            case "VOICE" -> {
                DubbingSubmitReq req = new DubbingSubmitReq();
                req.setEpisodeId(ep.getEpisodeId());
                req.setShotIdList(shotId == null ? null : List.of(shotId));
                req.setModelCode(model);
                response = dubbing.submit(req);
            }
            case "MUSIC" -> {
                if (shotId != null) throw new IllegalArgumentException("MUSIC 是分集背景音乐，不接受 shot_id");
                BgmGenerateReq req = new BgmGenerateReq();
                req.setWorkId(ep.getWorkId());
                req.setEpisodeId(ep.getEpisodeId());
                req.setModelCode(model);
                req.setPrompt(text(args, "prompt"));
                req.setLyrics(optionalText(args, "lyrics"));
                req.setIsInstrumental(flag(args, "is_instrumental", true) ? 1 : 0);
                response = films.generateBgm(req);
            }
            case "COMPOSE" -> {
                if (shotId != null || model != null) {
                    throw new IllegalArgumentException("COMPOSE 使用正式合成器，不接受 shot_id 或 model_code");
                }
                FilmReq req = new FilmReq();
                req.setEpisodeId(ep.getEpisodeId());
                response = films.compose(req);
            }
            default -> throw new IllegalArgumentException("task_type 仅支持 IMAGE、VIDEO、VOICE、MUSIC、COMPOSE");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        Object value = result(response);
        if (value instanceof Map<?, ?> values) values.forEach((k, v) -> out.put(String.valueOf(k), v));
        else throw new IllegalStateException("production task response missing");
        out.put("episode_id", ep.getEpisodeId());
        out.put("task_type", type);
        out.put("confirm_cost", true);
        return out;
    }

    private Object film(JsonNode args) {
        String episode = text(args, "episode_id");
        access.requireEpisode(episode);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("film", result(films.detail(episode)));
        out.put("precheck", result(films.precheck(episode)));
        out.put("dubbing", result(dubbing.list(episode)));
        out.put("subtitles", result(dubbing.subtitleDetail(episode)));
        return out;
    }

    private Object settings(JsonNode args) {
        ShortEpisode ep = access.requireEpisode(text(args, "episode_id"));
        FilmSettingsSaveReq req = new FilmSettingsSaveReq();
        req.setWorkId(ep.getWorkId());
        req.setEpisodeId(ep.getEpisodeId());
        if (args.has("burn_subtitle")) req.setBurnSubtitleFlag(flag(args, "burn_subtitle", true) ? 1 : 0);
        String ratio = optionalText(args, "aspect_ratio");
        if (ratio != null && !Set.of("9:16", "16:9", "1:1").contains(ratio)) {
            throw new IllegalArgumentException("aspect_ratio 仅支持 9:16、16:9、1:1");
        }
        req.setAspectRatio(ratio);
        if (args.hasNonNull("bgm_asset_id")) {
            req.setComposeSetting(Map.of("bgm_asset_id", optionalText(args, "bgm_asset_id")));
        }
        return result(films.settingsSave(req));
    }

    private Object reference(JsonNode args, boolean generate) {
        if (generate) confirmCost(args);
        String workId = text(args, "work_id");
        String type = text(args, "card_type").toUpperCase(Locale.ROOT);
        String id = text(args, "card_id");
        String collection = switch (type) {
            case "CHARACTER" -> "character_list";
            case "SCENE" -> "scene_list";
            case "PROP" -> "prop_list";
            default -> throw new IllegalArgumentException("card_type 仅支持 CHARACTER、SCENE、PROP");
        };
        JsonNode panel = json.valueToTree(result(characters.consistencyPanel(workId)));
        JsonNode card = null;
        String idKey = "CHARACTER".equals(type) ? "character_id" : "scene_card_id";
        for (JsonNode row : panel.path(collection)) if (id.equals(row.path(idKey).asText())) card = row;
        if (card == null) throw new ForbiddenException("card not found in work");
        CharacterReq req = new CharacterReq();
        req.setWorkId(workId);
        if ("CHARACTER".equals(type)) req.setCharacterId(id); else req.setSceneCardId(id);
        if (generate) {
            int count = args.hasNonNull("candidate_count") ? integer(args, "candidate_count", 2, 8) : 2;
            if (count != 2 && count != 4 && count != 8) throw new IllegalArgumentException("candidate_count 必须为 2/4/8");
            String appearance = optionalText(args, "appearance_prompt");
            if (appearance != null) {
                if (appearance.isBlank()) {
                    throw new IllegalArgumentException("appearance_prompt 不能为空");
                }
                req.setAppearancePrompt(appearance);
                result("CHARACTER".equals(type) ? characters.updateCharacter(req) : characters.updateScene(req));
            }
            req.setCandidateCount(count);
            req.setModelCode(optionalText(args, "model_code"));
            return result("CHARACTER".equals(type) ? characters.portrait(req) : characters.portraitScene(req));
        }
        String asset = text(args, "asset_id");
        boolean candidate = false;
        for (JsonNode row : card.path("candidate_list")) {
            if (asset.equals(row.path("asset_id").asText())) candidate = true;
        }
        if (!candidate) throw new ForbiddenException("asset must be a generated candidate of this card");
        req.setAssetId(asset);
        return result("CHARACTER".equals(type) ? characters.lockCharacter(req) : characters.lockScene(req));
    }

    private Object setCharacterVoice(JsonNode args) {
        String voice = text(args, "voice_id");
        JsonNode catalog = json.valueToTree(result(dubbing.voices()));
        boolean found = false;
        for (JsonNode row : catalog.path("voice_list")) if (voice.equals(row.path("voice_id").asText())) found = true;
        if (!found) throw new IllegalArgumentException("voice_id 必须来自 list_voices 正式目录");
        ApplyVoiceReq req = new ApplyVoiceReq();
        req.setCharacterId(text(args, "character_id"));
        req.setVoiceId(voice);
        result(dubbing.applyVoiceToCharacter(req));
        return Map.of("character_id", req.getCharacterId(), "voice_id", voice);
    }

    private Object align(JsonNode args) {
        ShortEpisode ep = access.requireEpisode(text(args, "episode_id"));
        String shot = text(args, "shot_id");
        requireEpisodeShot(ep, shot);
        String action = text(args, "align_action");
        if (!Set.of("PAD_SILENCE", "ADJUST_SPEED", "EXTEND_SHOT").contains(action)) {
            throw new IllegalArgumentException("invalid align_action");
        }
        DubbingAlignReq req = new DubbingAlignReq();
        req.setShotId(shot);
        req.setAlignAction(action);
        return result(dubbing.align(req));
    }

    private ShortShot requireEpisodeShot(ShortEpisode ep, String id) {
        ShortShot shot = shots.requireShot(id);
        if (!ep.getEpisodeId().equals(shot.getEpisodeId())) throw new ForbiddenException("shot not in episode");
        return shot;
    }

    private static Object result(ApiResponse response) {
        if (response == null || !response.isSuccess()) throw new IllegalStateException("production service failed");
        return response.getResult();
    }

    private static void confirmCost(JsonNode args) {
        if (!flag(args, "confirm_cost", false)) {
            throw new IllegalArgumentException("提交任务会消耗账户额度，必须显式传入 confirm_cost=true");
        }
    }

    private static String text(JsonNode args, String key) {
        String value = optionalText(args, key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("缺少必填参数: " + key);
        return value;
    }

    private static String optionalText(JsonNode args, String key) {
        if (args == null || !args.hasNonNull(key)) return null;
        if (!args.get(key).isTextual()) throw new IllegalArgumentException(key + " 必须为字符串");
        return args.get(key).asText().trim();
    }

    private static boolean flag(JsonNode args, String key, boolean fallback) {
        if (args == null || !args.has(key)) return fallback;
        if (!args.get(key).isBoolean()) throw new IllegalArgumentException(key + " 必须为 boolean");
        return args.get(key).booleanValue();
    }

    private static int integer(JsonNode args, String key, int min, int max) {
        JsonNode value = args.get(key);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()
                || value.intValue() < min || value.intValue() > max) {
            throw new IllegalArgumentException(key + " 必须为 " + min + "-" + max + " 的整数");
        }
        return value.intValue();
    }
}
