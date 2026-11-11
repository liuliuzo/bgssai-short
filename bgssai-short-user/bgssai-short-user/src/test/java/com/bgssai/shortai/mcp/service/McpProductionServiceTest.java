package com.bgssai.shortai.mcp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.production.dto.ProductionDtos.*;
import com.bgssai.shortai.production.dto.FeatureDtos.*;
import com.bgssai.shortai.production.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class McpProductionServiceTest {
    @Mock WorkAccessService access;
    @Mock ScriptService scripts;
    @Mock ShotService shots;
    @Mock AiTaskService tasks;
    @Mock DubbingService dubbing;
    @Mock FilmService films;
    @Mock CharacterService characters;
    @Mock AiModelResolveService models;
    McpProductionService service;
    final ObjectMapper json = new ObjectMapper();

    @BeforeEach void setup() {
        service = new McpProductionService(access, scripts, shots, tasks, dubbing, films, characters, models);
    }
    JsonNode args(String value) throws Exception { return json.readTree(value); }
    ShortEpisode ownEpisode() {
        ShortEpisode ep = new ShortEpisode(); ep.setEpisodeId("e-1"); ep.setWorkId("w-1"); ep.setUserId("u-1");
        when(access.requireEpisode("e-1")).thenReturn(ep);
        return ep;
    }
    ShortShot ownShot(String id, String episode) {
        ShortShot shot = new ShortShot(); shot.setShotId(id); shot.setEpisodeId(episode);
        when(shots.requireShot(id)).thenReturn(shot);
        return shot;
    }

    @Test void paidCallsRequireLiteralBooleanConfirmationBeforeAnyProductionAction() throws Exception {
        for (String flag : List.of("false", "\"true\"", "1", "null")) {
            JsonNode a = args("{\"episode_id\":\"e-1\",\"confirm_cost\":" + flag + "}");
            assertThrows(IllegalArgumentException.class, () -> service.execute("submit_render_task", a));
            assertThrows(IllegalArgumentException.class, () -> service.execute("generate_reference_image", a));
        }
        verifyNoInteractions(tasks, films, dubbing, characters);
    }

    @Test void imageAndVideoDelegateToFormalQueueWithoutInventingPromptOrAutoNext() throws Exception {
        ownEpisode(); ownShot("sh-1", "e-1");
        when(tasks.submit(any())).thenReturn(Responses.ok(Map.of("task_id_list", List.of("t-1"), "estimated_cost_cent", 10)));
        for (String type : List.of("IMAGE", "VIDEO")) {
            Object out = service.execute("submit_render_task", args("{\"episode_id\":\"e-1\",\"shot_id\":\"sh-1\",\"task_type\":\"" + type + "\",\"model_code\":\"bgssai-image\",\"confirm_cost\":true}"));
            assertEquals("t-1", json.valueToTree(out).path("task_id_list").get(0).asText());
        }
        ArgumentCaptor<AiTaskReq> req = ArgumentCaptor.forClass(AiTaskReq.class);
        verify(tasks, times(2)).submit(req.capture());
        assertEquals("VIDEO", req.getValue().getTaskType());
        assertEquals(List.of("sh-1"), req.getValue().getShotIds());
        assertEquals("e-1", req.getValue().getEpisodeId());
        assertEquals(0, req.getValue().getAutoNextFlag());
        verifyNoInteractions(dubbing, films);
    }

    @Test void foreignEpisodeAndCrossEpisodeShotCannotEnqueue() throws Exception {
        when(access.requireEpisode("other")).thenThrow(new ForbiddenException("episode not found"));
        assertThrows(ForbiddenException.class, () -> service.execute("submit_render_task", args("{\"episode_id\":\"other\",\"confirm_cost\":true}")));
        ownEpisode(); ownShot("sh-other", "e-other");
        assertThrows(ForbiddenException.class, () -> service.execute("submit_render_task", args("{\"episode_id\":\"e-1\",\"shot_id\":\"sh-other\",\"task_type\":\"VOICE\",\"confirm_cost\":true}")));
        verifyNoInteractions(tasks, films, dubbing);
    }

    @Test void voiceUsesDubbingServiceToBuildDialogueVoiceAndSubtitleFlow() throws Exception {
        ownEpisode(); ownShot("sh-1", "e-1");
        when(dubbing.submit(any())).thenReturn(Responses.ok(Map.of("task_id_list", List.of("voice-1"))));
        service.execute("submit_render_task", args("{\"episode_id\":\"e-1\",\"shot_id\":\"sh-1\",\"task_type\":\"VOICE\",\"model_code\":\"bgssai-tts\",\"confirm_cost\":true}"));
        ArgumentCaptor<DubbingSubmitReq> req = ArgumentCaptor.forClass(DubbingSubmitReq.class);
        verify(dubbing).submit(req.capture());
        assertEquals(List.of("sh-1"), req.getValue().getShotIdList());
        assertEquals("bgssai-tts", req.getValue().getModelCode());
        verifyNoInteractions(tasks, films);
    }

    @Test void musicUsesOfficialBgmServiceAndDoesNotDiscardPrompt() throws Exception {
        ownEpisode(); when(films.generateBgm(any())).thenReturn(Responses.ok(Map.of("task_id", "music-1")));
        service.execute("submit_render_task", args("{\"episode_id\":\"e-1\",\"task_type\":\"MUSIC\",\"prompt\":\"warm piano, rain, 60 seconds\",\"is_instrumental\":true,\"confirm_cost\":true}"));
        ArgumentCaptor<BgmGenerateReq> req = ArgumentCaptor.forClass(BgmGenerateReq.class);
        verify(films).generateBgm(req.capture());
        assertEquals("w-1", req.getValue().getWorkId());
        assertEquals("warm piano, rain, 60 seconds", req.getValue().getPrompt());
        assertEquals(1, req.getValue().getIsInstrumental());
        verifyNoInteractions(tasks);
    }

    @Test void composeUsesPrecheckAndQuotaAwareFormalService() throws Exception {
        ownEpisode(); when(films.compose(any())).thenThrow(new IllegalArgumentException("precheck not ready"));
        Exception err = assertThrows(IllegalArgumentException.class, () -> service.execute("submit_render_task", args("{\"episode_id\":\"e-1\",\"confirm_cost\":true}")));
        assertEquals("precheck not ready", err.getMessage());
        verify(films).compose(any()); verifyNoInteractions(tasks, dubbing);
    }

    @Test void unsupportedPromptCannotBeSilentlyIgnored() throws Exception {
        ownEpisode();
        assertThrows(IllegalArgumentException.class, () -> service.execute("submit_render_task", args("{\"episode_id\":\"e-1\",\"task_type\":\"IMAGE\",\"prompt\":\"new story\",\"confirm_cost\":true}")));
        verifyNoInteractions(tasks, films);
    }

    @Test void readyAndSplitKeepExplicitForceAndReplacementConfirmation() throws Exception {
        when(scripts.ready(any())).thenReturn(Responses.ok("ok"));
        service.execute("ready_episode_script", args("{\"episode_id\":\"e-1\"}"));
        ArgumentCaptor<ScriptGenerateReq> req = ArgumentCaptor.forClass(ScriptGenerateReq.class);
        verify(scripts).ready(req.capture()); assertEquals(0, req.getValue().getForceReadyFlag());
        assertThrows(IllegalArgumentException.class, () -> service.execute("split_storyboard", args("{\"episode_id\":\"e-1\"}")));
        verifyNoInteractions(shots);
        when(shots.split(any())).thenReturn(Responses.ok(Map.of("shot_count", 12)));
        service.execute("split_storyboard", args("{\"episode_id\":\"e-1\",\"confirm_replace\":true}"));
        verify(shots).split(any());
    }

    @Test void storyboardValidatesEveryShotBeforeWritingAndPreservesStructuredFields() throws Exception {
        ownEpisode(); ownShot("sh-1", "e-1"); ownShot("sh-2", "other");
        assertThrows(ForbiddenException.class, () -> service.execute("save_storyboard", args("{\"episode_id\":\"e-1\",\"shots\":[{\"shot_id\":\"sh-1\"},{\"shot_id\":\"sh-2\"}]}")));
        verify(shots, never()).update(any());
        service.execute("save_storyboard", args("{\"episode_id\":\"e-1\",\"shots\":[{\"shot_id\":\"sh-1\",\"shot_desc\":\"老人递来旧车票\",\"dialogue_text\":\"你救别人，爸等你回家。\",\"shot_duration_sec\":5,\"extra_prompt\":\"no readable text\"}]}"));
        ArgumentCaptor<ShotReq> req = ArgumentCaptor.forClass(ShotReq.class); verify(shots).update(req.capture());
        assertEquals(5, req.getValue().getShotDurationSec());
        assertEquals("no readable text", req.getValue().getExtraPrompt());
        assertEquals("你救别人，爸等你回家。", req.getValue().getDialogueText());
    }

    @Test void invalidDurationRejectedBeforeAnySave() throws Exception {
        ownEpisode(); ownShot("sh-1", "e-1");
        assertThrows(IllegalArgumentException.class, () -> service.execute("save_storyboard", args("{\"episode_id\":\"e-1\",\"shots\":[{\"shot_id\":\"sh-1\",\"shot_duration_sec\":-1}]}")));
        verify(shots, never()).update(any());
    }

    @Test void taskQueryAndModelCatalogReusePublicServices() throws Exception {
        when(tasks.detail("t-1")).thenReturn(Responses.ok(Map.of("task_status", "FAILED", "fail_reason", "model unavailable")));
        assertEquals("FAILED", json.valueToTree(service.execute("get_render_task", args("{\"task_id\":\"t-1\"}"))).path("task_status").asText());
        when(models.listEnabled("VIDEO")).thenReturn(Responses.ok(Map.of("model_list", List.of(Map.of("model_code", "bgssai-video")))));
        JsonNode catalog = json.valueToTree(service.execute("list_models", args("{\"ability_type\":\"VIDEO\"}")));
        assertEquals("bgssai-video", catalog.path("model_list").get(0).path("model_code").asText());
        verifyNoInteractions(films);
    }

    @Test void filmReturnsOnlyFormalResultIncludingUnavailableDownload() throws Exception {
        ownEpisode(); when(films.detail("e-1")).thenReturn(Responses.ok(Map.of("expired_flag", 1)));
        when(films.precheck("e-1")).thenReturn(Responses.ok(Map.of("ready", false)));
        when(dubbing.list("e-1")).thenReturn(Responses.ok(Map.of("dubbing_list", List.of())));
        when(dubbing.subtitleDetail("e-1")).thenReturn(Responses.ok(Map.of("subtitle_list", List.of())));
        JsonNode out = json.valueToTree(service.execute("get_episode_film", args("{\"episode_id\":\"e-1\"}")));
        assertEquals(1, out.path("film").path("expired_flag").asInt());
        assertFalse(out.path("film").has("download_url"));
        assertFalse(out.path("precheck").path("ready").asBoolean());
    }

    @Test void settingsInferWorkFromOwnedEpisodeAndRetainBgmAssetValidation() throws Exception {
        ownEpisode(); when(films.settingsSave(any())).thenReturn(Responses.ok(Map.of()));
        service.execute("save_film_settings", args("{\"episode_id\":\"e-1\",\"bgm_asset_id\":\"audio-1\",\"burn_subtitle\":true,\"aspect_ratio\":\"9:16\"}"));
        ArgumentCaptor<FilmSettingsSaveReq> req = ArgumentCaptor.forClass(FilmSettingsSaveReq.class);
        verify(films).settingsSave(req.capture());
        assertEquals("w-1", req.getValue().getWorkId()); assertEquals("e-1", req.getValue().getEpisodeId());
        assertEquals("audio-1", req.getValue().getComposeSetting().get("bgm_asset_id"));
        assertEquals(1, req.getValue().getBurnSubtitleFlag());
    }

    @Test void referenceCannotLockAnotherCardsAssetOrGenerateForeignCard() throws Exception {
        when(characters.consistencyPanel("w-1")).thenReturn(Responses.ok(Map.of("character_list", List.of(
                Map.of("character_id", "c-1", "candidate_list", List.of(Map.of("asset_id", "img-own")))))));
        assertThrows(ForbiddenException.class, () -> service.execute("lock_reference_card", args("{\"work_id\":\"w-1\",\"card_type\":\"CHARACTER\",\"card_id\":\"c-1\",\"asset_id\":\"img-other\"}")));
        assertThrows(ForbiddenException.class, () -> service.execute("generate_reference_image", args("{\"work_id\":\"w-1\",\"card_type\":\"CHARACTER\",\"card_id\":\"c-other\",\"confirm_cost\":true}")));
        verify(characters, never()).lockCharacter(any()); verify(characters, never()).portrait(any());
        when(characters.lockCharacter(any())).thenReturn(Responses.ok("ok"));
        service.execute("lock_reference_card", args("{\"work_id\":\"w-1\",\"card_type\":\"CHARACTER\",\"card_id\":\"c-1\",\"asset_id\":\"img-own\"}"));
        verify(characters).lockCharacter(any());
    }
    @Test void characterPortraitUsesSavedAppearanceThroughFormalCharacterService() throws Exception {
        when(characters.consistencyPanel("w-1")).thenReturn(Responses.ok(Map.of("character_list", List.of(Map.of("character_id", "c-1")))));
        when(characters.updateCharacter(any())).thenReturn(Responses.ok("ok"));
        when(characters.portrait(any())).thenReturn(Responses.ok(Map.of("task_id", "portrait-1")));
        service.execute("generate_reference_image", args("{\"work_id\":\"w-1\",\"card_type\":\"CHARACTER\",\"card_id\":\"c-1\",\"appearance_prompt\":\"65岁灰发老人，深灰旧外套\",\"confirm_cost\":true}"));
        ArgumentCaptor<CharacterReq> capture = ArgumentCaptor.forClass(CharacterReq.class);
        var ordered = inOrder(characters);
        ordered.verify(characters).consistencyPanel("w-1");
        ordered.verify(characters).updateCharacter(capture.capture());
        ordered.verify(characters).portrait(any());
        assertEquals("65岁灰发老人，深灰旧外套", capture.getValue().getAppearancePrompt());
        assertEquals(2, capture.getValue().getCandidateCount());
    }

    @Test void characterVoiceMustComeFromFormalCatalogAndKeepOwnershipCheck() throws Exception {
        when(dubbing.voices()).thenReturn(Responses.ok(Map.of("voice_list", List.of(Map.of("voice_id", "v-female-warm-01")))));
        JsonNode catalog = json.valueToTree(service.execute("list_voices", args("{}")));
        assertEquals("v-female-warm-01", catalog.path("voice_list").get(0).path("voice_id").asText());
        assertThrows(IllegalArgumentException.class, () -> service.execute("set_character_voice", args("{\"character_id\":\"c-1\",\"voice_id\":\"invented\"}")));
        verify(dubbing, never()).applyVoiceToCharacter(any());
        when(dubbing.applyVoiceToCharacter(any())).thenThrow(new ForbiddenException("character not found"));
        assertThrows(ForbiddenException.class, () -> service.execute("set_character_voice", args("{\"character_id\":\"other\",\"voice_id\":\"v-female-warm-01\"}")));
        doReturn(Responses.ok("ok")).when(dubbing).applyVoiceToCharacter(any());
        service.execute("set_character_voice", args("{\"character_id\":\"c-1\",\"voice_id\":\"v-female-warm-01\"}"));
        ArgumentCaptor<ApplyVoiceReq> capture = ArgumentCaptor.forClass(ApplyVoiceReq.class);
        verify(dubbing, times(2)).applyVoiceToCharacter(capture.capture());
        assertEquals("c-1", capture.getValue().getCharacterId());
        assertEquals("v-female-warm-01", capture.getValue().getVoiceId());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"SCENE", "PROP"})
    void sceneAndPropAppearanceUsesOwnedFormalUpdateBeforeGeneration(String type) throws Exception {
        String collection = "SCENE".equals(type) ? "scene_list" : "prop_list";
        when(characters.consistencyPanel("w-1")).thenReturn(Responses.ok(Map.of(collection,
                List.of(Map.of("scene_card_id", "sc-1")))));
        when(characters.updateScene(any())).thenReturn(Responses.ok("ok"));
        when(characters.portraitScene(any())).thenReturn(Responses.ok(Map.of("task_id", "scene-1")));
        JsonNode request = json.valueToTree(Map.of("work_id", "w-1", "card_type", type,
                "card_id", "sc-1", "appearance_prompt", "rainy convenience store at night", "confirm_cost", true));
        service.execute("generate_reference_image", request);
        ArgumentCaptor<CharacterReq> req = ArgumentCaptor.forClass(CharacterReq.class);
        var ordered = inOrder(characters);
        ordered.verify(characters).consistencyPanel("w-1");
        ordered.verify(characters).updateScene(req.capture());
        ordered.verify(characters).portraitScene(any());
        assertEquals("sc-1", req.getValue().getSceneCardId());
        assertEquals("rainy convenience store at night", req.getValue().getAppearancePrompt());
        verify(characters, never()).updateCharacter(any());
        assertThrows(ForbiddenException.class, () -> service.execute("generate_reference_image",
                json.valueToTree(Map.of("work_id", "w-1", "card_type", type,
                "card_id", "foreign", "appearance_prompt", "other", "confirm_cost", true))));
        verify(characters, times(1)).updateScene(any());
    }

}
