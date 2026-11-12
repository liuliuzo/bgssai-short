package com.bgssai.shortai.aiprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.ContentKind;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.PollKind;
import com.bgssai.shortai.aiprovider.ImageConditionedVideoProtocol.SubmitKind;

class ImageConditionedVideoProtocolTest {

    private static final String JOB = "i2v_" + "ab".repeat(16);

    @Test
    void acceptOnlyMatching202() {
        String body = "{\"id\":\"" + JOB + "\",\"status\":\"queued\",\"status_url\":\"/v1/videos/" + JOB + "\"}";
        var accepted = ImageConditionedVideoProtocol.interpretSubmit(202, body);
        assertEquals(SubmitKind.ACCEPTED, accepted.kind());
        assertEquals(JOB, accepted.jobId());
        String wrong = "{\"id\":\"" + JOB + "\",\"status\":\"queued\",\"status_url\":\"https://evil.example/v1/videos/" + JOB + "\"}";
        assertEquals(SubmitKind.AMBIGUOUS, ImageConditionedVideoProtocol.interpretSubmit(202, wrong).kind());
        assertEquals(SubmitKind.AMBIGUOUS, ImageConditionedVideoProtocol.interpretSubmit(500, body).kind());
        assertEquals(SubmitKind.AMBIGUOUS, ImageConditionedVideoProtocol.interpretSubmit(-1, "").kind());
    }

    @Test
    void busyAndUnavailableStayDistinct() {
        assertEquals(SubmitKind.BUSY, ImageConditionedVideoProtocol.interpretSubmit(429, "{\"detail\":\"One active job maximum\"}").kind());
        assertEquals(ImageConditionedVideoProtocol.BUSY,
                ImageConditionedVideoProtocol.interpretSubmit(429, "{\"detail\":\"One active job maximum\"}").userMessage());
        assertEquals(SubmitKind.UNAVAILABLE, ImageConditionedVideoProtocol.interpretSubmit(503, "{\"detail\":\"ram\"}").kind());
        assertFalse(ImageConditionedVideoProtocol.interpretSubmit(503, "{\"detail\":\"ram\"}").userMessage().toLowerCase().contains("ram"));
        assertEquals(SubmitKind.REJECTED, ImageConditionedVideoProtocol.interpretSubmit(413, "{\"detail\":\"Image too large\"}").kind());
        var pixels = ImageConditionedVideoProtocol.interpretSubmit(400, "{\"detail\":\"Invalid image\"}");
        assertEquals(SubmitKind.REJECTED, pixels.kind());
        assertFalse(pixels.userMessage().toLowerCase().contains("invalid"));
    }

    @Test
    void http200FailedDoesNotExposeProviderError() {
        String body = "{\"status\":\"failed\",\"error\":\"ltx-video worker exploded\",\"metrics\":{\"dtype\":\"bf16\"}}";
        var failed = ImageConditionedVideoProtocol.interpretPoll(JOB, 200, body);
        assertEquals(PollKind.FAILED, failed.kind());
        assertEquals(ImageConditionedVideoProtocol.FAILED, failed.userMessage());
        assertFalse(failed.userMessage().toLowerCase().contains("ltx"));
        assertFalse(failed.userMessage().contains("bf16"));
        assertEquals(PollKind.RUNNING, ImageConditionedVideoProtocol.interpretPoll(JOB, 200, "{\"status\":\"running\",\"stage\":\"infer\"}").kind());
        assertEquals(PollKind.TRANSIENT, ImageConditionedVideoProtocol.interpretPoll(JOB, 503, "{\"detail\":\"busy\"}").kind());
        assertEquals(PollKind.TRANSIENT, ImageConditionedVideoProtocol.interpretPoll(JOB, 429, "{}").kind());
    }

    @Test
    void contentIsFetchedOnlyForTheRelativeCompletedPath() {
        String ready = "{\"status\":\"completed\",\"content\":\"/v1/videos/" + JOB
                + "/content\",\"metrics\":{\"device\":\"cpu\",\"text_cache_hit\":true,\"model\":\"Lightricks/LTX-Video-0.9.5\"}}";
        var completed = ImageConditionedVideoProtocol.interpretPoll(JOB, 200, ready);
        assertEquals(PollKind.COMPLETED, completed.kind());
        assertEquals("", completed.userMessage());
        assertFalse(completed.userMessage().toLowerCase().contains("lightricks"));
        String absolute = "{\"status\":\"completed\",\"content\":\"https://i2v.invalid/v1/videos/" + JOB + "/content\"}";
        assertEquals(PollKind.RUNNING, ImageConditionedVideoProtocol.interpretPoll(JOB, 200, absolute).kind());
        byte[] mp4 = new byte[] {0, 0, 0, 20, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
        assertEquals(ContentKind.NOT_READY, ImageConditionedVideoProtocol.interpretContent(409, "application/json", "{}".getBytes()).kind());
        assertEquals(ContentKind.NOT_READY, ImageConditionedVideoProtocol.interpretContent(503, "application/json", new byte[0]).kind());
        assertEquals(ContentKind.READY, ImageConditionedVideoProtocol.interpretContent(200, "video/mp4", mp4).kind());
        assertEquals(ContentKind.FAILED, ImageConditionedVideoProtocol.interpretContent(200, "application/json", mp4).kind());
        assertEquals(ContentKind.FAILED, ImageConditionedVideoProtocol.interpretContent(200, "video/mp4", "not-a-video".getBytes()).kind());
        assertTrue(ImageConditionedVideoProtocol.mp4(mp4));
    }

    @Test
    void pathsStayRelative() {
        assertEquals("/v1/videos/" + JOB, ImageConditionedVideoProtocol.statusPath(JOB));
        assertEquals("/v1/videos/" + JOB + "/content", ImageConditionedVideoProtocol.contentPath(JOB));
        assertEquals(5_000L, ImageConditionedVideoProtocol.pollDelayMs(0));
        assertEquals(15_000L, ImageConditionedVideoProtocol.pollDelayMs(9));
        assertEquals(14_400, ImageConditionedVideoProtocol.DEADLINE_SEC);
        assertEquals(1258, ImageConditionedVideoProtocol.OBSERVED_RUN_SEC);
        assertTrue(ImageConditionedVideoProtocol.DEADLINE_SEC > ImageConditionedVideoProtocol.OBSERVED_RUN_SEC);
        assertEquals("v0.2-validated-20261111-1137Z", ImageConditionedVideoProtocol.CONTRACT);
    }
}
