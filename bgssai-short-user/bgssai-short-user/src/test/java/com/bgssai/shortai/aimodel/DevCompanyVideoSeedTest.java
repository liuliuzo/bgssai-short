package com.bgssai.shortai.aimodel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Seed contract for the DEV official generate-video default.
 * Production DML must not gain this default.
 */
class DevCompanyVideoSeedTest {

    @Test
    void sharedSeedAddsEnabledCompanyVideoWithoutTakingTheDefault() throws Exception {
        String shared = Files.readString(root().resolve("sql/DML.sql"));
        String statement = statementContaining(shared, "'m-tokenhub-video-bgssai'");
        assertTrue(statement.contains("'VIDEO'"));
        assertTrue(statement.contains("'TENCENT_TOKENHUB'"));
        assertTrue(statement.contains("'bgssai-video'"));
        assertTrue(statement.contains("\"hub\":\"cn\""));
        assertTrue(statement.contains("\"secret_ref\":\"ai.tokenhub_cn.api_key\""));
        assertTrue(statement.contains(", 20, 0, 1, 900, 1,"));
        String duplicate = statement.substring(statement.indexOf("ON DUPLICATE"));
        assertFalse(duplicate.contains("default_flag"));
        assertFalse(statement.contains("sk-"));
    }

    @Test
    void devSeedMakesCompanyVideoTheOnlyVideoDefault() throws Exception {
        String dev = Files.readString(root().resolve("sql/dev/DML.sql"));
        int at = dev.indexOf("2026-11-12");
        assertTrue(at > 0);
        String block = dev.substring(at);
        assertTrue(block.contains("'m-tokenhub-video-bgssai'"));
        assertTrue(block.contains("'bgssai-video'"));
        assertTrue(block.contains("\"hub\":\"cn\""));
        assertTrue(block.contains("\"secret_ref\":\"ai.tokenhub_cn.api_key\""));
        assertTrue(block.contains("`default_flag` = 1"));
        assertTrue(block.contains("`timeout_sec` = 900"));
        assertTrue(block.contains("SET `default_flag` = 0 WHERE `model_id` = 'm-tokenhub-video-minimax'"));
        assertTrue(block.contains("`model_code` = 'cogvideox'"));
        assertTrue(block.contains("NOT (`provider_code` = 'TENCENT_TOKENHUB' AND `model_code` = 'bgssai-video')"));
        assertFalse(block.contains("secret_value"));
        assertFalse(block.contains("sk-"));
        int promote = block.indexOf("`default_flag` = 1");
        int clearMinimax = block.indexOf("SET `default_flag` = 0 WHERE `model_id` = 'm-tokenhub-video-minimax'");
        assertTrue(promote > 0 && clearMinimax > promote);
        assertTrue(block.lastIndexOf("`default_flag` = 1") > block.indexOf("`model_code` = 'cogvideox'"));
    }

    @Test
    void productionSeedDoesNotFlipVideoDefaults() throws Exception {
        String prod = Files.readString(root().resolve("sql/prod/DML.sql"));
        assertFalse(prod.contains("m-tokenhub-video-bgssai"));
        assertFalse(prod.contains("bgssai-video"));
        assertFalse(prod.contains("m-tokenhub-video-minimax"));
        assertFalse(prod.contains("cogvideox"));
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
