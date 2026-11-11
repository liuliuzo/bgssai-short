package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.billing.UsageReceiptRow;
import com.bgssai.shortai.billing.UsageReceiptService;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.*;
import com.bgssai.shortai.production.dto.ProductionDtos.AiTaskReq;
import com.bgssai.shortai.production.support.ShortJson;

class VideoTaskRecoveryTest {
    final AiTaskService service = new AiTaskService();
    final WorkAccessService access = mock(WorkAccessService.class);
    final ShortAiTaskMapper tasks = mock(ShortAiTaskMapper.class);
    final ShortShotMapper shots = mock(ShortShotMapper.class);
    final AiModelResolveService models = mock(AiModelResolveService.class);
    final AiTaskQueue queue = mock(AiTaskQueue.class);
    final UsageReceiptService receipts = mock(UsageReceiptService.class);
    final ShortAiTask task = new ShortAiTask();
    final ShortShot shot = new ShortShot();
    final AiModelConfig model = new AiModelConfig();
    final AiTaskReq input = new AiTaskReq();

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service,"workAccessService",access);
        ReflectionTestUtils.setField(service,"shortAiTaskMapper",tasks);
        ReflectionTestUtils.setField(service,"shortShotMapper",shots);
        ReflectionTestUtils.setField(service,"aiModelResolveService",models);
        ReflectionTestUtils.setField(service,"aiTaskQueue",queue);
        ReflectionTestUtils.setField(service,"usageReceiptService",receipts);
        task.setId(20L); task.setTaskId("t-1"); task.setUserId("u"); task.setWorkId("w"); task.setEpisodeId("ep");
        task.setTaskType("VIDEO"); task.setTargetType("SHOT"); task.setTargetId("shot"); task.setTaskStatus("FAILED");
        task.setModelCode("bgssai-video"); task.setRequestJson("{\"duration_sec\":5}");
        shot.setShotId("shot"); shot.setUserId("u"); shot.setWorkId("w"); shot.setEpisodeId("ep"); shot.setShotStatus("VIDEO_GENERATING");
        model.setModelCode("bgssai-video"); model.setProviderCode("TENCENT_TOKENHUB"); model.setConfigJson("{\"hub\":\"cn\"}");
        input.setTaskId("t-1");
        when(access.requireUser()).thenReturn("u");
        when(tasks.selectOneByExample(any())).thenReturn(task);
        when(tasks.selectByExample(any())).thenReturn(List.of(task));
        when(shots.selectOneByExample(any())).thenReturn(shot);
        when(models.resolve("VIDEO","bgssai-video")).thenReturn(model);
        when(tasks.updateByExampleSelective(any(),any())).thenReturn(1);
        UsageReceiptRow receipt = new UsageReceiptRow(); receipt.requestId="981ab970-447e-4a04-a2f0-a085ebbf435b";
        when(receipts.requireVideoReceipt("u","w","t-1","bgssai-video")).thenReturn(receipt);
    }

    @Test void existingRecoverQueuesSameTaskWithTrustedReceiptInsteadOfNewGeneration() {
        assertTrue(service.recover(input).isSuccess());
        ArgumentCaptor<ShortAiTask> patch=ArgumentCaptor.forClass(ShortAiTask.class);
        ArgumentCaptor<ShortAiTaskExample> condition=ArgumentCaptor.forClass(ShortAiTaskExample.class);
        verify(tasks).updateByExampleSelective(patch.capture(),condition.capture());
        assertEquals("QUEUED",patch.getValue().getTaskStatus());
        assertEquals("981ab970-447e-4a04-a2f0-a085ebbf435b",ShortJson.map(patch.getValue().getRequestJson()).get("resume_request_id"));
        assertEquals(5,ShortJson.map(patch.getValue().getRequestJson()).get("duration_sec"));
        assertTrue(condition.getValue().getOredCriteria().get(0).getAllCriteria().stream()
            .anyMatch(c -> c.getCondition().equals("task_status =") && c.getValue().equals("FAILED")));
        verify(tasks,never()).insertSelective(any());
    }

    @Test void foreignTaskAndShotCannotResume() {
        task.setUserId("other"); assertThrows(RuntimeException.class,()->service.resumeVideo(input));
        task.setUserId("u"); shot.setUserId("other"); assertThrows(RuntimeException.class,()->service.resumeVideo(input));
        verify(tasks,never()).updateByExampleSelective(any(),any());
    }

    @Test void activeCompletedOrNonCompanyTasksCannotResume() {
        for(String state:List.of("RUNNING","QUEUED","SUCCEEDED")) {
            task.setTaskStatus(state); assertThrows(RuntimeException.class,()->service.resumeVideo(input));
        }
        task.setTaskStatus("FAILED"); task.setModelCode("third-party");
        assertThrows(RuntimeException.class,()->service.resumeVideo(input));
        verify(tasks,never()).updateByExampleSelective(any(),any());
    }

    @Test void existingOrLockedVideoIsNeverOverwritten() {
        shot.setVideoAssetId("existing"); assertThrows(RuntimeException.class,()->service.resumeVideo(input));
        shot.setVideoAssetId(null); shot.setShotStatus("LOCKED"); assertThrows(RuntimeException.class,()->service.resumeVideo(input));
        verify(tasks,never()).updateByExampleSelective(any(),any());
    }

    @Test void laterAttemptOrAnotherRunningTaskBlocksRecovery() {
        ShortAiTask other=new ShortAiTask(); other.setId(21L); other.setTaskStatus("FAILED");
        when(tasks.selectByExample(any())).thenReturn(List.of(task,other));
        assertThrows(RuntimeException.class,()->service.resumeVideo(input));
        other.setId(10L); other.setTaskStatus("RUNNING"); assertThrows(RuntimeException.class,()->service.resumeVideo(input));
        verify(tasks,never()).updateByExampleSelective(any(),any());
    }

    @Test void missingReceiptOrConcurrentStateChangeDoesNotReportQueued() {
        when(receipts.requireVideoReceipt(anyString(),anyString(),anyString(),anyString())).thenThrow(new IllegalArgumentException("ambiguous"));
        assertThrows(RuntimeException.class,()->service.resumeVideo(input));
        verify(tasks,never()).updateByExampleSelective(any(),any());
        UsageReceiptRow row=new UsageReceiptRow();row.requestId="request";
        doReturn(row).when(receipts).requireVideoReceipt(anyString(),anyString(),anyString(),anyString());
        when(tasks.updateByExampleSelective(any(),any())).thenReturn(0);
        assertThrows(RuntimeException.class,()->service.resumeVideo(input));
    }

    @Test void videoPromptRetainsStoryWhenStyleIsPresent() {
        shot.setShotDesc("A shopkeeper examines an old ticket"); shot.setExtraPrompt("warm amber light"); shot.setCameraMove("PUSH");
        String prompt=ReflectionTestUtils.invokeMethod(ShortAiTaskWorker.class,"shotPrompt",shot,"VIDEO");
        assertTrue(prompt.startsWith(shot.getShotDesc())); assertTrue(prompt.contains("warm amber light")); assertTrue(prompt.contains("PUSH"));
        shot.setShotDesc(""); shot.setImagePrompt("A rainy shop window");
        prompt=ReflectionTestUtils.invokeMethod(ShortAiTaskWorker.class,"shotPrompt",shot,"VIDEO");
        assertTrue(prompt.startsWith("A rainy shop window")); assertTrue(prompt.contains("warm amber light"));
    }
}
