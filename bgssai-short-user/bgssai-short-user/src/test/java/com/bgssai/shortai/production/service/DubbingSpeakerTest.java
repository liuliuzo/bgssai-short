package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortShot;

class DubbingSpeakerTest {
    @Test void speakingRoleWinsForBothObjectAndIdReferenceFormats() {
        ShortCharacter first = new ShortCharacter(); first.setCharacterId("first"); first.setVoiceId("v-female-warm-01");
        ShortCharacter speaker = new ShortCharacter(); speaker.setCharacterId("speaker"); speaker.setVoiceId("v-male-deep-01");
        Map<String, ShortCharacter> cast = new LinkedHashMap<>(); cast.put("first", first); cast.put("speaker", speaker);
        DubbingService service = new DubbingService();
        for (String refs : java.util.List.of("[{\"character_id\":\"speaker\"},{\"character_id\":\"first\"}]", "[\"speaker\",\"first\"]")) {
            ShortShot shot = new ShortShot(); shot.setCharacterRefJson(refs);
            ShortCharacter resolved = ReflectionTestUtils.invokeMethod(service, "resolveCharacter", shot, cast);
            assertEquals("v-male-deep-01", resolved.getVoiceId());
        }
    }
}
