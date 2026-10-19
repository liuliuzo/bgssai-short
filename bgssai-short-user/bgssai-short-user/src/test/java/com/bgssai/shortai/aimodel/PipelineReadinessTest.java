package com.bgssai.shortai.aimodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void blocksEveryUnavailablePipelineStepBeforeQueueing() {
        AiModelResolveService models = org.mockito.Mockito.mock(AiModelResolveService.class);
        when(models.resolve("TEXT", null)).thenReturn(model("deepseek-chat"));
        when(models.resolve("IMAGE", null)).thenReturn(model("kling-v2-1"));
        when(models.resolve("VOICE", null)).thenReturn(model("minimax-speech-2.8-hd"));
        PipelineReadiness readiness = new PipelineReadiness(models,
                new SysSecretReader(key -> "sk-test"),
                "https://dev.user.bgssai-tokenhub.cn",
                new ScriptedGateway(false));
        BizException ex = assertThrows(BizException.class, () -> readiness.assertReady("TEXT", null));
        assertEquals(CommonErrorCode.ABILITY_UNAVAILABLE.code(), ex.getCode());
        assertTrue(ex.getMessage().contains("大纲、剧本、分镜现在不能开始：模型服务暂未开通：deepseek")
                || ex.getMessage().contains("大纲、剧本、分镜（模型服务暂未开通：deepseek）"));
        assertTrue(ex.getMessage().contains("定妆、图片"));
        assertTrue(ex.getMessage().contains("kling-v2-1"));
        assertTrue(ex.getMessage().contains("配音"));
        assertTrue(ex.getMessage().contains("minimax-speech-2.8-hd"));
        assertTrue(ex.getMessage().contains("请稍后重试"));
        assertTrue(!ex.getMessage().contains("provider_not_configured"));
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
                new ScriptedGateway(true));
        readiness.assertReady("IMAGE", null);
        readiness.assertReady("COMPOSE", null);
        verify(models, never()).resolve(org.mockito.ArgumentMatchers.eq("COMPOSE"), any());
    }

    @Test
    void queueDoesNotInsertWhenThePipelineIsBlocked() {
        AiTaskQueue queue = new AiTaskQueue();
        PipelineReadiness readiness = org.mockito.Mockito.mock(PipelineReadiness.class);
        ShortAiTaskMapper mapper = org.mockito.Mockito.mock(ShortAiTaskMapper.class);
        org.mockito.Mockito.doThrow(new BizException(CommonErrorCode.ABILITY_UNAVAILABLE,
                "大纲、剧本、分镜现在不能开始：模型服务暂未开通：deepseek，请稍后重试"))
                .when(readiness).assertReady("TEXT", null);
        ReflectionTestUtils.setField(queue, "pipelineReadiness", readiness);
        ReflectionTestUtils.setField(queue, "shortAiTaskMapper", mapper);
        BizException ex = assertThrows(BizException.class,
                () -> queue.enqueue("u", "w", null, "TEXT", "OUTLINE", "w", java.util.Map.of(), null, 0));
        assertEquals("4014", ex.getCode());
        verify(mapper, never()).insertSelective(any());
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

        private ScriptedGateway(boolean deepseekReady) {
            this.deepseekReady = deepseekReady;
        }

        @Override
        public Reply get(String url, String apiKey) {
            assertTrue(url.startsWith("https://dev.user.bgssai-tokenhub.cn/v1/models"));
            assertEquals("sk-test", apiKey);
            return new Reply(200, "{\"data\":["
                    + "{\"id\":\"deepseek-chat\",\"capabilities\":[\"chat\"]},"
                    + "{\"id\":\"kling-v3\",\"capabilities\":[\"image_generation\"]},"
                    + "{\"id\":\"qwen-tts\",\"capabilities\":[\"audio_speech\"]}"
                    + "]}");
        }

        @Override
        public Reply post(String url, String apiKey, String json) {
            if (!deepseekReady && json.contains("deepseek-chat")) {
                return new Reply(503, "{\"error\":{\"message\":\"Provider is not configured: deepseek\","
                        + "\"type\":\"service_unavailable_error\",\"code\":\"provider_not_configured\"}}");
            }
            return new Reply(400, "{\"error\":{\"message\":\"messages is required\",\"code\":\"invalid_request\"}}");
        }
    }
}
