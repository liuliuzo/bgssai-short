package com.bgssai.shortai.mcp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.common.exception.UnauthorizedException;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.production.service.*;
import com.bgssai.shortai.storage.ObjectStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Real ownership services with isolated persistence/storage doubles; no real media or writes. */
class McpAssetReviewServiceTest {
    final ObjectMapper json = new ObjectMapper();
    ShortAssetMapper assetMapper;
    ShortEpisodeMapper episodeMapper;
    ShortShotMapper shotMapper;
    ObjectStorage storage;
    AiTaskService tasks;
    FilmService films;
    DubbingService dubbing;
    McpProductionService service;

    @BeforeEach void setup() {
        UserContext.setUserId("owner");
        assetMapper = mock(ShortAssetMapper.class);
        episodeMapper = mock(ShortEpisodeMapper.class);
        shotMapper = mock(ShortShotMapper.class);
        storage = mock(ObjectStorage.class);
        tasks = mock(AiTaskService.class);
        films = mock(FilmService.class);
        dubbing = mock(DubbingService.class);
        AssetService assets = new AssetService();
        ReflectionTestUtils.setField(assets, "shortAssetMapper", assetMapper);
        ReflectionTestUtils.setField(assets, "objectStorage", storage);
        WorkAccessService access = new WorkAccessService();
        ReflectionTestUtils.setField(access, "shortEpisodeMapper", episodeMapper);
        ShotService shots = new ShotService();
        ReflectionTestUtils.setField(shots, "workAccessService", access);
        ReflectionTestUtils.setField(shots, "shortShotMapper", shotMapper);
        ReflectionTestUtils.setField(shots, "shortEpisodeMapper", episodeMapper);
        service = new McpProductionService(access, mock(ScriptService.class), shots, tasks,
                dubbing, films, mock(CharacterService.class), mock(AiModelResolveService.class), assets);
    }

    @AfterEach void clearIdentity() { UserContext.clear(); }

    JsonNode args(String text) throws Exception { return json.readTree(text); }
    JsonNode lockArgs() throws Exception {
        return args("{\"episode_id\":\"ep\",\"shot_id\":\"shot\",\"confirm_review\":true}");
    }
    ShortEpisode episode(String owner) {
        ShortEpisode row = new ShortEpisode(); row.setId(2L); row.setEpisodeId("ep"); row.setUserId(owner);
        when(episodeMapper.selectOneByExample(any())).thenReturn(row);
        return row;
    }
    ShortShot shot(String owner, String episode) {
        ShortShot row = new ShortShot(); row.setId(1L); row.setShotId("shot"); row.setUserId(owner);
        row.setEpisodeId(episode); row.setShotStatus("VIDEO_READY"); row.setVideoAssetId("video");
        when(shotMapper.selectOneByExample(any())).thenReturn(row);
        return row;
    }

    @Test void ownedAssetUsesFormalPresignedUrlAndUnchangedExpiry() throws Exception {
        ShortAsset asset = new ShortAsset(); asset.setAssetId("asset"); asset.setUserId("owner"); asset.setStorageKey("owner/video.mp4");
        when(assetMapper.selectOneByExample(any())).thenReturn(asset);
        Instant expiry = Instant.parse("2026-11-11T12:00:00Z");
        when(storage.presignGet("owner/video.mp4", 1800)).thenReturn(new ObjectStorage.PresignResult("https://media.example/video?signature=fixture", expiry));
        JsonNode out = json.valueToTree(service.execute("get_asset_download", args("{\"asset_id\":\"asset\"}")));
        assertEquals("https://media.example/video?signature=fixture", out.path("download_url").asText());
        assertEquals(expiry.toString(), out.path("expire_at").asText());
        assertEquals(2, out.size());
        verify(storage).presignGet("owner/video.mp4", 1800);
        verifyNoInteractions(tasks, films, dubbing, shotMapper);
    }

    @Test void foreignAndMissingAssetNeverIssueDownloadSignature() throws Exception {
        ShortAsset asset = new ShortAsset(); asset.setUserId("other"); asset.setStorageKey("foreign/video.mp4");
        when(assetMapper.selectOneByExample(any())).thenReturn(asset).thenReturn(null);
        for (int i = 0; i < 2; i++) {
            assertThrows(ForbiddenException.class, () -> service.execute("get_asset_download", args("{\"asset_id\":\"foreign\"}")));
        }
        verifyNoInteractions(storage, tasks, films, dubbing);
    }

