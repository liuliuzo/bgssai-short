package com.bgssai.shortai.aiprovider;

import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.ContentDecision;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.PollDecision;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.SubmitDecision;
import com.bgssai.shortai.model.AiModelConfig;

/**
 * Server-side image-to-video calls. Implementations must not fetch an
 * arbitrary URL from the caller and must not fall back to text-to-video.
 */
public interface ImageConditionedVideoClient {

    boolean ready(AiModelConfig model);

    /** An already accepted job may still be polled after the row is disabled. */
    boolean callable(AiModelConfig model);

    SubmitDecision submit(AiModelConfig model, byte[] image, String mime, String prompt,
            int width, int height, int frames, int fps, int steps, int seed);

    PollDecision poll(AiModelConfig model, String jobId);

    ContentDecision fetch(AiModelConfig model, String jobId);
}
