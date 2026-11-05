package com.bgssai.shortai.aimodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.production.service.AiTaskQueue;
import com.bgssai.shortai.secret.SysSecretReader;

class PipelineReadinessTest {

    @Test
    void blocksUnlistedVoiceAndUnreadyTextWithoutSwitchingImageVendor() {
        AiModelResolveService models = org.mockito.Mockito.mock(AiModelResolveService.class);
        when(models.resolve("TEXT", null)).thenReturn(model("deepseek-chat"));
        when(models.resolve("IMAGE", null)).thenReturn(model("kling-v2-1"));
        when(models.resolve("VOICE", null)).thenReturn(model("minimax-speech-2.8-hd"));
        ScriptedGateway gateway = new ScriptedGateway(false, catalog(false));
        PipelineReadiness readiness = new PipelineReadiness(models,
                new SysSecretReader(key -> "sk-test"),
                "https://dev.user.bgssai-tokenhub.cn",
                gateway);
        BizException ex = assertThrows(BizException.class, () -> readiness.assertReady("TEXT", null));
        assertEquals(CommonErrorCode.ABILITY_UNAVAILABLE.code(), ex.getCode());
        assertTrue(ex.getMessage().contains("大纲、剧本、分镜"));
        assertTrue(ex.getMessage().contains("模型服务暂未开通"));
        assertTrue(ex.getMessage().contains("配音"));
        assertTrue(ex.getMessage().contains("该模型暂未开放：BGSSAI TTS"));
        assertTrue(ex.getMessage().contains("请稍后重试"));
        assertFalse(ex.getMessage().toLowerCase().contains("deepseek"));
        assertFalse(ex.getMessage().toLowerCase().contains("minimax"));
        assertFalse(ex.getMessage().contains("kling-v2-1"));
        assertFalse(ex.getMessage().contains("qwen-tts"));
        assertFalse(ex.getMessage().contains("provider_not_configured"));
        assertTrue(gateway.posted.stream().anyMatch(body -> body.contains("\"model\":\"kling-v3\"")));
        assertFalse(gateway.posted.stream().anyMatch(body -> body.contains("kling-v2-1")));
        assertFalse(gateway.posted.stream().anyMatch(body -> body.contains("qwen-tts")));
    }

    @Test
    void usesRegisteredMinimaxSpeechOnceItAppears() {
        AiModelResolveService models = org.mockito.Mockito.mock(AiModelResolveService.class);
        when(models.resolve("TEXT", null)).thenReturn(model("deepseek-chat"));
        when(models.resolve("IMAGE", null)).thenReturn(model("kling-v3"));
        when(models.resolve("VOICE", null)).thenReturn(model("minimax-speech-2.8-hd"));
        ScriptedGateway gateway = new ScriptedGateway(true, catalog(true));
        PipelineReadiness readiness = new PipelineReadiness(models,
                new SysSecretReader(key -> "sk-test"),
                "https://dev.user.bgssai-tokenhub.cn",
                gateway);
        readiness.assertReady("VOICE", null);
        assertTrue(gateway.posted.stream().anyMatch(body -> body.contains("\"model\":\"speech-02-hd\"")));
        assertFalse(gateway.posted.stream().anyMatch(body -> body.contains("minimax-speech-2.8-hd")));
        assertFalse(gateway.posted.stream().anyMatch(body -> body.contains("qwen-tts")));
    }

    @Test
    void explicitUnlistedModelIsReportedAsAMismatch() {
        AiModelResolveService models = org.mockito.Mockito.mock(AiModelResolveService.class);
        when(models.resolve("TEXT", null)).thenReturn(model("deepseek-chat"));
        when(models.resolve("IMAGE", "kling-v2-1")).thenReturn(model("kling-v2-1"));
        when(models.resolve("VOICE", null)).thenReturn(model("minimax-speech-2.8-hd"));
        ScriptedGateway gateway = new ScriptedGateway(true, catalog(true));
        PipelineReadiness readiness = new PipelineReadiness(models,
                new SysSecretReader(key -> "sk-test"),
                "https://dev.user.bgssai-tokenhub.cn",
                gateway);
        BizException ex = assertThrows(BizException.class, () -> readiness.assertReady("IMAGE", "kling-v2-1"));
        assertTrue(ex.getMessage().contains("该模型暂未开放：BGSSAI Image"));
        assertFalse(ex.getMessage().toLowerCase().contains("kling"));
        assertFalse(gateway.posted.stream().anyMatch(body -> body.contains("\"model\":\"kling-v3\"")));
    }

