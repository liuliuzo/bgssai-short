package com.bgssai.shortai.aimodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.ibatis.annotations.Param;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import com.bgssai.shortai.mapper.AiModelConfigMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.AiModelConfigExample;

class CompanyModelSeederTest {

    @Test
    void backfillsMissingCompanyRowsAndPromotesLegacyDefaultsWithoutDuplicateKeyCollision() {
        InMemoryModelMapper mapper = new InMemoryModelMapper();

        // Simulate a database where m-tokenhub-video-cogvideox already owns ('VIDEO', 'TENCENT_TOKENHUB', 'bgssai-video')
        // and legacy Kling / DeepSeek / MiniMax rows still hold default_flag=1.
        mapper.add("m-tokenhub-text-deepseek", "TEXT", "TENCENT_TOKENHUB", "deepseek-chat", 1);
        mapper.add("m-tokenhub-image-kling", "IMAGE", "TENCENT_TOKENHUB", "kling-v3", 1);
        mapper.add("m-tokenhub-video-minimax", "VIDEO", "TENCENT_TOKENHUB", "minimax-video-h3", 1);
        mapper.add("m-tokenhub-video-cogvideox", "VIDEO", "TENCENT_TOKENHUB", "bgssai-video", 0);
        mapper.add("m-tokenhub-music-minimax", "MUSIC", "TENCENT_TOKENHUB", "minimax-music-v2.6", 1);

        new CompanyModelSeeder(mapper).run(new DefaultApplicationArguments());

        // No duplicate ('VIDEO', 'TENCENT_TOKENHUB', 'bgssai-video') inserted.
        long companyVideoCount = mapper.rows.stream()
                .filter(r -> "VIDEO".equals(r.getAbilityType())
                        && "TENCENT_TOKENHUB".equals(r.getProviderCode())
                        && "bgssai-video".equals(r.getModelCode()))
                .count();
        assertEquals(1L, companyVideoCount);

        assertEquals("bgssai-chat", mapper.defaultOf("TEXT").getModelCode());
        assertEquals("bgssai-image", mapper.defaultOf("IMAGE").getModelCode());
        assertEquals("bgssai-tts", mapper.defaultOf("VOICE").getModelCode());
        assertEquals("bgssai-video", mapper.defaultOf("VIDEO").getModelCode());
        assertEquals("bgssai-music", mapper.defaultOf("MUSIC").getModelCode());
        assertEquals("ffmpeg-concat", mapper.defaultOf("COMPOSE").getModelCode());

        assertEquals(0, mapper.byModelId("m-tokenhub-text-deepseek").getDefaultFlag());
        assertEquals(0, mapper.byModelId("m-tokenhub-image-kling").getDefaultFlag());
        assertEquals(0, mapper.byModelId("m-tokenhub-video-minimax").getDefaultFlag());
        assertEquals(0, mapper.byModelId("m-tokenhub-music-minimax").getDefaultFlag());

        AiModelConfig i2v = mapper.byModelId("m-bgssai-image-to-video");
        assertNotNull(i2v);
        assertTrue(i2v.getConfigJson().contains("https://chat.bgssai.com/i2v"));
        assertEquals(0, i2v.getDefaultFlag());

        AiModelConfig asr = mapper.byModelId("m-compat-asr-whisper");
        assertNotNull(asr);
        assertEquals("whisper-1", asr.getModelCode());

        AiModelConfig musicCompat = mapper.byModelId("m-compat-music-musicgen");
        assertNotNull(musicCompat);
        assertEquals("musicgen-small", musicCompat.getModelCode());
    }

    @Test
    void preservesCustomAdministratorDefault() {
        InMemoryModelMapper mapper = new InMemoryModelMapper();
        mapper.add("m-custom-text", "TEXT", "TENCENT_TOKENHUB", "custom-enterprise-llm", 1);

        new CompanyModelSeeder(mapper).run(new DefaultApplicationArguments());

        assertEquals("custom-enterprise-llm", mapper.defaultOf("TEXT").getModelCode());
        assertEquals(0, mapper.byModelId("m-tokenhub-text-bgssai-chat").getDefaultFlag());
    }

    private static final class InMemoryModelMapper implements AiModelConfigMapper {
        private final List<AiModelConfig> rows = new ArrayList<>();
        private final AtomicLong seq = new AtomicLong(1L);

