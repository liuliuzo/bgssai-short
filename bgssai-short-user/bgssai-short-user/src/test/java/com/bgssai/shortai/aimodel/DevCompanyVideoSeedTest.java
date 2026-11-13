package com.bgssai.shortai.aimodel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Seed contract for the BGSSAI company multimodal defaults across shared, DEV, and PROD DML.
 * Real company API keys must never appear in SQL seeds.
 */
class DevCompanyVideoSeedTest {

    @Test
    void sharedSeedConfiguresCompanyMultimodalDefaultsAndPlaceholders() throws Exception {
        String shared = Files.readString(root().resolve("sql/DML.sql"));
        String statement = statementContaining(shared, "'m-tokenhub-video-bgssai'");
        assertTrue(statement.contains("'VIDEO'"));
        assertTrue(statement.contains("'TENCENT_TOKENHUB'"));
        assertTrue(statement.contains("'bgssai-video'"));
        assertTrue(statement.contains("\"hub\":\"cn\""));
        assertTrue(statement.contains("\"secret_ref\":\"ai.tokenhub_cn.api_key\""));
        assertTrue(statement.contains("`default_flag` = 1"));
        assertTrue(shared.contains("'m-compat-asr-whisper'"));
        assertTrue(shared.contains("'m-bgssai-image-to-video'"));
        assertTrue(shared.contains("'ai.openai_compat.api_key', ''"));
        assertTrue(shared.contains("'ai.image_to_video.api_key', ''"));
        assertFalse(shared.substring(shared.indexOf("2026-11-13")).contains("sk-"));
    }

    @Test
    void devSeedAlignsAllCompanyDefaultsWithoutLeakingSecrets() throws Exception {
        String dev = Files.readString(root().resolve("sql/dev/DML.sql"));
        assertCompanyAlignment(dev);
    }

    @Test
    void prodSeedAlignsAllCompanyDefaultsWithoutLeakingSecrets() throws Exception {
        String prod = Files.readString(root().resolve("sql/prod/DML.sql"));
        assertCompanyAlignment(prod);
    }

    private static void assertCompanyAlignment(String sql) {
        assertFalse(sql.contains("model_code = 'deepseek-chat', model_name = 'DeepSeek Chat', default_flag = 1"));
        assertFalse(sql.contains("AND model_id <> 'm-tokenhub-text-deepseek'"));
        assertFalse(sql.contains("AND `model_id` <> 'm-tokenhub-image-kling'"));

        int at = sql.indexOf("2026-11-1");
        assertTrue(at > 0);
        String block = sql.substring(at);
        assertTrue(block.contains("'m-tokenhub-text-bgssai-chat'"));
        assertTrue(block.contains("'m-tokenhub-image-bgssai'"));
        assertTrue(block.contains("'m-tokenhub-voice-bgssai-tts'"));
        assertTrue(block.contains("'m-tokenhub-video-bgssai'"));
        assertTrue(block.contains("'m-compat-asr-whisper'"));
        assertTrue(block.contains("'m-bgssai-image-to-video'"));
        assertTrue(block.contains("SET `default_flag` = 0 WHERE `model_id` = 'm-tokenhub-video-minimax'"));
        assertTrue(block.contains("NOT (`provider_code` = 'TENCENT_TOKENHUB' AND `model_code` = 'bgssai-video')"));
        assertTrue(block.contains("'ai.openai_compat.api_key', ''"));
        assertTrue(block.contains("'ai.image_to_video.api_key', ''"));
        assertFalse(block.contains("sk-"));
    }

    private static String statementContaining(String sql, String marker) {
        int at = sql.indexOf(marker);
        assertTrue(at > 0, marker);
        int start = sql.lastIndexOf("INSERT INTO", at);
        int end = sql.indexOf(';', at);
        assertTrue(start >= 0 && end > at);
        return sql.substring(start, end);
    }

    private static Path root() {
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.isRegularFile(dir.resolve("sql/DML.sql"))
                    && Files.isRegularFile(dir.resolve("sql/dev/DML.sql"))
                    && Files.isRegularFile(dir.resolve("sql/prod/DML.sql"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("repo root not found from " + Path.of("").toAbsolutePath());
    }
}