    @Test
    void configuredModelsPassAndComposeIsNotGated() {
        AiModelResolveService models = org.mockito.Mockito.mock(AiModelResolveService.class);
        when(models.resolve("TEXT", null)).thenReturn(model("deepseek-chat"));
        when(models.resolve("IMAGE", null)).thenReturn(model("kling-v3"));
        when(models.resolve("VOICE", null)).thenReturn(model("qwen-tts"));
        PipelineReadiness readiness = new PipelineReadiness(models,
                new SysSecretReader(key -> "sk-test"),
                "https://dev.user.bgssai-tokenhub.cn",
                new ScriptedGateway(true, catalog(false)));
        readiness.assertReady("IMAGE", null);
        readiness.assertReady("COMPOSE", null);
        verify(models, never()).resolve(org.mockito.ArgumentMatchers.eq("COMPOSE"), any());
    }

    @Test
    void companyImageAndSpeechSchemaRejectionDoesNotBlockCreate() {
        AiModelResolveService models = org.mockito.Mockito.mock(AiModelResolveService.class);
        when(models.resolve("TEXT", null)).thenReturn(model("bgssai-chat"));
        when(models.resolve("IMAGE", null)).thenReturn(model("bgssai-image"));
        when(models.resolve("VOICE", null)).thenReturn(model("bgssai-tts"));
        ScriptedGateway gateway = new ScriptedGateway(true, companyCatalog(), 422);
        PipelineReadiness readiness = new PipelineReadiness(models,
                new SysSecretReader(key -> "sk-test"),
                "https://dev.user.bgssai-tokenhub.cn",
                gateway);
        readiness.assertReady("TEXT", null);
        assertTrue(gateway.posted.stream().anyMatch(body -> body.contains("\"model\":\"bgssai-image\"")));
        assertTrue(gateway.posted.stream().anyMatch(body -> body.contains("\"model\":\"bgssai-tts\"")));
        assertFalse(gateway.posted.stream().anyMatch(body -> body.contains("kling-v3")));
    }

    @Test
    void videoTaskChecksTheCatalogAndDoesNotPostAVideoProbe() {
        AiModelResolveService models = org.mockito.Mockito.mock(AiModelResolveService.class);
        when(models.resolve("TEXT", null)).thenReturn(model("deepseek-chat"));
        when(models.resolve("IMAGE", null)).thenReturn(model("kling-v3"));
        when(models.resolve("VOICE", null)).thenReturn(model("speech-02-hd"));
        ScriptedGateway gateway = new ScriptedGateway(true, catalog(true));
        PipelineReadiness readiness = new PipelineReadiness(models,
                new SysSecretReader(key -> "sk-test"),
                "https://dev.user.bgssai-tokenhub.cn",
                gateway);
        BizException ex = assertThrows(BizException.class, () -> readiness.assertReady("VIDEO", null));
        assertTrue(ex.getMessage().contains("镜头视频"));
        assertTrue(ex.getMessage().contains("没有可用模型"));
        assertEquals(3, gateway.posted.size());
        assertFalse(gateway.posted.stream().anyMatch(body -> body.contains("cogvideox") || body.contains("/videos")));
    }

    @Test
    void compatKeyUsesCompanyCatalogWithoutPostingGeneration() {
        AiModelResolveService models = org.mockito.Mockito.mock(AiModelResolveService.class);
        ScriptedGateway gateway = new ScriptedGateway(true, companyDirectCatalog(), 400,
                "https://chat.bgssai.com/v1/models", "sk-company");
        PipelineReadiness readiness = new PipelineReadiness(models,
                new SysSecretReader(key -> SysSecretReader.AI_OPENAI_COMPAT_API_KEY.equals(key) ? "sk-company" : ""),
                "https://dev.user.bgssai-tokenhub.cn",
                gateway);
        readiness.assertReady("IMAGE", null);
        readiness.assertReady("VIDEO", null);
        assertTrue(gateway.posted.isEmpty());
        assertTrue(gateway.got.stream().allMatch(url -> url.startsWith("https://chat.bgssai.com/v1/models")));
        verify(models, never()).resolve(any(), any());
    }