        void add(String modelId, String ability, String provider, String modelCode, int defaultFlag) {
            AiModelConfig row = new AiModelConfig();
            row.setId(seq.getAndIncrement());
            row.setModelId(modelId);
            row.setAbilityType(ability);
            row.setProviderCode(provider);
            row.setModelCode(modelCode);
            row.setDefaultFlag(defaultFlag);
            row.setEnabledFlag(1);
            row.setDeletedFlag(0);
            rows.add(row);
        }

        AiModelConfig byModelId(String modelId) {
            return rows.stream().filter(r -> modelId.equals(r.getModelId())).findFirst().orElse(null);
        }

        AiModelConfig defaultOf(String ability) {
            return rows.stream()
                    .filter(r -> ability.equals(r.getAbilityType()) && Integer.valueOf(1).equals(r.getDefaultFlag()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public long countByExample(AiModelConfigExample example) {
            return selectByExample(example).size();
        }

        @Override
        public int insert(AiModelConfig row) {
            return insertSelective(row);
        }

        @Override
        public int insertSelective(AiModelConfig row) {
            row.setId(seq.getAndIncrement());
            rows.add(row);
            return 1;
        }

        @Override
        public List<AiModelConfig> selectByExample(AiModelConfigExample example) {
            List<AiModelConfig> matched = new ArrayList<>();
            for (AiModelConfig row : rows) {
                if (matches(row, example)) {
                    matched.add(row);
                }
            }
            return matched;
        }

        @Override
        public AiModelConfig selectOneByExample(AiModelConfigExample example) {
            List<AiModelConfig> list = selectByExample(example);
            return list.isEmpty() ? null : list.get(0);
        }

        @Override
        public AiModelConfig selectByPrimaryKey(Long id) {
            return rows.stream().filter(r -> id.equals(r.getId())).findFirst().orElse(null);
        }

        @Override
        public int updateByExampleSelective(@Param("row") AiModelConfig patch,
                @Param("example") AiModelConfigExample example) {
            int updated = 0;
            for (AiModelConfig existing : selectByExample(example)) {
                apply(existing, patch);
                updated++;
            }
            return updated;
        }

        @Override
        public int updateByPrimaryKeySelective(AiModelConfig patch) {
            AiModelConfig existing = selectByPrimaryKey(patch.getId());
            if (existing == null) {
                return 0;
            }
            apply(existing, patch);
            return 1;
        }

        private static void apply(AiModelConfig target, AiModelConfig patch) {
            if (patch.getDefaultFlag() != null) {
                target.setDefaultFlag(patch.getDefaultFlag());
            }
            if (patch.getEnabledFlag() != null) {
                target.setEnabledFlag(patch.getEnabledFlag());
            }
            if (patch.getTimeoutSec() != null) {
                target.setTimeoutSec(patch.getTimeoutSec());
            }
            if (patch.getConfigJson() != null) {
                target.setConfigJson(patch.getConfigJson());
            }
        }

        private static boolean matches(AiModelConfig row, AiModelConfigExample example) {
            if (example == null || example.getOredCriteria().isEmpty()) {
                return true;
            }
            for (AiModelConfigExample.Criteria criteria : example.getOredCriteria()) {
                if (matchesCriteria(row, criteria)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean matchesCriteria(AiModelConfig row, AiModelConfigExample.Criteria criteria) {
            for (AiModelConfigExample.Criterion c : criteria.getAllCriteria()) {
                String cond = c.getCondition().trim();
                Object val = c.getValue();
                if ("model_id =".equals(cond) && !val.equals(row.getModelId())) {
                    return false;
                }
                if ("ability_type =".equals(cond) && !val.equals(row.getAbilityType())) {
                    return false;
                }
                if ("provider_code =".equals(cond) && !val.equals(row.getProviderCode())) {
                    return false;
                }
                if ("model_code =".equals(cond) && !val.equals(row.getModelCode())) {
                    return false;
                }
                if ("enabled_flag =".equals(cond) && !val.equals(row.getEnabledFlag())) {
                    return false;
                }
                if ("deleted_flag =".equals(cond) && !val.equals(row.getDeletedFlag())) {
                    return false;
                }
                if ("default_flag =".equals(cond) && !val.equals(row.getDefaultFlag())) {
                    return false;
                }
            }
            return true;
        }
    }
}
