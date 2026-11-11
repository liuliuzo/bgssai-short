package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import java.util.UUID;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.ShortShot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class RecoveredVideoCasMapperTest {
    JdbcTemplate db;
    ShortShotMapper mapper;
    org.springframework.jdbc.datasource.DataSourceTransactionManager transactions;
    ShortShot expected, patch;
    @BeforeEach void setup() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        db = new JdbcTemplate(ds);
        transactions = new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds);
        db.execute("create table short_shot(id bigint primary key, shot_id varchar(64), user_id varchar(64), work_id varchar(64), episode_id varchar(64), deleted_flag int, shot_status varchar(32), updated_time timestamp, video_asset_id varchar(64), last_frame_asset_id varchar(64), shot_version int, prompt_version int)");
        db.execute("create table short_ai_task(id bigint primary key, user_id varchar(64), work_id varchar(64), episode_id varchar(64), target_id varchar(64), target_type varchar(32), task_type varchar(32), task_status varchar(32))");
        db.update("insert into short_shot values(7,'shot','owner','work','episode',0,'IMAGE_READY',?,null,null,1,1)", new Date(1000));
        db.update("insert into short_ai_task values(9,'owner','work','episode','shot','SHOT','VIDEO','RUNNING')");
        var factory = new SqlSessionFactoryBean();
        factory.setDataSource(ds);
        factory.setMapperLocations(new ClassPathResource("mybatis/mapper/ShortShotMapper.xml"));
        mapper = new SqlSessionTemplate(factory.getObject()).getMapper(ShortShotMapper.class);
        expected = new ShortShot(); expected.setId(7L); expected.setShotId("shot"); expected.setUserId("owner");
        expected.setWorkId("work"); expected.setEpisodeId("episode"); expected.setShotStatus("IMAGE_READY"); expected.setUpdatedTime(new Date(1000));
        expected.setShotVersion(1); expected.setPromptVersion(1);
        patch = new ShortShot(); patch.setVideoAssetId("recovered"); patch.setLastFrameAssetId("tail"); patch.setUpdatedTime(new Date(3000));
    }
    @Test void nullAndBlankVideoAreAttachedOnceWithExactSnapshot() {
        assertEquals(1, mapper.attachRecoveredVideoIfUnchanged(expected, patch, 9L));
        assertEquals(0, mapper.attachRecoveredVideoIfUnchanged(expected, patch, 9L));
        assertEquals("recovered", db.queryForObject("select video_asset_id from short_shot", String.class));
        assertEquals("tail", db.queryForObject("select last_frame_asset_id from short_shot", String.class));
        db.update("update short_shot set video_asset_id='  ',shot_status='IMAGE_READY',updated_time=?", new Date(1000));
        expected.setVideoAssetId("  ");
        assertEquals(1, mapper.attachRecoveredVideoIfUnchanged(expected, patch, 9L));
    }
    @ParameterizedTest @ValueSource(strings = {"LOCK", "VIDEO", "TIME", "SHOT_VERSION", "PROMPT_VERSION", "STATUS", "DELETED", "OWNER", "CANCELED", "LATER", "ACTIVE"})
    void changesAfterPrecheckAreRejectedByActualSql(String change) {
        switch (change) {
            case "LOCK" -> db.update("update short_shot set shot_status='LOCKED'");
            case "VIDEO" -> db.update("update short_shot set video_asset_id='new-selected'");
            case "TIME" -> db.update("update short_shot set updated_time=?", new Date(2000));
            case "SHOT_VERSION" -> db.update("update short_shot set shot_version=2");
            case "PROMPT_VERSION" -> db.update("update short_shot set prompt_version=2");
            case "STATUS" -> db.update("update short_shot set shot_status='GENERATING'");
            case "DELETED" -> db.update("update short_shot set deleted_flag=1");
            case "OWNER" -> db.update("update short_shot set user_id='other'");
            case "CANCELED" -> db.update("update short_ai_task set task_status='CANCELED'");
            case "LATER" -> db.update("insert into short_ai_task values(10,'owner','work','episode','shot','SHOT','VIDEO','FAILED')");
            case "ACTIVE" -> db.update("insert into short_ai_task values(8,'owner','work','episode','shot','SHOT','VIDEO','PAUSED')");
        }
        var before = db.queryForMap("select * from short_shot");
        assertEquals(0, mapper.attachRecoveredVideoIfUnchanged(expected, patch, 9L));
        assertEquals(before, db.queryForMap("select * from short_shot"));
    }
    @Test void attachmentCanRollBackWithSelectionFailureInTheSameDatabaseTransaction() {
        var before = db.queryForMap("select * from short_shot");
        assertThrows(IllegalStateException.class, () ->
                new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(tx -> {
                    assertEquals(1, mapper.attachRecoveredVideoIfUnchanged(expected, patch, 9L));
                    throw new IllegalStateException("selection update failed");
                }));
        assertEquals(before, db.queryForMap("select * from short_shot"));
    }
    @ParameterizedTest @ValueSource(strings = {"shot_version", "prompt_version"})
    void nullVersionIsMatchedAsNullAndNeverAsZero(String field) {
        if ("shot_version".equals(field)) expected.setShotVersion(null);
        else expected.setPromptVersion(null);
        db.update("update short_shot set " + field + "=0");
        assertEquals(0, mapper.attachRecoveredVideoIfUnchanged(expected, patch, 9L));
        db.update("update short_shot set " + field + "=null");
        assertEquals(1, mapper.attachRecoveredVideoIfUnchanged(expected, patch, 9L));
    }
    @ParameterizedTest @ValueSource(strings = {"user_id", "work_id", "episode_id", "target_id", "target_type", "task_type"})
    void unrelatedTaskDoesNotBlockButOriginalTaskMustMatchAllOwnershipDimensions(String field) {
        db.update("insert into short_ai_task values(10,'owner','work','episode','shot','SHOT','VIDEO','RUNNING')");
        db.update("update short_ai_task set " + field + "='unrelated' where id=10");
        assertEquals(1, mapper.attachRecoveredVideoIfUnchanged(expected, patch, 9L));
        db.update("update short_shot set video_asset_id=null,shot_status='IMAGE_READY',updated_time=?", new Date(1000));
        db.update("update short_ai_task set " + field + "='unrelated' where id=9");
        assertEquals(0, mapper.attachRecoveredVideoIfUnchanged(expected, patch, 9L));
    }
}
