package com.bgssai.shortai.production.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aiprovider.AiProviderRequest;
import com.bgssai.shortai.aiprovider.AiProviderResult;
import com.bgssai.shortai.aiprovider.FfmpegComposeProvider;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.CommercialOutcomeGuard;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortDubbingMapper;
import com.bgssai.shortai.mapper.ShortFilmMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortSubtitleMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortFilm;
import com.bgssai.shortai.model.ShortSceneCard;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.service.AssetService;
import com.bgssai.shortai.production.service.FilmService;
import com.bgssai.shortai.production.service.ShortAiTaskWorker;
import com.bgssai.shortai.production.service.WorkAccessService;
import com.bgssai.shortai.production.support.ShotConsistency;
import com.bgssai.shortai.production.support.SubtitleSrtBuilder;
import com.bgssai.shortai.storage.ObjectStorage;

/**
 * SHORT-01 P0 一集生产验收骨架：契约锁定失败口径与证据字段；
 * 真实模型 / OBS / Media E2E 见 docs/review，本类不标 PASS。
 */
class Short01EpisodeProductionAcceptanceTest {

    @TempDir
    Path temp;

    @Test
    void subtitleAndTimelineAlignToThreeShotSequence() {
        ShortShot s1 = shot(1, 3, "镜1对白");
        ShortShot s2 = shot(2, 2, "镜2对白");
        ShortShot s3 = shot(3, 4, "镜3对白");
        List<Map<String, Object>> cues = SubtitleSrtBuilder.cuesFromShots(List.of(s1, s2, s3));
        assertEquals(3, cues.size());
        assertEquals(0, cues.get(0).get("start_ms"));
        assertEquals(3000, cues.get(0).get("end_ms"));
        assertEquals(3000, cues.get(1).get("start_ms"));
        assertEquals(5000, cues.get(1).get("end_ms"));
        assertEquals(5000, cues.get(2).get("start_ms"));
        assertEquals(9000, cues.get(2).get("end_ms"));
        String srt = SubtitleSrtBuilder.fromCues(cues);
        assertTrue(srt.contains("00:00:00,000 --> 00:00:03,000"));
        assertTrue(srt.contains("00:00:05,000 --> 00:00:09,000"));
    }

    @Test
    void characterAndSceneConsistencyInjectedForShots() {
        ShortCharacter c = new ShortCharacter();
        c.setCharacterId("c-1");
        c.setCharacterName("林晚");
        c.setAppearancePrompt("黑长直，米色风衣");
        ShortSceneCard sc = new ShortSceneCard();
        sc.setSceneCardId("sc-1");
        sc.setCardName("天台");
        sc.setAppearancePrompt("夜色霓虹");
        String frag = ShotConsistency.appearanceFragment(List.of(c), sc);
        String prompt = ShotConsistency.withConsistency("中景对峙", frag);
        assertTrue(prompt.contains("林晚"));
        assertTrue(prompt.contains("天台"));
        assertTrue(prompt.contains("一致性要求"));
        assertEquals("c-1", ShotConsistency.characterIds(
                ShotConsistency.bindCharacterRefs(List.of(c), "林晚推开门")).get(0));
        assertEquals("sc-1", ShotConsistency.bindSceneCardId(List.of(sc), "天台夜景"));
    }

