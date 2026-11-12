package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.aiprovider.AiProvider;
import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.aiprovider.AiProviderRequest;
import com.bgssai.shortai.aiprovider.AiProviderResult;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.model.AiModelConfig;

class DramaAssistantServiceTest {

    private AiModelResolveService models;
    private AiProviderRegistry registry;
    private DramaAssistantService service;

    @BeforeEach
    void setUp() {
        models = mock(AiModelResolveService.class);
        registry = mock(AiProviderRegistry.class);
        service = new DramaAssistantService(models, registry);
        UserContext.setUserId("user-1");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void askReturnsModelText() {
        AiModelConfig model = new AiModelConfig();
        model.setAbilityType("TEXT");
        model.setModelCode("bgssai-chat");
        AiProvider provider = mock(AiProvider.class);
        when(models.resolve("TEXT", null)).thenReturn(model);
        when(registry.resolve(model)).thenReturn(provider);
        when(provider.execute(any())).thenReturn(AiProviderResult.text("把对峙拆成过肩、推近和仰拍。"));

        var result = service.ask("director", "优化这场对峙", "镜1：两人站在门口", null);

        assertEquals("DIRECTOR", result.get("role"));
        assertEquals("把对峙拆成过肩、推近和仰拍。", result.get("reply"));
        assertEquals("bgssai-chat", result.get("model_code"));
        ArgumentCaptor<AiProviderRequest> sent = ArgumentCaptor.forClass(AiProviderRequest.class);
        verify(provider).execute(sent.capture());
        assertEquals("TEXT", sent.getValue().getAbilityType());
        assertFalse(sent.getValue().getPrompt().contains("你以为你赢了"));
        assertFalse(result.get("reply").toString().contains("你以为你赢了"));
    }

    @Test
    void missingModelDoesNotInventAReply() {
        when(models.resolve("TEXT", null))
                .thenThrow(new BizException(CommonErrorCode.ABILITY_UNAVAILABLE, "当前没有可用的对话模型"));

        BizException ex = assertThrows(BizException.class, () -> service.ask("SCRIPT", "写一句反转", "", ""));

        assertEquals(CommonErrorCode.ABILITY_UNAVAILABLE.code(), ex.getCode());
        assertFalse(ex.getMessage().contains("你以为你赢了"));
    }

    @Test
    void blankModelContentIsAnError() {
        AiModelConfig model = new AiModelConfig();
        model.setModelCode("bgssai-chat");
        AiProvider provider = mock(AiProvider.class);
        when(models.resolve("TEXT", null)).thenReturn(model);
        when(registry.resolve(model)).thenReturn(provider);
        when(provider.execute(any())).thenReturn(AiProviderResult.text("  "));

        BizException ex = assertThrows(BizException.class, () -> service.ask("VISUAL", "加电影光", null, null));

        assertEquals(CommonErrorCode.UPSTREAM_ERROR.code(), ex.getCode());
    }
}
