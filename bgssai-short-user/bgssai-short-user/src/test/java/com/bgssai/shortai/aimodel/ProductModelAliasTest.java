package com.bgssai.shortai.aimodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ProductModelAliasTest {

    @Test
    void storedGatewayIdsBecomeProductNames() {
        assertEquals("bgssai-chat", ProductModelAlias.forStored("TEXT", "deepseek-v4-pro").code());
        assertEquals("BGSSAI Chat", ProductModelAlias.forStored("TEXT", "qwen").name());
        assertEquals("BGSSAI Image", ProductModelAlias.forStored("IMAGE", "kling-v3").name());
        assertEquals("BGSSAI Image", ProductModelAlias.forStored("IMAGE", "sd-turbo").name());
        assertEquals("BGSSAI Video", ProductModelAlias.forStored("VIDEO", "cogvideox").name());
        assertEquals("bgssai-video", ProductModelAlias.forStored("VIDEO", "bgssai-video-i2v").code());
        assertEquals("BGSSAI Video", ProductModelAlias.forStored("VIDEO", "bgssai-video-i2v").name());
        assertEquals("BGSSAI TTS", ProductModelAlias.forStored("VOICE", "piper").name());
        assertEquals("BGSSAI ASR", ProductModelAlias.forStored("ASR", "hy-asr-3.0-preview").name());
        assertEquals("BGSSAI Music", ProductModelAlias.forStored("MUSIC", "minimax-music-v2.6").name());
        assertEquals("ffmpeg-local", ProductModelAlias.forStored("COMPOSE", "ffmpeg-local").code());
    }

    @Test
    void scrubDropsVendorTokensFromSentences() {
        String text = ProductModelAlias.scrub("模型服务暂未开通：deepseek，请稍后重试");
        assertEquals("模型服务暂未开通，请稍后重试", text);
        assertFalse(text.toLowerCase().contains("deepseek"));
        assertFalse(ProductModelAlias.scrub("该模型暂未开放：kling-v3").toLowerCase().contains("kling"));
        assertEquals("上游拒绝", ProductModelAlias.scrub("上游拒绝"));
    }

    @Test
    void explicitAliasMustStayOnTheProductPrefix() {
        assertEquals("bgssai-chat-pro", ProductModelAlias.explicitAlias(
                "{\"public_alias\":\"bgssai-chat-pro\",\"public_name\":\"BGSSAI Chat Pro\"}"));
        assertEquals("", ProductModelAlias.explicitAlias("{\"public_alias\":\"kling-v3\"}"));
        ProductModelAlias.Face face = ProductModelAlias.faceOf("TEXT", "qwen",
                "{\"public_alias\":\"bgssai-chat-pro\",\"public_name\":\"BGSSAI Chat Pro\"}");
        assertEquals("bgssai-chat-pro", face.code());
        assertEquals("BGSSAI Chat Pro", face.name());
    }

    @Test
    void hideStoredModelReplacesTheClientFields() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("model_code", "minimax-speech-2.8-hd");
        ProductModelAlias.hideStoredModel(item, "VOICE", "minimax-speech-2.8-hd");
        assertEquals("bgssai-tts", item.get("model_code"));
        assertEquals("BGSSAI TTS", item.get("display_name"));
        assertTrue(ProductModelAlias.leaks("可灵 kling-v3"));
        assertTrue(ProductModelAlias.leaks("ltx-video-2b-0.9.5"));
        assertTrue(ProductModelAlias.leaks("lightricks"));
        String hidden = ProductModelAlias.scrub("失败 ltx-video 127.0.0.1:8007");
        assertFalse(hidden.toLowerCase().contains("ltx"));
        assertFalse(hidden.contains("127.0.0.1"));
        assertFalse(hidden.contains("8007"));
        assertFalse(ProductModelAlias.leaks("BGSSAI Image"));
    }
}