    @Test void missingIdentityRejectsBothToolsBeforePersistence() throws Exception {
        UserContext.clear();
        assertThrows(UnauthorizedException.class, () -> service.execute("get_asset_download", args("{\"asset_id\":\"asset\"}")));
        assertThrows(UnauthorizedException.class, () -> service.execute("lock_shot", lockArgs()));
        verifyNoInteractions(assetMapper, episodeMapper, shotMapper, storage);
    }

    @Test void literalReviewConfirmationIsRequiredBeforeAnyShotAccess() throws Exception {
        for (String flag : List.of("false", "\"true\"", "1", "null", "{}", "[]")) {
            JsonNode input = args("{\"episode_id\":\"ep\",\"shot_id\":\"shot\",\"confirm_review\":" + flag + "}");
            assertThrows(IllegalArgumentException.class, () -> service.execute("lock_shot", input));
        }
        assertThrows(IllegalArgumentException.class, () -> service.execute("lock_shot", args("{\"episode_id\":\"ep\",\"shot_id\":\"shot\"}")));
        verifyNoInteractions(episodeMapper, shotMapper, tasks, films, dubbing);
    }

    @Test void foreignEpisodeCannotLockEvenAnOwnedShot() throws Exception {
        episode("other");
        assertThrows(ForbiddenException.class, () -> service.execute("lock_shot", lockArgs()));
        verifyNoInteractions(shotMapper);
    }

    @Test void foreignShotCannotBeLockedWithinOwnedEpisode() throws Exception {
        episode("owner"); shot("other", "ep");
        assertThrows(ForbiddenException.class, () -> service.execute("lock_shot", lockArgs()));
        verify(shotMapper, never()).updateByExampleSelective(any(), any());
    }

    @Test void ownedShotFromAnotherEpisodeCannotBeLocked() throws Exception {
        episode("owner"); shot("owner", "other-episode");
        assertThrows(ForbiddenException.class, () -> service.execute("lock_shot", lockArgs()));
        verify(shotMapper, never()).updateByExampleSelective(any(), any());
    }

    @Test void unreadyStateOrMissingActualVideoCannotBeLocked() throws Exception {
        episode("owner"); ShortShot row = shot("owner", "ep");
        for (String state : List.of("DRAFT", "IMAGE_READY", "GENERATING", "FAILED", "STALE", "")) {
            row.setShotStatus(state);
            assertThrows(IllegalArgumentException.class, () -> service.execute("lock_shot", lockArgs()));
        }
        row.setShotStatus(null);
        assertThrows(IllegalArgumentException.class, () -> service.execute("lock_shot", lockArgs()));
        row.setShotStatus("VIDEO_READY"); row.setVideoAssetId(null);
        assertThrows(IllegalArgumentException.class, () -> service.execute("lock_shot", lockArgs()));
        row.setVideoAssetId(" ");
        assertThrows(IllegalArgumentException.class, () -> service.execute("lock_shot", lockArgs()));
        verify(shotMapper, never()).updateByExampleSelective(any(), any());
        verifyNoInteractions(tasks, films, dubbing);
    }

    @Test void readyAndAlreadyLockedShotsUseFormalLockWithoutAssetOverwriteOrGeneration() throws Exception {
        episode("owner"); ShortShot row = shot("owner", "ep");
        for (String state : List.of("VIDEO_READY", "LOCKED")) {
            row.setShotStatus(state);
            assertEquals("ok", service.execute("lock_shot", lockArgs()));
        }
        ArgumentCaptor<ShortShot> update = ArgumentCaptor.forClass(ShortShot.class);
        verify(shotMapper, times(2)).updateByExampleSelective(update.capture(), any());
        for (ShortShot patch : update.getAllValues()) {
            assertEquals("LOCKED", patch.getShotStatus());
            assertNull(patch.getVideoAssetId());
            assertNull(patch.getFirstFrameAssetId());
        }
        verify(episodeMapper, times(2)).updateByExampleSelective(any(), any());
        verifyNoInteractions(tasks, films, dubbing, assetMapper, storage);
    }

    @Test void clientCannotOverrideAssetOrCallerFields() throws Exception {
        for (String field : List.of("asset_id", "video_asset_id", "first_frame_asset_id", "user_id", "locked")) {
            JsonNode input = args("{\"episode_id\":\"ep\",\"shot_id\":\"shot\",\"confirm_review\":true,\"" + field + "\":\"override\"}");
            assertThrows(IllegalArgumentException.class, () -> service.execute("lock_shot", input));
        }
        assertThrows(IllegalArgumentException.class, () -> service.execute("get_asset_download", args("{\"asset_id\":\"asset\",\"user_id\":\"other\"}")));
        verifyNoInteractions(assetMapper, episodeMapper, shotMapper, storage, tasks, films, dubbing);
    }
}
