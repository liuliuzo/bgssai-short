package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.UUID;
import java.util.Arrays;
import java.util.stream.Collectors;
import com.bgssai.shortai.mapper.*;
import com.bgssai.shortai.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

class TerminalOutcomeCasTest {
    JdbcTemplate db;
    ShortShotMapper shots;
    ShortAiTaskMapper tasks;
    ShotGenerateOutcome outcome;
    ShortShot expected, patch;
    ShortAiTask task;

    @BeforeEach void setup() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        db = new JdbcTemplate(ds);
        schema("short_shot", ShortShot.class);
        schema("short_ai_task", ShortAiTask.class);
        var factory = new SqlSessionFactoryBean(); factory.setDataSource(ds);
        factory.setMapperLocations(new ClassPathResource("mybatis/mapper/ShortShotMapper.xml"),
                new ClassPathResource("mybatis/mapper/ShortAiTaskMapper.xml"));
        var session = new SqlSessionTemplate(factory.getObject());
        shots = session.getMapper(ShortShotMapper.class); tasks = session.getMapper(ShortAiTaskMapper.class);
        expected = new ShortShot(); expected.setId(7L); expected.setShotId("shot"); expected.setUserId("owner");
        expected.setWorkId("work"); expected.setEpisodeId("episode"); expected.setDeletedFlag(0);
        expected.setShotStatus("VIDEO_GENERATING"); expected.setVideoAssetId("retained-video"); expected.setFirstFrameAssetId("frame");
        expected.setShotVersion(1); expected.setPromptVersion(1); expected.setUpdatedTime(new Date(1000));
        shots.insertSelective(expected);
        task = new ShortAiTask(); task.setId(9L); task.setTaskId("original-task"); task.setUserId("owner");
        task.setWorkId("work"); task.setEpisodeId("episode"); task.setTargetId("shot"); task.setTargetType("SHOT");
        task.setTaskType("VIDEO"); task.setTaskStatus("FAILED"); task.setRetryCount(1); task.setUpdatedTime(new Date(1000));
        tasks.insertSelective(task);
        patch = new ShortShot(); patch.setShotStatus("VIDEO_READY"); patch.setUpdatedTime(new Date(2000));
        outcome = new ShotGenerateOutcome();
        ReflectionTestUtils.setField(outcome, "shortShotMapper", shots);
        ReflectionTestUtils.setField(outcome, "shortAiTaskMapper", tasks);
    }
    private void schema(String table, Class<?> model) {
        String fields = Arrays.stream(model.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers()))
            .map(f -> f.getName().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase()
                + " " + (Date.class.isAssignableFrom(f.getType()) ? "timestamp" : Number.class.isAssignableFrom(f.getType()) ? "bigint" : "varchar(4096)")
                + ("id".equals(f.getName()) ? " primary key" : ""))
            .collect(Collectors.joining(","));
        db.execute("create table " + table + "(" + fields + ")");
    }
    @Test void currentTerminalTaskRestoresStatusWithoutChangingAssetIds() {
        outcome.restoreBoundMedia(task);
        ShortShot after = shots.selectByPrimaryKey(7L);
        assertEquals("VIDEO_READY", after.getShotStatus());
        assertEquals("retained-video", after.getVideoAssetId());
        assertEquals("frame", after.getFirstFrameAssetId());
    }
    @ParameterizedTest @ValueSource(strings = {"QUEUED", "RUNNING", "SUCCEEDED"})
    void oldFailureCallbackCannotChangeSameTaskAfterRecovery(String status) {
        db.update("update short_ai_task set task_status=?,retry_count=2", status);
        outcome.restoreBoundMedia(task);
        assertEquals("VIDEO_GENERATING", shots.selectByPrimaryKey(7L).getShotStatus());
    }
    @Test void sameTaskRecoveryBetweenApplicationChecksAndSqlIsRejected() {
        ShortShotMapper intercepted = mock(ShortShotMapper.class);
        when(intercepted.selectOneByExample(any())).thenAnswer(call -> {
            db.update("update short_ai_task set task_status='RUNNING',retry_count=2");
            return expected;
        });
        when(intercepted.restoreBoundMediaAfterTerminalTask(any(), any(), any())).thenAnswer(call ->
            shots.restoreBoundMediaAfterTerminalTask(call.getArgument(0),call.getArgument(1),call.getArgument(2)));
        ReflectionTestUtils.setField(outcome, "shortShotMapper", intercepted);
        outcome.restoreBoundMedia(task);
        verify(intercepted).restoreBoundMediaAfterTerminalTask(any(), any(), any());
        assertEquals("VIDEO_GENERATING", shots.selectByPrimaryKey(7L).getShotStatus());
    }
    @ParameterizedTest @ValueSource(strings = {"TASK_RETRY", "TASK_TIME", "SHOT_VERSION", "PROMPT_VERSION", "SHOT_TIME", "VIDEO", "FRAME", "LOCK", "LATER", "ACTIVE", "OWNER"})
    void finalSqlRejectsStaleSnapshotsEvenWithUnchangedSecondTimestamp(String change) {
        switch (change) {
            case "TASK_RETRY" -> db.update("update short_ai_task set retry_count=2");
            case "TASK_TIME" -> db.update("update short_ai_task set updated_time=?", new Date(5000));
            case "SHOT_VERSION" -> db.update("update short_shot set shot_version=2");
            case "PROMPT_VERSION" -> db.update("update short_shot set prompt_version=2");
            case "SHOT_TIME" -> db.update("update short_shot set updated_time=?", new Date(5000));
            case "VIDEO" -> db.update("update short_shot set video_asset_id='new-video'");
            case "FRAME" -> db.update("update short_shot set first_frame_asset_id='new-frame'");
            case "LOCK" -> db.update("update short_shot set shot_status='LOCKED'");
            case "OWNER" -> db.update("update short_ai_task set user_id='other'");
            default -> {
                ShortAiTask other = tasks.selectByPrimaryKey(9L);
                other.setId("LATER".equals(change) ? 10L : 8L); other.setTaskId("other");
                other.setTaskStatus("LATER".equals(change) ? "FAILED" : "PAUSED"); tasks.insertSelective(other);
            }
        }
        var before = db.queryForMap("select * from short_shot");
        assertEquals(0, shots.restoreBoundMediaAfterTerminalTask(expected,task,patch));
        assertEquals(before, db.queryForMap("select * from short_shot"));
    }
    @Test void workerCannotFailANewerRetryEvenWhenItIsRunningAgain() {
        db.update("update short_ai_task set task_status='RUNNING',retry_count=2");
        ShortAiTaskWorker worker = new ShortAiTaskWorker();
        ReflectionTestUtils.setField(worker,"shortAiTaskMapper",tasks);
        Boolean changed = ReflectionTestUtils.invokeMethod(worker,"failIfRunning",task,"MODEL_TIMEOUT","stale error");
        assertEquals(Boolean.FALSE,changed);
        assertEquals("RUNNING",tasks.selectByPrimaryKey(9L).getTaskStatus());
    }
    @ParameterizedTest @ValueSource(strings = {"SUCCEEDED", "RUNNING"})
    void cancelCasMissCannotCancelSuccessOrANewerRetryAndDoesNotNotifyOutcome(String concurrentStatus) {
        db.update("update short_ai_task set task_status='RUNNING'");
        ShortAiTaskMapper racing = mock(ShortAiTaskMapper.class);
        when(racing.selectOneByExample(any())).thenAnswer(call -> tasks.selectOneByExample(call.getArgument(0)));
        when(racing.updateByExampleSelective(any(), any())).thenAnswer(call -> {
            db.update("update short_ai_task set task_status=?,retry_count=2", concurrentStatus);
            return tasks.updateByExampleSelective(call.getArgument(0),call.getArgument(1));
        });
        var access = mock(WorkAccessService.class); when(access.requireUser()).thenReturn("owner");
        var callback = mock(ShotGenerateOutcome.class);
        var service = new AiTaskService();
        ReflectionTestUtils.setField(service,"workAccessService",access);
        ReflectionTestUtils.setField(service,"shortAiTaskMapper",racing);
        ReflectionTestUtils.setField(service,"shotGenerateOutcome",callback);
        var req = new com.bgssai.shortai.production.dto.ProductionDtos.AiTaskReq(); req.setTaskId("original-task");
        assertThrows(com.bgssai.shortai.common.exception.BizException.class, () -> service.cancel(req));
        assertEquals(concurrentStatus,tasks.selectByPrimaryKey(9L).getTaskStatus());
        verifyNoInteractions(callback);
    }
}
