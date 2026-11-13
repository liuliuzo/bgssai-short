package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.aiprovider.*;
import com.bgssai.shortai.mapper.*;
import com.bgssai.shortai.model.*;
import com.bgssai.shortai.storage.ObjectStorage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class WorkerVideoRecoveryTest {
    @BeforeAll static void requireRealMediaTools() throws Exception {
        for (String tool : List.of("ffmpeg", "ffprobe")) {
            Process p = new ProcessBuilder(tool, "-version").redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            assertTrue(p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS) && p.exitValue() == 0,
                    tool + " is required for the real-video recovery regression");
        }
    }

    @Test void recoverySelectsOnlyAfterCasAndKeepsActualVideoDurationAndOriginalRequest() throws Exception {
        Fixture f = new Fixture(true);
        f.worker.tick();
        assertEquals(2, f.video().getDurationSec()); // fixture 1.25 s, requested shot 5 s
        assertEquals(0, f.video().getSelectedFlag()); // insert was a candidate, never preselected
        var ordered = inOrder(f.assets, f.shots);
        ordered.verify(f.assets).insertSelective(argThat(a -> "VIDEO".equals(a.getAssetType())));
        ordered.verify(f.shots).attachRecoveredVideoIfUnchanged(any(), any(), eq(9L));
        ordered.verify(f.assets).updateByExampleSelective(argThat(a -> Integer.valueOf(1).equals(a.getSelectedFlag())), any());
        verify(f.assets, never()).updateByPrimaryKeySelective(any());
        verify(f.settlement).settle(eq(f.task), eq(f.model), eq(2));
        assertEquals("original-request", f.request.get().param("resume_request_id"));
        assertTrue(f.request.get().getPrompt().contains("人物走进门"));
        assertTrue(f.request.get().getPrompt().contains("暖色光线"));
    }

    @ParameterizedTest @ValueSource(strings = {"LOCKED", "VIDEO", "EDIT", "SHOT_VERSION", "PROMPT_VERSION", "OWNER", "LATER", "ACTIVE"})
    void changesDuringProviderWaitRemainUntouchedAndKeepUnselectedCandidate(String change) throws Exception {
        Fixture f = new Fixture(true);
        f.duringWait = () -> {
            ShortShot current = shot();
            switch (change) {
                case "LOCKED" -> current.setShotStatus("LOCKED");
                case "VIDEO" -> current.setVideoAssetId("new-selected-video");
                case "EDIT" -> current.setUpdatedTime(new Date(2000));
                case "SHOT_VERSION" -> current.setShotVersion(2);
                case "PROMPT_VERSION" -> current.setPromptVersion(2);
                case "OWNER" -> current.setUserId("other-owner");
                default -> {
                    ShortAiTask other = new ShortAiTask();
                    other.setId("LATER".equals(change) ? 10L : 8L);
                    other.setTaskStatus("LATER".equals(change) ? "FAILED" : "RUNNING");
                    when(f.tasks.selectByExample(any())).thenReturn(List.of(other));
                }
            }
            f.current.set(current);
        };
        f.worker.tick();
        f.assertRejected();
        verify(f.shots, never()).attachRecoveredVideoIfUnchanged(any(), any(), any());
    }

    @Test void finalCasMissNeverSelectsAssetOrInvalidatesFilm() throws Exception {
        Fixture f = new Fixture(true);
        when(f.shots.attachRecoveredVideoIfUnchanged(any(), any(), any())).thenReturn(0);
        f.worker.tick();
        f.assertRejected();
        verify(f.shots).attachRecoveredVideoIfUnchanged(any(), any(), eq(9L));
    }

    @Test void ordinaryVideoAlsoKeepsRealDurationInsteadOfRequestedShotLength() throws Exception {
        Fixture f = new Fixture(false);
        f.worker.tick();
        assertEquals(2, f.video().getDurationSec());
        verify(f.assets, never()).updateByPrimaryKeySelective(any());
        verify(f.settlement).settle(eq(f.task), eq(f.model), eq(2));
        assertNull(f.request.get().param("resume_request_id"));
    }

    @Test void selectionFailureRollsBackShotAttachmentWithoutSettlementOrLifecycleChanges() throws Exception {
        Fixture f = new Fixture(true);
        when(f.assets.updateByExampleSelective(any(), any())).thenReturn(0);
        f.worker.tick();
        assertEquals(0, f.video().getSelectedFlag());
        verify(f.transactions).rollback(any());
        verify(f.transactions, never()).commit(any());
        verify(f.settlement, never()).settle(any(), any(), anyInt());
        verifyNoInteractions(f.films, f.frames);
    }

    private static ShortShot shot() {
        ShortShot shot = new ShortShot();
        shot.setId(7L); shot.setShotId("shot"); shot.setUserId("owner"); shot.setWorkId("work");
        shot.setEpisodeId("episode"); shot.setShotNo(1); shot.setShotDurationSec(5);
        shot.setShotStatus("IMAGE_READY"); shot.setShotDesc("人物走进门"); shot.setExtraPrompt("暖色光线");
        shot.setUpdatedTime(new Date(1000)); shot.setDeletedFlag(0);
        shot.setShotVersion(1); shot.setPromptVersion(1);
        return shot;
    }

    private static final class Fixture {
        final ShortAiTaskWorker worker = new ShortAiTaskWorker();
        final ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
        final ShortShotMapper shots = mock(ShortShotMapper.class);
        final ShortAssetMapper assets = mock(ShortAssetMapper.class);
        final ShortTaskSettlementService settlement = mock(ShortTaskSettlementService.class);
        final FinishedFilmLifecycle films = mock(FinishedFilmLifecycle.class);
        final FrameLinkService frames = mock(FrameLinkService.class);
        final org.springframework.transaction.PlatformTransactionManager transactions = mock(org.springframework.transaction.PlatformTransactionManager.class);
        final ShortAiTask task = new ShortAiTask();
        final AiModelConfig model = new AiModelConfig();
        final AtomicReference<ShortShot> current = new AtomicReference<>(shot());
        final AtomicReference<AiProviderRequest> request = new AtomicReference<>();
        final List<ShortAsset> inserted = new ArrayList<>();
        Runnable duringWait = () -> { };

        Fixture(boolean recovery) throws Exception {
            var resolve = mock(AiModelResolveService.class);
            var registry = mock(AiProviderRegistry.class);
            var provider = mock(AiProvider.class);
            ReflectionTestUtils.setField(worker, "shortAiTaskMapper", tasks);
            ReflectionTestUtils.setField(worker, "shortShotMapper", shots);
            ReflectionTestUtils.setField(worker, "shortAssetMapper", assets);
            ReflectionTestUtils.setField(worker, "settlement", settlement);
            ReflectionTestUtils.setField(worker, "finishedFilmLifecycle", films);
            ReflectionTestUtils.setField(worker, "frameLinkService", frames);
            ReflectionTestUtils.setField(worker, "transactionManager", transactions);
            when(transactions.getTransaction(any())).thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());
            ReflectionTestUtils.setField(worker, "aiModelResolveService", resolve);
            ReflectionTestUtils.setField(worker, "aiProviderRegistry", registry);
            ReflectionTestUtils.setField(worker, "objectStorage", mock(ObjectStorage.class));
            ReflectionTestUtils.setField(worker, "environment", new MockEnvironment());
            ReflectionTestUtils.setField(worker, "aiTaskExecutor", new SyncTaskExecutor());
            task.setId(9L); task.setTaskId("task"); task.setTaskType("VIDEO"); task.setTargetType("SHOT");
            task.setTargetId("shot"); task.setUserId("owner"); task.setWorkId("work"); task.setEpisodeId("episode");
            task.setTaskStatus("QUEUED"); task.setModelCode("bgssai-video");
            task.setRequestJson(recovery ? "{\"resume_request_id\":\"original-request\"}" : "{}");
            model.setPriceUnit("PER_SECOND"); model.setModelCode("bgssai-video");
            when(tasks.selectByExample(any())).thenReturn(List.of(task));
            when(tasks.updateByExampleSelective(any(), any())).thenReturn(1);
            when(shots.selectOneByExample(any())).thenAnswer(c -> current.get());
            when(shots.attachRecoveredVideoIfUnchanged(any(), any(), any())).thenReturn(1);
            when(resolve.resolve(any(), any())).thenReturn(model);
            when(registry.resolve(model)).thenReturn(provider);
            byte[] video;
            try (var stream = WorkerVideoRecoveryTest.class.getResourceAsStream("/media/probe-video.mp4")) {
                video = stream.readAllBytes();
            }
            when(provider.execute(any())).thenAnswer(c -> {
                request.set(c.getArgument(0)); duringWait.run();
                return AiProviderResult.binary(video, "video/mp4");
            });
            when(assets.insertSelective(any())).thenAnswer(c -> { inserted.add(c.getArgument(0)); return 1; });
            when(assets.updateByExampleSelective(any(), any())).thenReturn(1);
            when(settlement.settle(any(), any(), anyInt())).thenReturn(true);
        }
        ShortAsset video() { return inserted.stream().filter(a -> "VIDEO".equals(a.getAssetType())).findFirst().orElseThrow(); }
        void assertRejected() {
            assertEquals(0, video().getSelectedFlag());
            assertEquals(2, video().getDurationSec());
            verify(assets, never()).updateByExampleSelective(any(), any());
            verify(settlement, never()).settle(any(), any(), anyInt());
            verifyNoInteractions(films, frames);
            ArgumentCaptor<ShortAiTask> updates = ArgumentCaptor.forClass(ShortAiTask.class);
            verify(tasks, atLeastOnce()).updateByExampleSelective(updates.capture(), any());
            assertEquals("FAILED", updates.getValue().getTaskStatus());
            assertTrue(updates.getValue().getFailReason().contains("未选候选"));
        }
    }
}
