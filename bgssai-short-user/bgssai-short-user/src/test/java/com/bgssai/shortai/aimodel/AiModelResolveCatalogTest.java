package com.bgssai.shortai.aimodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aiprovider.AiProviderRegistry;
import com.bgssai.shortai.mapper.AiModelConfigMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.secret.SysSecretReader;

class AiModelResolveCatalogTest {

    private final AiModelConfigMapper mapper = org.mockito.Mockito.mock(AiModelConfigMapper.class);
    private final AiProviderRegistry registry = org.mockito.Mockito.mock(AiProviderRegistry.class);
    private final TokenHubModelCatalog catalog = org.mockito.Mockito.mock(TokenHubModelCatalog.class);
    private final AiModelResolveService service = new AiModelResolveService();

    @BeforeEach
    void wire() {
        ReflectionTestUtils.setField(service, "aiModelConfigMapper", mapper);
        ReflectionTestUtils.setField(service, "aiProviderRegistry", registry);
        ReflectionTestUtils.setField(service, "tokenHubModelCatalog", catalog);
        when(registry.supports(any())).thenReturn(true);
    }

    @Test
    void blankImageCodeBecomesRegisteredKlingModel() {
        when(mapper.selectByExample(any())).thenReturn(List.of(row("IMAGE", "KLING", "kling-v2-1")));
        when(catalog.snapshot()).thenReturn(List.of(
                new TokenHubModelCatalog.HubModel("kling-v3", List.of("image_generation", "video_generation"),
                        Map.of(), "快手可灵")));
        AiModelConfig chosen = service.resolve("IMAGE", null);
        assertEquals("kling-v3", chosen.getModelCode());
        assertEquals("TENCENT_TOKENHUB", chosen.getProviderCode());
    }

    @Test
    void blankVoiceCodeFollowsProvisionedMinimaxSpeech() {
        when(mapper.selectByExample(any())).thenReturn(List.of(row("VOICE", "TENCENT_TOKENHUB", "minimax-speech-2.8-hd")));
        when(catalog.snapshot()).thenReturn(List.of(
                new TokenHubModelCatalog.HubModel("qwen-tts", List.of("audio_speech"), Map.of(), "dashscope"),
                new TokenHubModelCatalog.HubModel("speech-02-hd", List.of("audio_speech"), Map.of(), "MiniMax")));
        AiModelConfig chosen = service.resolve("VOICE", " ");
        assertEquals("speech-02-hd", chosen.getModelCode());
    }

    @Test
    void unlistedVoiceStaysConfiguredWhenMinimaxSpeechIsAbsent() {
        when(mapper.selectByExample(any())).thenReturn(List.of(row("VOICE", "TENCENT_TOKENHUB", "minimax-speech-2.8-hd")));
        when(catalog.snapshot()).thenReturn(List.of(
                new TokenHubModelCatalog.HubModel("qwen-tts", List.of("audio_speech"), Map.of(), "dashscope")));
        AiModelConfig chosen = service.resolve("VOICE", null);
        assertEquals("minimax-speech-2.8-hd", chosen.getModelCode());
    }

    @Test
    void emptyCatalogDoesNotRewriteTheConfiguredDefault() {
        when(mapper.selectByExample(any())).thenReturn(List.of(row("IMAGE", "KLING", "kling-v2-1")));
        when(catalog.snapshot()).thenReturn(List.of());
        AiModelConfig chosen = service.resolve("IMAGE", null);
        assertEquals("kling-v2-1", chosen.getModelCode());
        assertEquals("KLING", chosen.getProviderCode());
    }

    @Test
    void explicitCodeIsNotRewritten() {
        when(mapper.selectByExample(any())).thenReturn(List.of());
        AiModelConfig chosen = service.resolve("IMAGE", "kling-v2-1");
        assertEquals("kling-v2-1", chosen.getModelCode());
    }

    @Test
    void compatKeyAloneResolvesCompanyImage() {
        ReflectionTestUtils.setField(service, "secrets", new SysSecretReader(key ->
                SysSecretReader.AI_OPENAI_COMPAT_API_KEY.equals(key) ? "sk-company" : ""));
        when(mapper.selectByExample(any())).thenReturn(List.of());
        AiModelConfig chosen = service.resolve("IMAGE", null);
        assertEquals("sd-turbo", chosen.getModelCode());
        assertEquals("OPENAI_COMPAT", chosen.getProviderCode());
        assertTrue(chosen.getConfigJson().contains("chat.bgssai.com"));
    }

