package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.model.ShortAiTask;

class ImageToVideoPolicyTest {

    @Test
    void acceptedJobCannotBeCanceledOrRetriedIntoTextToVideo() {
        ShortAiTask queued = task("QUEUED", "{}");
        ImageToVideoPolicy.assertCanCancel(queued);
        ImageToVideoPolicy.assertCanRetry(task("FAILED", "{}"), false);
        ShortAiTask accepted = task("RUNNING", "{\"provider_job_id\":\"i2v_" + "ab".repeat(16) + "\"}");
        assertThrows(BizException.class, () -> ImageToVideoPolicy.assertCanCancel(accepted));
        assertThrows(BizException.class, () -> ImageToVideoPolicy.assertCanRetry(accepted, false));
        ShortAiTask ambiguous = task("FAILED", "{}");
        ambiguous.setFailReasonCode("I2V_AMBIGUOUS");
        BizException retry = assertThrows(BizException.class, () -> ImageToVideoPolicy.assertCanRetry(ambiguous, false));
        assertFalse(retry.getMessage().toLowerCase().contains("ltx"));
        assertThrows(BizException.class, () -> ImageToVideoPolicy.assertCanRetry(task("FAILED", "{}"), true));
        assertTrue(ImageToVideoPolicy.hasProviderJob(accepted));
        assertFalse(ImageToVideoPolicy.hasProviderJob(queued));
    }

    @Test
    void clientResultDropsProviderFields() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("video_asset_id", "a-1");
        json.put("metrics", Map.of("dtype", "bf16", "text_cache_hit", true));
        json.put("text_cache_hit", true);
        json.put("model", "Lightricks/LTX-Video-0.9.5");
        json.put("error", "ltx-video exploded");
        json.put("provider_job_id", "i2v_secret");
        json.put("base_url", "http://127.0.0.1:8007");
        json.put("status_url", "/v1/videos/x");
        Map<String, Object> scrubbed = ImageToVideoPolicy.scrubResult(json);
        assertEquals("a-1", scrubbed.get("video_asset_id"));
        assertFalse(scrubbed.containsKey("metrics"));
        assertFalse(scrubbed.containsKey("provider_job_id"));
        assertFalse(scrubbed.containsKey("base_url"));
        assertFalse(scrubbed.containsKey("status_url"));
        assertFalse(scrubbed.containsKey("text_cache_hit"));
        assertFalse(scrubbed.containsKey("model"));
        assertFalse(scrubbed.containsKey("error"));
    }

    private static ShortAiTask task(String status, String request) {
        ShortAiTask task = new ShortAiTask();
        task.setTargetType(ImageToVideoPolicy.TARGET);
        task.setTaskStatus(status);
        task.setRequestJson(request);
        return task;
    }
}