    @Test
    void missingClipFailsWithNamedShots() {
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ShortShotMapper shotMapper = mock(ShortShotMapper.class);
        ObjectStorage storage = mock(ObjectStorage.class);
        ReflectionTestUtils.setField(worker, "objectStorage", storage);
        ShortShot missing = shot(2, 3, "缺片");
        missing.setVideoAssetId(null);
        ShortShot ok = shot(1, 3, "有片");
        ok.setVideoAssetId("a-v1");
        ShortAssetMapper assetMapper = mock(ShortAssetMapper.class);
        ReflectionTestUtils.setField(worker, "shortAssetMapper", assetMapper);
        ShortAsset asset = new ShortAsset();
        asset.setAssetId("a-v1");
        asset.setStorageKey("short/local/u/w/a-v1.mp4");
        when(assetMapper.selectOneByExample(any())).thenReturn(asset);
        when(storage.get("short/local/u/w/a-v1.mp4")).thenReturn(new byte[] {1, 2, 3});

        BizException ex = assertThrows(BizException.class,
                () -> ReflectionTestUtils.invokeMethod(worker, "loadClips", List.of(ok, missing)));
        assertEquals(CommonErrorCode.RULE_REJECT.code(), ex.getCode());
        assertTrue(ex.getMessage().contains("镜2"));
    }

    @Test
    void emptyClipsRejectedByFfmpegProvider() {
        FfmpegComposeProvider provider = new FfmpegComposeProvider();
        AiProviderRequest req = new AiProviderRequest();
        req.setAbilityType("COMPOSE");
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode(FfmpegComposeProvider.CODE);
        model.setTimeoutSec(60);
        req.setModel(model);
        req.getParams().put(FfmpegComposeProvider.PARAM_CLIPS, List.of());
        BizException ex = assertThrows(BizException.class, () -> provider.execute(req));
        assertEquals(CommonErrorCode.RULE_REJECT.code(), ex.getCode());
        assertTrue(ex.getMessage().contains("没有可合成"));
    }

    @Test
    void placeholderUrlNeverCommercialPass() {
        assertTrue(CommercialOutcomeGuard.isFakeUrl("stub://film/demo.mp4"));
        assertTrue(CommercialOutcomeGuard.isFakeUrl("https://example.com/a.mp4"));
        assertNull(CommercialOutcomeGuard.safeCommercialUrl("placeholder://x"));
        assertEquals("https://obs.my-cdn.com/films/a.mp4",
                CommercialOutcomeGuard.safeCommercialUrl("https://obs.my-cdn.com/films/a.mp4"));
        assertThrows(BizException.class,
                () -> CommercialOutcomeGuard.rejectFakeUrl("stub://x", "film video_url"));
    }