    @Test
    void queueDoesNotInsertWhenThePipelineIsBlocked() {
        AiTaskQueue queue = new AiTaskQueue();
        PipelineReadiness readiness = org.mockito.Mockito.mock(PipelineReadiness.class);
        ShortAiTaskMapper mapper = org.mockito.Mockito.mock(ShortAiTaskMapper.class);
        org.mockito.Mockito.doThrow(new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                "大纲、剧本、分镜现在不能开始：模型服务暂未开通，请稍后重试"))
                .when(readiness).assertReady("TEXT", null);
        ReflectionTestUtils.setField(queue, "pipelineReadiness", readiness);
        ReflectionTestUtils.setField(queue, "shortAiTaskMapper", mapper);
        BizException ex = assertThrows(BizException.class,
                () -> queue.enqueue("u", "w", null, "TEXT", "OUTLINE", "w", java.util.Map.of(), null, 0));
        assertEquals("4014", ex.getCode());
        verify(mapper, never()).insertSelective(any());
    }

    private static String companyDirectCatalog() {
        return "{\"data\":[{\"id\":\"qwen\"},{\"id\":\"sd-turbo\"},{\"id\":\"piper\"},{\"id\":\"cogvideox\"}]}";
    }

    private static String companyCatalog() {
        return "{\"data\":["
                + "{\"id\":\"bgssai-chat\",\"owned_by\":\"BGSSAI\",\"capabilities\":[\"chat\"]},"
                + "{\"id\":\"bgssai-image\",\"owned_by\":\"BGSSAI\",\"capabilities\":[\"image_generation\"]},"
                + "{\"id\":\"bgssai-tts\",\"owned_by\":\"BGSSAI\",\"capabilities\":[\"audio_speech\"]}"
                + "]}";
    }

    private static String catalog(boolean minimaxSpeech) {
        String speech = minimaxSpeech
                ? ",{\"id\":\"speech-02-hd\",\"owned_by\":\"MiniMax\",\"capabilities\":[\"audio_speech\"]}"
                : "";
        return "{\"data\":["
                + "{\"id\":\"deepseek-chat\",\"capabilities\":[\"chat\"]},"
                + "{\"id\":\"kling-v3\",\"owned_by\":\"快手可灵\",\"capabilities\":[\"image_generation\",\"video_generation\"]},"
                + "{\"id\":\"kling-v3-omni\",\"owned_by\":\"快手可灵\",\"capabilities\":[\"image_generation\"]},"
                + "{\"id\":\"qwen-tts\",\"owned_by\":\"dashscope\",\"capabilities\":[\"audio_speech\"]}"
                + speech
                + "]}";
    }

    private static AiModelConfig model(String code) {
        AiModelConfig row = new AiModelConfig();
        row.setProviderCode("TENCENT_TOKENHUB");
        row.setModelCode(code);
        row.setAbilityType("TEXT");
        return row;
    }

    private static final class ScriptedGateway implements PipelineReadiness.GatewayClient {
        private final boolean deepseekReady;
        private final String catalogBody;
        private final int validationStatus;
        private final String urlPrefix;
        private final String expectedKey;
        private final List<String> posted = new ArrayList<>();
        private final List<String> got = new ArrayList<>();

        private ScriptedGateway(boolean deepseekReady, String catalogBody) {
            this(deepseekReady, catalogBody, 400);
        }

        private ScriptedGateway(boolean deepseekReady, String catalogBody, int validationStatus) {
            this(deepseekReady, catalogBody, validationStatus,
                    "https://dev.user.bgssai-tokenhub.cn/v1/models", "sk-test");
        }

        private ScriptedGateway(boolean deepseekReady, String catalogBody, int validationStatus,
                String urlPrefix, String expectedKey) {
            this.deepseekReady = deepseekReady;
            this.catalogBody = catalogBody;
            this.validationStatus = validationStatus;
            this.urlPrefix = urlPrefix;
            this.expectedKey = expectedKey;
        }

        @Override
        public Reply get(String url, String apiKey) {
            got.add(url);
            assertTrue(url.startsWith(urlPrefix));
            assertEquals(expectedKey, apiKey);
            return new Reply(200, catalogBody);
        }

        @Override
        public Reply post(String url, String apiKey, String json) {
            posted.add(json);
            if (!deepseekReady && json.contains("deepseek-chat")) {
                return new Reply(503, "{\"error\":{\"message\":\"Provider is not configured: deepseek\","
                        + "\"type\":\"service_unavailable_error\",\"code\":\"provider_not_configured\"}}");
            }
            if (validationStatus == 422 && json.contains("bgssai-image")) {
                return new Reply(422, "{\"detail\":[{\"type\":\"greater_than_equal\",\"loc\":[\"body\",\"n\"],"
                        + "\"msg\":\"Input should be greater than or equal to 1\"}]}");
            }
            if (validationStatus == 422 && json.contains("bgssai-tts")) {
                return new Reply(422, "{\"detail\":[{\"type\":\"missing\",\"loc\":[\"body\",\"input\"],"
                        + "\"msg\":\"Field required\"}]}");
            }
            return new Reply(400, "{\"error\":{\"message\":\"messages is required\",\"code\":\"invalid_request\"}}");
        }
    }
}
