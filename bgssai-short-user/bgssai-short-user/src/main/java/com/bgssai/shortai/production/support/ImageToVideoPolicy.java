package com.bgssai.shortai.production.support;

import java.util.Map;

import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.model.ShortAiTask;

/** Retry and cancel rules for an image-to-video task. The provider has neither operation. */
public final class ImageToVideoPolicy {

    public static final String TARGET = "IMAGE_TO_VIDEO";

    private ImageToVideoPolicy() {
    }

    public static void assertCanRetry(ShortAiTask task, boolean fallback) {
        if (task == null || !TARGET.equals(task.getTargetType())) {
            return;
        }
        if (fallback || hasProviderJob(task) || "I2V_AMBIGUOUS".equals(task.getFailReasonCode())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, ImageConditionedVideoProtocol.NO_RETRY);
        }
    }

    public static void assertCanCancel(ShortAiTask task) {
        if (task == null || !TARGET.equals(task.getTargetType())) {
            return;
        }
        if (hasProviderJob(task) || !"QUEUED".equals(task.getTaskStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, ImageConditionedVideoProtocol.NO_CANCEL);
        }
    }

    public static boolean hasProviderJob(ShortAiTask task) {
        if (task == null) {
            return false;
        }
        return !ShortJson.text(ShortJson.map(task.getRequestJson()).get("provider_job_id")).isBlank();
    }

    public static Map<String, Object> scrubResult(Map<String, Object> json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        json.remove("metrics");
        json.remove("text_cache_hit");
        json.remove("device");
        json.remove("dtype");
        json.remove("offload");
        json.remove("gpu_used");
        json.remove("model");
        json.remove("stage");
        json.remove("error");
        json.remove("status_url");
        json.remove("provider_job_id");
        json.remove("base_url");
        json.remove("content");
        return json;
    }
}