    @Test
    void filmDetailExposesTaskModelUsageStorageEvidence() {
        FilmService service = new FilmService();
        WorkAccessService access = mock(WorkAccessService.class);
        AssetService assets = mock(AssetService.class);
        ShortFilmMapper filmMapper = mock(ShortFilmMapper.class);
        ShortAssetMapper assetMapper = mock(ShortAssetMapper.class);
        ShortAiTaskMapper taskMapper = mock(ShortAiTaskMapper.class);
        ShortDubbingMapper dubbingMapper = mock(ShortDubbingMapper.class);
        ShortSubtitleMapper subtitleMapper = mock(ShortSubtitleMapper.class);
        ShortShotMapper shotMapper = mock(ShortShotMapper.class);
        ShortWorkMapper workMapper = mock(ShortWorkMapper.class);
        ReflectionTestUtils.setField(service, "workAccessService", access);
        ReflectionTestUtils.setField(service, "assetService", assets);
        ReflectionTestUtils.setField(service, "shortFilmMapper", filmMapper);
        ReflectionTestUtils.setField(service, "shortAssetMapper", assetMapper);
        ReflectionTestUtils.setField(service, "shortAiTaskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "shortDubbingMapper", dubbingMapper);
        ReflectionTestUtils.setField(service, "shortSubtitleMapper", subtitleMapper);
        ReflectionTestUtils.setField(service, "shortShotMapper", shotMapper);
        ReflectionTestUtils.setField(service, "shortWorkMapper", workMapper);

        ShortEpisode ep = new ShortEpisode();
        ep.setEpisodeId("ep-1");
        ep.setWorkId("w-1");
        ep.setComposeStatus("READY");
        ShortWork work = new ShortWork();
        work.setWorkId("w-1");
        work.setComposeSettingJson("{}");
        when(access.requireEpisode("ep-1")).thenReturn(ep);
        when(access.requireWork("w-1")).thenReturn(work);

        ShortFilm film = new ShortFilm();
        film.setFilmId("fl-1");
        film.setFilmAssetId("a-film");
        film.setFilmDurationSec(9);
        film.setComposeStatus("READY");
        film.setComposeTaskId("t-compose-1");
        film.setMediaPlayUrl("stub://should-not-pass");
        when(filmMapper.selectOneByExample(any())).thenReturn(film);

        ShortAsset asset = new ShortAsset();
        asset.setAssetId("a-film");
        asset.setSourceTaskId("t-compose-1");
        asset.setStorageKey("short/dev/u1/w1/a-film.mp4");
        asset.setFileSizeByte(12_345);
        asset.setMimeType("video/mp4");
        asset.setDurationSec(9);
        when(assetMapper.selectOneByExample(any())).thenReturn(asset);

        ShortAiTask task = new ShortAiTask();
        task.setTaskId("t-compose-1");
        task.setModelCode("ffmpeg-local");
        task.setCostCent(0);
        task.setTaskStatus("SUCCEEDED");
        task.setTaskType("COMPOSE");
        when(taskMapper.selectOneByExample(any())).thenReturn(task);
        when(assets.resolveUrl("a-film")).thenReturn("https://obs.my-cdn.com/films/a-film.mp4");

        ApiResponse resp = service.detail("ep-1");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();
        assertEquals("t-compose-1", result.get("compose_task_id"));
        assertEquals("https://obs.my-cdn.com/films/a-film.mp4", result.get("download_url"));
        assertNull(result.get("media_play_url"), "stub media play_url must not surface");
        @SuppressWarnings("unchecked")
        Map<String, Object> evidence = (Map<String, Object>) result.get("production_evidence");
        assertNotNull(evidence);
        assertEquals("ffmpeg-local", evidence.get("model_code"));
        assertEquals(0, evidence.get("cost_cent"));
        assertEquals("short/dev/u1/w1/a-film.mp4", evidence.get("storage_key"));
        assertEquals(12_345, evidence.get("file_size_byte"));
        assertTrue(Boolean.TRUE.equals(result.get("commercial_ready")));
    }

    @Test
    void precheckReportsShotCountForAcceptanceFloor() {
        FilmService service = new FilmService();
        WorkAccessService access = mock(WorkAccessService.class);
        ShortShotMapper shotMapper = mock(ShortShotMapper.class);
        ShortDubbingMapper dubbingMapper = mock(ShortDubbingMapper.class);
        ReflectionTestUtils.setField(service, "workAccessService", access);
        ReflectionTestUtils.setField(service, "shortShotMapper", shotMapper);
        ReflectionTestUtils.setField(service, "shortDubbingMapper", dubbingMapper);
        ShortEpisode ep = new ShortEpisode();
        ep.setEpisodeId("ep-1");
        when(access.requireEpisode("ep-1")).thenReturn(ep);
        ShortShot a = shot(1, 3, "a");
        a.setShotStatus("VIDEO_READY");
        a.setVideoAssetId("v1");
        ShortShot b = shot(2, 3, "b");
        b.setShotStatus("VIDEO_READY");
        b.setVideoAssetId("v2");
        when(shotMapper.selectByExample(any())).thenReturn(List.of(a, b));
        when(dubbingMapper.selectByExample(any())).thenReturn(List.of());

        ApiResponse resp = service.precheck("ep-1");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();
        assertEquals(2, result.get("shot_count"));
        assertEquals(3, result.get("acceptance_min_shot_count"));
        assertFalse(Boolean.TRUE.equals(result.get("acceptance_shot_count_ok")));
        assertTrue(Boolean.TRUE.equals(result.get("ready")), "shot floor is advisory, not a compose blocker");
    }

    @Test
    void threeClipComposeWithSubtitleAndDubbingProducesPlayableMp4() throws Exception {
        assumeTrue(ffmpegAvailable(), "ffmpeg missing on this host — contract covered; E2E NOT_VERIFIED");
        Path c1 = temp.resolve("c1.mp4");
        Path c2 = temp.resolve("c2.mp4");
        Path c3 = temp.resolve("c3.mp4");
        Path vo = temp.resolve("vo.wav");
        runLocal("ffmpeg", "-y", "-f", "lavfi", "-i", "color=c=black:s=320x240:d=1",
                "-c:v", "libx264", "-pix_fmt", "yuv420p", c1.toString());
        runLocal("ffmpeg", "-y", "-f", "lavfi", "-i", "color=c=black:s=320x240:d=1",
                "-c:v", "libx264", "-pix_fmt", "yuv420p", c2.toString());
        runLocal("ffmpeg", "-y", "-f", "lavfi", "-i", "color=c=black:s=320x240:d=1",
                "-c:v", "libx264", "-pix_fmt", "yuv420p", c3.toString());
        runLocal("ffmpeg", "-y", "-f", "lavfi", "-i", "sine=frequency=440:duration=1",
                "-ar", "44100", "-ac", "2", vo.toString());

        FfmpegComposeProvider provider = new FfmpegComposeProvider();
        AiProviderRequest req = new AiProviderRequest();
        req.setAbilityType("COMPOSE");
        AiModelConfig model = new AiModelConfig();
        model.setProviderCode(FfmpegComposeProvider.CODE);
        model.setTimeoutSec(120);
        req.setModel(model);
        req.getParams().put(FfmpegComposeProvider.PARAM_CLIPS,
                List.of(Files.readAllBytes(c1), Files.readAllBytes(c2), Files.readAllBytes(c3)));
        req.getParams().put(FfmpegComposeProvider.PARAM_DUBBING_LIST,
                List.of(Files.readAllBytes(vo), new byte[0], Files.readAllBytes(vo)));
        req.getParams().put(FfmpegComposeProvider.PARAM_CLIP_DURATION_SEC_LIST, List.of(1, 1, 1));
        req.getParams().put(FfmpegComposeProvider.PARAM_SUBTITLE_SRT,
                SubtitleSrtBuilder.fromCues(SubtitleSrtBuilder.cuesFromShots(
                        List.of(shot(1, 1, "一"), shot(2, 1, "二"), shot(3, 1, "三")))));
        req.getParams().put("duration_sec", 3);

        AiProviderResult result = provider.execute(req);
        assertEquals("video/mp4", result.getMimeType());
        assertTrue(result.getBinaryContent().length > 1000);
        Path out = temp.resolve("film.mp4");
        Files.write(out, result.getBinaryContent());
        Process probe = new ProcessBuilder("ffprobe", "-v", "error", "-show_entries",
                "format=duration", "-of", "default=noprint_wrappers=1:nokey=1", out.toString())
                .redirectErrorStream(true).start();
        String duration = new String(probe.getInputStream().readAllBytes()).trim();
        assertTrue(probe.waitFor(30, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(0, probe.exitValue());
        double sec = Double.parseDouble(duration);
        assertTrue(sec >= 2.5 && sec <= 4.5, "duration should track 3×1s clips, got " + sec);
    }

    private static ShortShot shot(int no, int dur, String dialogue) {
        ShortShot s = new ShortShot();
        s.setShotId("s-" + no);
        s.setShotNo(no);
        s.setShotDurationSec(dur);
        s.setDialogueText(dialogue);
        s.setShotDesc(dialogue);
        return s;
    }

    private static boolean ffmpegAvailable() {
        try {
            Process p = new ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private static void runLocal(String... cmd) throws Exception {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        if (!p.waitFor(60, java.util.concurrent.TimeUnit.SECONDS) || p.exitValue() != 0) {
            throw new IllegalStateException("ffmpeg fixture failed: " + String.join(" ", cmd));
        }
    }
}
