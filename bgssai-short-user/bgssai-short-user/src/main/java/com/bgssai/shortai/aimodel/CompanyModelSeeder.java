package com.bgssai.shortai.aimodel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.aiprovider.CompanyCompatGateway;
import com.bgssai.shortai.aiprovider.ImageConditionEndpoint;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProvider;
import com.bgssai.shortai.aiprovider.OpenAiCompatibleAiProvider;
import com.bgssai.shortai.mapper.AiModelConfigMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.AiModelConfigExample;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.secret.SysSecretReader;

/**
 * Idempotently backfills BGSSAI company multimodal catalog rows, secret placeholders,
 * and company defaults on startup without overwriting custom administrator settings
 * or storing real API keys in source control.
 */
@Component
@Order(100)
public class CompanyModelSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CompanyModelSeeder.class);

    private static final Map<String, Set<String>> LEGACY_DEFAULT_CODES = Map.of(
            "TEXT", Set.of("deepseek-chat", "deepseek-v3", "gpt-4o-mini", "qwen-plus"),
            "IMAGE", Set.of("kling-v3", "kling-v2-1", "image-01"),
            "VOICE", Set.of("minimax-speech-2.8-hd", "speech-02-hd", "tts-1"),
            "VIDEO", Set.of("minimax-video-h3", "minimax-hailuo-2.3", "kling-v2-6", "cogvideox"));

    private final AiModelConfigMapper mapper;
    private final DataSource dataSource;

    @Autowired
    public CompanyModelSeeder(AiModelConfigMapper mapper,
            @Autowired(required = false) DataSource dataSource) {
        this.mapper = mapper;
        this.dataSource = dataSource;
    }

    CompanyModelSeeder(AiModelConfigMapper mapper) {
        this(mapper, null);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedSecretPlaceholders();
            seedModels();
            promoteCompanyDefaults();
        } catch (RuntimeException ex) {
            log.warn("company model seeder skipped: {}", ex.getMessage());
        }
    }

    private void seedSecretPlaceholders() {
        if (dataSource == null) {
            return;
        }
        List<SecretPlaceholder> placeholders = List.of(
                new SecretPlaceholder(SysSecretReader.AI_TOKENHUB_CN_API_KEY,
                        "BGSSAI TokenHub-CN API Key（境内模型中枢）"),
                new SecretPlaceholder(SysSecretReader.AI_OPENAI_COMPAT_API_KEY,
                        "公司自建多模态网关 Bearer（https://chat.bgssai.com/v1）。不写真实密钥。"),
                new SecretPlaceholder(SysSecretReader.AI_IMAGE_TO_VIDEO_API_KEY,
                        "图生视频 Bearer。空值时改用公司键 ai.openai_compat.api_key 调用 https://chat.bgssai.com/i2v 。不写真实密钥。"));
        try (Connection conn = dataSource.getConnection()) {
            for (SecretPlaceholder item : placeholders) {
                ensureSecretPlaceholder(conn, item);
            }
        } catch (Exception ex) {
            log.warn("company secret placeholder check skipped: {}", ex.getMessage());
        }
    }

    private static void ensureSecretPlaceholder(Connection conn, SecretPlaceholder item) throws Exception {
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT 1 FROM sys_secret WHERE secret_key = ?")) {
            check.setString(1, item.key());
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO sys_secret (secret_key, secret_value, description, enabled, updated_by) "
                        + "VALUES (?, '', ?, 1, 'system')")) {
            insert.setString(1, item.key());
            insert.setString(2, item.description());
            insert.executeUpdate();
        }
    }

    private void seedModels() {
        for (SeedRow seed : catalogRows()) {
            AiModelConfig existing = findExisting(seed);
            if (existing == null) {
                mapper.insertSelective(toEntity(seed));
                continue;
            }
            if (ImageConditionedVideoProvider.MODEL_ID.equals(seed.modelId())) {
                backfillImageToVideo(existing);
            }
        }
    }

    private AiModelConfig findExisting(SeedRow seed) {
        AiModelConfigExample byId = new AiModelConfigExample();
        byId.createCriteria().andModelIdEqualTo(seed.modelId());
        List<AiModelConfig> idMatches = mapper.selectByExample(byId);
        if (idMatches != null && !idMatches.isEmpty()) {
            return idMatches.get(0);
        }
        AiModelConfigExample byCode = new AiModelConfigExample();
        byCode.createCriteria()
                .andAbilityTypeEqualTo(seed.abilityType())
                .andProviderCodeEqualTo(seed.providerCode())
                .andModelCodeEqualTo(seed.modelCode())
                .andDeletedFlagEqualTo(0);
        List<AiModelConfig> codeMatches = mapper.selectByExample(byCode);
        return codeMatches == null || codeMatches.isEmpty() ? null : codeMatches.get(0);
    }

    private void backfillImageToVideo(AiModelConfig existing) {
        Map<String, Object> cfg = new LinkedHashMap<>(ShortJson.map(existing.getConfigJson()));
        boolean changed = false;
        if (ShortJson.text(cfg.get("base_url")).isBlank()) {
            cfg.put("base_url", ImageConditionEndpoint.COMPANY_IMAGE_TO_VIDEO);
            changed = true;
        }
        if (ShortJson.text(cfg.get("secret_ref")).isBlank()) {
            cfg.put("secret_ref", SysSecretReader.AI_IMAGE_TO_VIDEO_API_KEY);
            changed = true;
        }
        Integer timeout = existing.getTimeoutSec();
        int nextTimeout = timeout == null ? 14400 : Math.max(timeout, 14400);
        if (timeout == null || timeout < 14400) {
            changed = true;
        }
        if (!changed) {
            return;
        }
        AiModelConfig patch = new AiModelConfig();
        patch.setConfigJson(ShortJson.toJson(cfg));
        patch.setTimeoutSec(nextTimeout);
        updateRow(existing, patch);
    }

    private void promoteCompanyDefaults() {
        promoteAbilityDefault("COMPOSE", "FFMPEG", "ffmpeg-concat");
        promoteAbilityDefault("TEXT", "TENCENT_TOKENHUB", "bgssai-chat");
        promoteAbilityDefault("IMAGE", "TENCENT_TOKENHUB", "bgssai-image");
        promoteAbilityDefault("VOICE", "TENCENT_TOKENHUB", "bgssai-tts");
        promoteAbilityDefault("VIDEO", "TENCENT_TOKENHUB", "bgssai-video");
    }

    private void promoteAbilityDefault(String ability, String providerCode, String modelCode) {
        AiModelConfigExample ex = new AiModelConfigExample();
        ex.createCriteria()
                .andAbilityTypeEqualTo(ability)
                .andEnabledFlagEqualTo(1)
                .andDeletedFlagEqualTo(0);
        ex.setOrderByClause("default_flag desc, id asc");
        List<AiModelConfig> rows = mapper.selectByExample(ex);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        AiModelConfig target = null;
        List<AiModelConfig> currentDefaults = new ArrayList<>();
        for (AiModelConfig row : rows) {
            if (row == null) {
                continue;
            }
            if (target == null && providerCode.equalsIgnoreCase(row.getProviderCode())
                    && modelCode.equalsIgnoreCase(row.getModelCode())) {
                target = row;
            }
            if (Integer.valueOf(1).equals(row.getDefaultFlag())) {
                currentDefaults.add(row);
            }
        }
        if (target == null) {
            return;
        }
        if (currentDefaults.size() == 1 && sameRow(currentDefaults.get(0), target)) {
            return;
        }
        for (AiModelConfig current : currentDefaults) {
            if (!sameRow(current, target) && !isLegacyDefault(ability, current.getModelCode())) {
                return;
            }
        }
        if (!Integer.valueOf(1).equals(target.getDefaultFlag())) {
            AiModelConfig promote = new AiModelConfig();
            promote.setDefaultFlag(1);
            updateRow(target, promote);
        }
        for (AiModelConfig current : currentDefaults) {
            if (!sameRow(current, target)) {
                AiModelConfig clear = new AiModelConfig();
                clear.setDefaultFlag(0);
                updateRow(current, clear);
            }
        }
    }

    private void updateRow(AiModelConfig existing, AiModelConfig patch) {
        if (existing.getId() != null) {
            patch.setId(existing.getId());
            mapper.updateByPrimaryKeySelective(patch);
            return;
        }
        if (existing.getModelId() != null && !existing.getModelId().isBlank()) {
            AiModelConfigExample ex = new AiModelConfigExample();
            ex.createCriteria().andModelIdEqualTo(existing.getModelId());
            mapper.updateByExampleSelective(patch, ex);
        }
    }

    private static boolean sameRow(AiModelConfig a, AiModelConfig b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.getId() != null && b.getId() != null) {
            return a.getId().equals(b.getId());
        }
        return a.getModelId() != null && a.getModelId().equals(b.getModelId());
    }

    private static boolean isLegacyDefault(String ability, String modelCode) {
        if (modelCode == null || modelCode.isBlank()) {
            return true;
        }
        Set<String> legacy = LEGACY_DEFAULT_CODES.get(ability);
        return legacy != null && legacy.contains(modelCode.trim().toLowerCase(Locale.ROOT));
    }

    private static AiModelConfig toEntity(SeedRow seed) {
        AiModelConfig row = new AiModelConfig();
        row.setModelId(seed.modelId());
        row.setAbilityType(seed.abilityType());
        row.setProviderCode(seed.providerCode());
        row.setModelCode(seed.modelCode());
        row.setModelName(seed.modelName());
        row.setQualityLevel(seed.qualityLevel());
        row.setPriceUnit(seed.priceUnit());
        row.setUnitPriceCent(seed.unitPriceCent());
        row.setDefaultFlag(0);
        row.setEnabledFlag(1);
        row.setTimeoutSec(seed.timeoutSec());
        row.setMaxRetry(seed.maxRetry());
        row.setConfigJson(seed.configJson());
        row.setDeletedFlag(0);
        return row;
    }

    private static List<SeedRow> catalogRows() {
        return List.of(
                new SeedRow("m-compose-ffmpeg", "COMPOSE", "FFMPEG", "ffmpeg-concat",
                        "成片合成（本机 FFmpeg）", "STANDARD", "PER_CALL", 0, 300, 0,
                        "{\"container\":\"mp4\",\"resolution\":\"1080x1920\",\"fps\":30}"),
                new SeedRow("m-tokenhub-text-bgssai-chat", "TEXT", "TENCENT_TOKENHUB", "bgssai-chat",
                        "BGSSAI Chat", "STANDARD", "PER_KILO_TOKEN", 1, 120, 1,
                        "{\"hub\":\"cn\",\"secret_ref\":\"ai.tokenhub_cn.api_key\",\"chat_path\":\"/v1/chat/completions\",\"public_alias\":\"bgssai-chat\",\"public_name\":\"BGSSAI Chat\"}"),
                new SeedRow("m-tokenhub-image-bgssai", "IMAGE", "TENCENT_TOKENHUB", "bgssai-image",
                        "BGSSAI Image", "STANDARD", "PER_CALL", 40, 180, 1,
                        "{\"hub\":\"cn\",\"secret_ref\":\"ai.tokenhub_cn.api_key\",\"public_alias\":\"bgssai-image\",\"public_name\":\"BGSSAI Image\"}"),
                new SeedRow("m-tokenhub-voice-bgssai-tts", "VOICE", "TENCENT_TOKENHUB", "bgssai-tts",
                        "BGSSAI TTS", "STANDARD", "PER_CALL", 2, 180, 1,
                        "{\"hub\":\"cn\",\"secret_ref\":\"ai.tokenhub_cn.api_key\",\"default_voice_id\":\"alloy\",\"public_alias\":\"bgssai-tts\",\"public_name\":\"BGSSAI TTS\"}"),
                new SeedRow("m-tokenhub-video-bgssai", "VIDEO", "TENCENT_TOKENHUB", "bgssai-video",
                        "BGSSAI Video", "STANDARD", "PER_SECOND", 20, 900, 1,
                        "{\"hub\":\"cn\",\"secret_ref\":\"ai.tokenhub_cn.api_key\",\"poll_interval_ms\":3000,\"public_alias\":\"bgssai-video\",\"public_name\":\"BGSSAI Video\"}"),
                new SeedRow(ImageConditionedVideoProvider.MODEL_ID, "VIDEO", ImageConditionedVideoProvider.CODE,
                        ImageConditionedVideoProvider.MODEL_CODE, "BGSSAI Video", "STANDARD", "PER_SECOND", 20, 14400, 0,
                        "{\"base_url\":\"https://chat.bgssai.com/i2v\",\"secret_ref\":\"ai.image_to_video.api_key\",\"public_alias\":\"bgssai-video\",\"public_name\":\"BGSSAI Video\",\"width\":384,\"height\":256,\"num_frames\":25,\"fps\":8}"),
                new SeedRow("m-compat-text-qwen", "TEXT", OpenAiCompatibleAiProvider.CODE, "qwen",
                        "剧本文本", "STANDARD", "PER_KILO_TOKEN", 1, 180, 1,
                        "{\"base_url\":\"" + CompanyCompatGateway.DEFAULT_BASE + "\",\"secret_ref\":\"ai.openai_compat.api_key\",\"chat_path\":\"/chat/completions\"}"),
                new SeedRow("m-compat-image-sd-turbo", "IMAGE", OpenAiCompatibleAiProvider.CODE, "sd-turbo",
                        "分镜画面", "FAST", "PER_CALL", 20, 180, 1,
                        "{\"base_url\":\"" + CompanyCompatGateway.DEFAULT_BASE + "\",\"secret_ref\":\"ai.openai_compat.api_key\",\"image_path\":\"/images/generations\",\"size\":\"512x768\"}"),
                new SeedRow("m-compat-video-cogvideox", "VIDEO", OpenAiCompatibleAiProvider.CODE, "cogvideox",
                        "镜头视频", "STANDARD", "PER_SECOND", 20, 900, 1,
                        "{\"base_url\":\"" + CompanyCompatGateway.DEFAULT_BASE + "\",\"secret_ref\":\"ai.openai_compat.api_key\",\"video_path\":\"/videos/generations\",\"video_task_path\":\"/videos/generations/{task_id}\",\"poll_interval_ms\":3000}"),
                new SeedRow("m-compat-voice-piper", "VOICE", OpenAiCompatibleAiProvider.CODE, "piper",
                        "台词配音", "STANDARD", "PER_CALL", 2, 180, 1,
                        "{\"base_url\":\"" + CompanyCompatGateway.DEFAULT_BASE + "\",\"secret_ref\":\"ai.openai_compat.api_key\",\"speech_path\":\"/audio/speech\",\"default_voice_id\":\"zh\"}"),
                new SeedRow("m-compat-asr-whisper", "ASR", OpenAiCompatibleAiProvider.CODE, "whisper-1",
                        "台词转写", "STANDARD", "PER_MINUTE", 2, 300, 1,
                        "{\"base_url\":\"" + CompanyCompatGateway.DEFAULT_BASE + "\",\"secret_ref\":\"ai.openai_compat.api_key\",\"transcription_path\":\"/audio/transcriptions\",\"language\":\"zh\",\"public_alias\":\"bgssai-asr\",\"public_name\":\"BGSSAI ASR\"}"));
    }

    private record SecretPlaceholder(String key, String description) {
    }

    private record SeedRow(
            String modelId,
            String abilityType,
            String providerCode,
            String modelCode,
            String modelName,
            String qualityLevel,
            String priceUnit,
            int unitPriceCent,
            int timeoutSec,
            int maxRetry,
            String configJson) {
    }
}
