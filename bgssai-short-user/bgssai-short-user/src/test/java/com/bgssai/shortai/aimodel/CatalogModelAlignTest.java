package com.bgssai.shortai.aimodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CatalogModelAlignTest {

    @Test
    void keepsConfiguredDefaultOnlyWhenTheCatalogListsIt() {
        List<TokenHubModelCatalog.HubModel> catalog = List.of(
                image("kling-v3", "快手可灵"),
                speech("qwen-tts", "dashscope"),
                chat("deepseek-chat"));
        assertEquals("kling-v3", CatalogModelAlign.choose("IMAGE", "kling-v3", catalog));
        assertEquals("deepseek-chat", CatalogModelAlign.choose("TEXT", "deepseek-chat", catalog));
        assertEquals("qwen-tts", CatalogModelAlign.choose("VOICE", "qwen-tts", catalog));
    }

    @Test
    void imageMovesFromUnlistedKlingIdToRegisteredKlingV3() {
        List<TokenHubModelCatalog.HubModel> catalog = List.of(
                image("kling-v3-omni", "快手可灵"),
                image("kling-v3", "快手可灵"),
                speech("qwen-tts", "dashscope"));
        assertEquals("kling-v3", CatalogModelAlign.choose("IMAGE", "kling-v2-1", catalog));
    }

    @Test
    void imageMismatchStaysOnTheConfiguredIdWhenKlingIsAbsent() {
        List<TokenHubModelCatalog.HubModel> catalog = List.of(speech("qwen-tts", "dashscope"));
        assertEquals("kling-v2-1", CatalogModelAlign.choose("IMAGE", "kling-v2-1", catalog));
        assertEquals("image-01", CatalogModelAlign.choose("IMAGE", "image-01", catalog));
    }

    @Test
    void voiceUsesRegisteredMinimaxSpeechAndIgnoresOtherVendors() {
        List<TokenHubModelCatalog.HubModel> catalog = List.of(
                speech("qwen-tts", "dashscope"),
                speech("cosyvoice-v3", "dashscope"),
                speech("speech-02-turbo", "MiniMax"),
                speech("speech-02-hd", "海螺"));
        assertEquals("speech-02-hd", CatalogModelAlign.choose("VOICE", "minimax-speech-2.8-hd", catalog));
    }

    @Test
    void voiceMismatchNamesTheConfiguredModelUntilMinimaxSpeechExists() {
        List<TokenHubModelCatalog.HubModel> catalog = List.of(
                speech("qwen-tts", "dashscope"),
                new TokenHubModelCatalog.HubModel("minimax-video-h3", List.of("video_generation"), Map.of(), "MiniMax"));
        assertEquals("minimax-speech-2.8-hd",
                CatalogModelAlign.choose("VOICE", "minimax-speech-2.8-hd", catalog));
    }

    @Test
    void unlistedVideoMovesToListedCompanyModelAndListedMinimaxStays() {
        List<TokenHubModelCatalog.HubModel> both = List.of(
                video("minimax-video-h3", "MiniMax"),
                video("cogvideox", "BGSSAI"));
        assertEquals("minimax-video-h3", CatalogModelAlign.choose("VIDEO", "minimax-video-h3", both));
        List<TokenHubModelCatalog.HubModel> company = List.of(video("cogvideox", "BGSSAI"));
        assertEquals("cogvideox", CatalogModelAlign.choose("VIDEO", "minimax-video-h3", company));
    }

    @Test
    void videoCompanyFallbackDoesNotMistakeZhipuCogvideoxForCompany() {
        List<TokenHubModelCatalog.HubModel> both = List.of(
                video("cogvideox", "zhipu"), video("bgssai-video", "bgssai-company"));
        assertEquals("bgssai-video", CatalogModelAlign.choose("VIDEO", "missing-default", both));
        assertEquals("cogvideox", CatalogModelAlign.choose("VIDEO", "cogvideox", both));
        assertEquals("missing-default", CatalogModelAlign.choose("VIDEO", "missing-default",
                List.of(video("cogvideox", "zhipu"))));
    }

    @Test
    void unlistedImageMovesToSdTurboOnlyWhenKlingIsAbsent() {
        assertEquals("sd-turbo", CatalogModelAlign.choose("IMAGE", "kling-v2-1",
                List.of(image("sd-turbo", "BGSSAI"))));
        assertEquals("kling-v3", CatalogModelAlign.choose("IMAGE", "kling-v2-1",
                List.of(image("kling-v3", "快手可灵"), image("sd-turbo", "BGSSAI"))));
    }

    @Test
    void unlistedVoiceMovesToPiperAndDoesNotUseQwenTts() {
        assertEquals("piper", CatalogModelAlign.choose("VOICE", "minimax-speech-2.8-hd",
                List.of(speech("piper", "BGSSAI"))));
        assertEquals("minimax-speech-2.8-hd", CatalogModelAlign.choose("VOICE", "minimax-speech-2.8-hd",
                List.of(speech("qwen-tts", "dashscope"))));
    }

    @Test
    void explicitFamilyIsNotRequiredForAnUnrelatedDefault() {
        List<TokenHubModelCatalog.HubModel> catalog = List.of(chat("kimi-k3"));
        assertEquals("deepseek-chat", CatalogModelAlign.choose("TEXT", "deepseek-chat", catalog));
    }

    private static TokenHubModelCatalog.HubModel image(String id, String owner) {
        return new TokenHubModelCatalog.HubModel(id, List.of("image_generation"), Map.of(), owner);
    }

    private static TokenHubModelCatalog.HubModel speech(String id, String owner) {
        return new TokenHubModelCatalog.HubModel(id, List.of("audio_speech"), Map.of(), owner);
    }

    private static TokenHubModelCatalog.HubModel video(String id, String owner) {
        return new TokenHubModelCatalog.HubModel(id, List.of("video_generation"), Map.of(), owner);
    }

    private static TokenHubModelCatalog.HubModel chat(String id) {
        return new TokenHubModelCatalog.HubModel(id, List.of("chat"), Map.of(), "vendor");
    }
}