    @Test
    void explicitKlingStaysOnHubWhenOnlyCompatKeyIsSet() {
        ReflectionTestUtils.setField(service, "secrets", new SysSecretReader(key ->
                SysSecretReader.AI_OPENAI_COMPAT_API_KEY.equals(key) ? "sk-company" : ""));
        when(mapper.selectByExample(any())).thenReturn(List.of());
        AiModelConfig chosen = service.resolve("IMAGE", "kling-v2-1");
        assertEquals("kling-v2-1", chosen.getModelCode());
        assertEquals("TENCENT_TOKENHUB", chosen.getProviderCode());
    }

    @Test
    void hubKeyWinsWhenBothSecretsAreSet() {
        ReflectionTestUtils.setField(service, "secrets", new SysSecretReader(key -> "sk"));
        AiModelConfig hub = row("IMAGE", "TENCENT_TOKENHUB", "sd-turbo");
        AiModelConfig compat = row("IMAGE", "OPENAI_COMPAT", "sd-turbo");
        compat.setConfigJson("{\"base_url\":\"https://chat.bgssai.com/v1\"}");
        when(mapper.selectByExample(any())).thenReturn(List.of(hub, compat));
        AiModelConfig chosen = service.resolve("IMAGE", "sd-turbo");
        assertEquals("TENCENT_TOKENHUB", chosen.getProviderCode());
    }

    @Test
    void asrUsesCompanyWhisperWhenBothSecretsAreSet() {
        ReflectionTestUtils.setField(service, "secrets", new SysSecretReader(key -> "sk"));
        AiModelConfig whisper = row("ASR", "OPENAI_COMPAT", "whisper-1");
        whisper.setDefaultFlag(0);
        whisper.setConfigJson("{\"base_url\":\"https://chat.bgssai.com/v1\",\"transcription_path\":\"/audio/transcriptions\"}");
        when(mapper.selectByExample(any())).thenReturn(List.of(whisper));
        AiModelConfig chosen = service.resolve("ASR", null);
        assertEquals("OPENAI_COMPAT", chosen.getProviderCode());
        assertEquals("whisper-1", chosen.getModelCode());
    }

    @Test
    void asrFallsBackToImageToVideoCompanyKeyWhenCompatKeyIsBlank() {
        ReflectionTestUtils.setField(service, "secrets", new SysSecretReader(key -> {
            if (SysSecretReader.AI_IMAGE_TO_VIDEO_API_KEY.equals(key)) {
                return "sk-company";
            }
            if (SysSecretReader.AI_TOKENHUB_CN_API_KEY.equals(key)) {
                return "sk-hub";
            }
            return "";
        }));
        AiModelConfig whisper = row("ASR", "OPENAI_COMPAT", "whisper-1");
        whisper.setDefaultFlag(0);
        whisper.setConfigJson("{\"base_url\":\"https://chat.bgssai.com/v1\",\"transcription_path\":\"/audio/transcriptions\"}");
        when(mapper.selectByExample(any())).thenReturn(List.of(whisper));
        AiModelConfig chosen = service.resolve("ASR", null);
        assertEquals("OPENAI_COMPAT", chosen.getProviderCode());
        assertEquals("whisper-1", chosen.getModelCode());
    }

    @Test
    void explicitLocalFfmpegComposeResolvesLocalProviderEvenWhenHubKeyIsSet() {
        ReflectionTestUtils.setField(service, "secrets", new SysSecretReader(key -> "sk"));
        AiModelConfig ffmpeg = row("COMPOSE", "FFMPEG", "ffmpeg-concat");
        when(mapper.selectByExample(any())).thenReturn(List.of(ffmpeg));
        AiModelConfig chosen = service.resolve("COMPOSE", "ffmpeg-concat");
        assertEquals("FFMPEG", chosen.getProviderCode());
        assertEquals("ffmpeg-concat", chosen.getModelCode());

        when(mapper.selectByExample(any())).thenReturn(List.of());
        org.junit.jupiter.api.Assertions.assertThrows(
                com.bgssai.shortai.common.exception.BizException.class,
                () -> service.resolve("COMPOSE", "ffmpeg-concat"));
    }

    private static AiModelConfig row(String ability, String provider, String code) {
        AiModelConfig row = new AiModelConfig();
        row.setAbilityType(ability);
        row.setProviderCode(provider);
        row.setModelCode(code);
        row.setDefaultFlag(1);
        row.setEnabledFlag(1);
        row.setUnitPriceCent(80);
        row.setPriceUnit("PER_CALL");
        return row;
    }
}
