package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class PlayableVideoDurationTest {

    @Test
    void fractionalVideoStreamDurationFloorsToComposeSafeWholeSeconds() {
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classes = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Integer ltx = PlayableVideoDuration.floorSeconds(new byte[32],
                List.of(javaExecutable, "-cp", classes, StubProbe.class.getName(), "3.125"), 5_000L);
        assertEquals(3, ltx);

        Integer cog = PlayableVideoDuration.floorSeconds(new byte[32],
                List.of(javaExecutable, "-cp", classes, StubProbe.class.getName(), "6.125"), 5_000L);
        assertEquals(6, cog);

        Integer jitter = PlayableVideoDuration.floorSeconds(new byte[32],
                List.of(javaExecutable, "-cp", classes, StubProbe.class.getName(), "2.98"), 5_000L);
        assertEquals(3, jitter);
    }

    public static class StubProbe {
        public static void main(String[] args) {
            System.out.print("{\"streams\":[{\"codec_type\":\"video\",\"duration\":\"" + args[0]
                    + "\"}],\"format\":{\"duration\":\"9.99\"}}");
        }
    }
}
