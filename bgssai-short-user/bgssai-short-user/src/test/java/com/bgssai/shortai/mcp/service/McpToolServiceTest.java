package com.bgssai.shortai.mcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.health.service.QueryLivenessService;
import com.bgssai.shortai.health.service.QueryReadinessService;
import com.bgssai.shortai.loginlog.service.ListLoginLogsService;
import com.bgssai.shortai.mapper.McpAccessTokenMapper;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortScriptMapper;
import com.bgssai.shortai.mapper.ShortScriptVersionMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.mcp.domain.McpCaller;
import com.bgssai.shortai.model.McpAccessToken;
import com.bgssai.shortai.model.McpAccessTokenExample;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortScript;
import com.bgssai.shortai.model.ShortScriptExample;
import com.bgssai.shortai.model.ShortScriptVersion;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;
import com.bgssai.shortai.production.dto.ProductionDtos.FilmPublishToMediaReq;
import com.bgssai.shortai.production.service.FilmPublishToMediaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@ExtendWith(MockitoExtension.class)
class McpToolServiceTest {

    private static final String CALLER_USER = "u-owner-001";
    private static final String OTHER_USER = "u-other-999";

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private ListLoginLogsService listLoginLogsService;
    @Mock
    private QueryLivenessService queryLivenessService;
    @Mock
    private QueryReadinessService queryReadinessService;
    @Mock
    private ShortWorkMapper shortWorkMapper;
    @Mock
    private ShortEpisodeMapper shortEpisodeMapper;
    @Mock
    private ShortScriptMapper shortScriptMapper;
    @Mock
    private ShortScriptVersionMapper shortScriptVersionMapper;
    @Mock
    private ShortShotMapper shortShotMapper;
    @Mock
    private ShortAiTaskMapper shortAiTaskMapper;
    @Mock
    private FilmPublishToMediaService filmPublishToMediaService;
    @Mock
    private McpAccessTokenMapper mcpAccessTokenMapper;

    private McpToolService service;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(CALLER_USER);
        service = new McpToolService(
                listLoginLogsService,
                queryLivenessService,
                queryReadinessService,
                shortWorkMapper,
                shortEpisodeMapper,
                shortScriptMapper,
                shortScriptVersionMapper,
                shortShotMapper,
                shortAiTaskMapper,
                filmPublishToMediaService);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void listToolDefinitions_containsAllBusinessAndProbeTools() {
        ArrayNode tools = service.listToolDefinitions();
        assertEquals(11, tools.size());
        List<String> names = new ArrayList<>();
        for (JsonNode t : tools) {
            names.add(t.get("name").asText());
            assertEquals("object", t.get("inputSchema").get("type").asText());
        }
        assertTrue(names.containsAll(List.of(
                "list_login_logs",
                "get_liveness",
                "get_readiness",
                "list_works",
                "get_work",
                "create_work",
                "list_episodes",
                "save_episode_script",
                "list_shots",
                "submit_render_task",
                "publish_episode_to_media")));
        assertTrue(service.hasTool("list_my_login_logs"));
        assertTrue(service.hasTool("health_live"));
        assertTrue(service.hasTool("health_ready"));
        assertFalse(service.hasTool("unknown_tool_xyz"));
    }

    @Test
    void listWorks_and_getWork_enforceCallerOwnership() throws Exception {
        ShortWork work = new ShortWork();
        work.setId(1L);
        work.setWorkId("w-101");
        work.setUserId(CALLER_USER);
        work.setWorkTitle("逆袭风云");
        work.setOneLinePitch("三年之期已到");
        work.setGenre("REVENGE");
        work.setWorkStatus("DRAFT");
        work.setEpisodeCount(2);
        work.setEpisodeDurationSec(90);

        when(shortWorkMapper.selectByExample(any(ShortWorkExample.class))).thenReturn(List.of(work));
        McpToolService.ToolResult listRes = service.callTool("list_works",
                mapper.readTree("{\"page_num\":1,\"page_size\":10}"));
        assertFalse(listRes.isError());
        JsonNode listJson = mapper.readTree(listRes.text());
        assertEquals(1, listJson.get("total_size").asInt());
        assertEquals("w-101", listJson.get("works").get(0).get("work_id").asText());

        ShortEpisode ep = new ShortEpisode();
        ep.setEpisodeId("e-101");
        ep.setWorkId("w-101");
        ep.setUserId(CALLER_USER);
        ep.setEpisodeNo(1);
        ep.setEpisodeTitle("第1集");
        ep.setScriptStatus("READY");
        when(shortWorkMapper.selectOneByExample(any(ShortWorkExample.class))).thenReturn(work);
        when(shortEpisodeMapper.selectByExample(any(ShortEpisodeExample.class))).thenReturn(List.of(ep));

        McpToolService.ToolResult detailRes = service.callTool("get_work",
                mapper.readTree("{\"work_id\":\"w-101\"}"));
        assertFalse(detailRes.isError());
        JsonNode detailJson = mapper.readTree(detailRes.text());
        assertEquals("逆袭风云", detailJson.get("work_title").asText());
        assertEquals(1, detailJson.get("episodes").size());

        // Cross-user isolation: another user's work must be rejected
        ShortWork foreignWork = new ShortWork();
        foreignWork.setWorkId("w-foreign");
        foreignWork.setUserId(OTHER_USER);
        when(shortWorkMapper.selectOneByExample(any(ShortWorkExample.class))).thenReturn(foreignWork);

        McpToolService.ToolResult forbiddenRes = service.callTool("get_work",
                mapper.readTree("{\"work_id\":\"w-foreign\"}"));
        assertTrue(forbiddenRes.isError());
        assertTrue(forbiddenRes.text().contains("无权访问"));
    }

    @Test
    void createWork_createsWorkAndInitializesEpisodesForCaller() throws Exception {
        McpToolService.ToolResult res = service.callTool("create_work",
                mapper.readTree("{\"work_title\":\"法医手记\",\"one_line_pitch\":\"真相只有一个\",\"genre\":\"SUSPENSE\",\"episode_count\":3,\"episode_duration_sec\":60}"));
        assertFalse(res.isError());
        JsonNode json = mapper.readTree(res.text());
        assertEquals(CALLER_USER, json.get("user_id").asText());
        assertEquals("法医手记", json.get("work_title").asText());
        assertEquals("DRAFT", json.get("work_status").asText());
        assertEquals(3, json.get("episode_count").asInt());

        verify(shortWorkMapper, times(1)).insertSelective(any(ShortWork.class));
        verify(shortEpisodeMapper, times(3)).insertSelective(any(ShortEpisode.class));
    }

    @Test
    void saveEpisodeScript_persistsDraftVersionAndRejectsCrossUserEpisode() throws Exception {
        ShortWork work = new ShortWork();
        work.setWorkId("w-101");
        work.setUserId(CALLER_USER);
        work.setEpisodeDurationSec(90);

        ShortEpisode ep = new ShortEpisode();
        ep.setId(11L);
        ep.setEpisodeId("e-101");
        ep.setWorkId("w-101");
        ep.setUserId(CALLER_USER);
        ep.setEpisodeNo(1);
        ep.setEpisodeTitle("第1集");

        when(shortEpisodeMapper.selectOneByExample(any(ShortEpisodeExample.class))).thenReturn(ep);
        when(shortWorkMapper.selectOneByExample(any(ShortWorkExample.class))).thenReturn(work);
        when(shortScriptMapper.selectOneByExample(any(ShortScriptExample.class))).thenReturn(null);

        McpToolService.ToolResult saveRes = service.callTool("save_episode_script",
                mapper.readTree("{\"episode_id\":\"e-101\",\"episode_title\":\"雨夜追踪\",\"script_content\":\"暴雨夜，林法医推开解剖室大门。\"}"));
        assertFalse(saveRes.isError());
        JsonNode saveJson = mapper.readTree(saveRes.text());
        assertEquals("e-101", saveJson.get("episode_id").asText());
        assertEquals(1, saveJson.get("script_version").asInt());
        assertEquals("DRAFT", saveJson.get("script_status").asText());
        verify(shortScriptMapper, times(1)).insertSelective(any(ShortScript.class));
        verify(shortScriptVersionMapper, times(1)).insertSelective(any(ShortScriptVersion.class));

        // Cross-user episode rejection
        ShortEpisode foreignEp = new ShortEpisode();
        foreignEp.setEpisodeId("e-foreign");
        foreignEp.setWorkId("w-foreign");
        foreignEp.setUserId(OTHER_USER);
        when(shortEpisodeMapper.selectOneByExample(any(ShortEpisodeExample.class))).thenReturn(foreignEp);

        McpToolService.ToolResult forbiddenSave = service.callTool("save_episode_script",
                mapper.readTree("{\"episode_id\":\"e-foreign\",\"script_content\":\"越权修改\"}"));
        assertTrue(forbiddenSave.isError());
        assertTrue(forbiddenSave.text().contains("无权访问"));
    }

    @Test
    void listEpisodes_and_listShots_returnOwnedItemsOnly() throws Exception {
        ShortWork work = new ShortWork();
        work.setWorkId("w-101");
        work.setUserId(CALLER_USER);
        when(shortWorkMapper.selectOneByExample(any(ShortWorkExample.class))).thenReturn(work);

        ShortEpisode ep = new ShortEpisode();
        ep.setEpisodeId("e-101");
        ep.setWorkId("w-101");
        ep.setUserId(CALLER_USER);
        ep.setEpisodeNo(1);
        when(shortEpisodeMapper.selectByExample(any(ShortEpisodeExample.class))).thenReturn(List.of(ep));

        McpToolService.ToolResult epRes = service.callTool("list_episodes",
                mapper.readTree("{\"work_id\":\"w-101\"}"));
        assertFalse(epRes.isError());
        assertEquals(1, mapper.readTree(epRes.text()).get("episodes").size());

        when(shortEpisodeMapper.selectOneByExample(any(ShortEpisodeExample.class))).thenReturn(ep);
        ShortShot shot = new ShortShot();
        shot.setShotId("sh-1");
        shot.setEpisodeId("e-101");
        shot.setWorkId("w-101");
        shot.setUserId(CALLER_USER);
        shot.setShotNo(1);
        shot.setShotStatus("VIDEO_READY");
        when(shortShotMapper.selectByExample(any(ShortShotExample.class))).thenReturn(List.of(shot));

        McpToolService.ToolResult shotRes = service.callTool("list_shots",
                mapper.readTree("{\"episode_id\":\"e-101\"}"));
        assertFalse(shotRes.isError());
        JsonNode shotJson = mapper.readTree(shotRes.text());
        assertEquals(1, shotJson.get("shots").size());
        assertEquals("sh-1", shotJson.get("shots").get(0).get("shot_id").asText());
    }

    @Test
    void submitRenderTask_requiresConfirmCostTrueAndEnqueuesAsyncTask() throws Exception {
        ShortEpisode ep = new ShortEpisode();
        ep.setEpisodeId("e-101");
        ep.setWorkId("w-101");
        ep.setUserId(CALLER_USER);

        // Missing confirm_cost -> rejected without DB write
        McpToolService.ToolResult missingConfirm = service.callTool("submit_render_task",
                mapper.readTree("{\"episode_id\":\"e-101\",\"task_type\":\"COMPOSE\"}"));
        assertTrue(missingConfirm.isError());
        assertTrue(missingConfirm.text().contains("confirm_cost=true"));
        verify(shortAiTaskMapper, never()).insertSelective(any(ShortAiTask.class));

        // confirm_cost=false -> rejected without DB write
        McpToolService.ToolResult falseConfirm = service.callTool("submit_render_task",
                mapper.readTree("{\"episode_id\":\"e-101\",\"task_type\":\"COMPOSE\",\"confirm_cost\":false}"));
        assertTrue(falseConfirm.isError());
        verify(shortAiTaskMapper, never()).insertSelective(any(ShortAiTask.class));

        // confirm_cost=true -> enqueues QUEUED task
        when(shortEpisodeMapper.selectOneByExample(any(ShortEpisodeExample.class))).thenReturn(ep);
        McpToolService.ToolResult okRes = service.callTool("submit_render_task",
                mapper.readTree("{\"episode_id\":\"e-101\",\"task_type\":\"COMPOSE\",\"confirm_cost\":true}"));
        assertFalse(okRes.isError());
        JsonNode okJson = mapper.readTree(okRes.text());
        assertEquals("QUEUED", okJson.get("task_status").asText());
        assertEquals("COMPOSE", okJson.get("task_type").asText());
        assertTrue(okJson.get("confirm_cost").asBoolean());

        ArgumentCaptor<ShortAiTask> captor = ArgumentCaptor.forClass(ShortAiTask.class);
        verify(shortAiTaskMapper, times(1)).insertSelective(captor.capture());
        assertEquals(CALLER_USER, captor.getValue().getUserId());
        assertEquals("QUEUED", captor.getValue().getTaskStatus());
    }

    @Test
    void publishEpisodeToMedia_requiresConfirmPublishTrueAndApprovedReview() throws Exception {
        // Missing confirm_publish -> rejected immediately
        McpToolService.ToolResult noConfirm = service.callTool("publish_episode_to_media",
                mapper.readTree("{\"episode_id\":\"e-101\"}"));
        assertTrue(noConfirm.isError());
        assertTrue(noConfirm.text().contains("confirm_publish=true"));
        verify(filmPublishToMediaService, never()).publish(any(FilmPublishToMediaReq.class));

        // Unapproved episode -> rejected before Media ingest
        ShortEpisode pendingEp = new ShortEpisode();
        pendingEp.setEpisodeId("e-101");
        pendingEp.setWorkId("w-101");
        pendingEp.setUserId(CALLER_USER);
        pendingEp.setReviewStatus("PENDING_REVIEW");
        when(shortEpisodeMapper.selectOneByExample(any(ShortEpisodeExample.class))).thenReturn(pendingEp);

        McpToolService.ToolResult unapprovedRes = service.callTool("publish_episode_to_media",
                mapper.readTree("{\"episode_id\":\"e-101\",\"confirm_publish\":true}"));
        assertTrue(unapprovedRes.isError());
        assertTrue(unapprovedRes.text().contains("APPROVED"));
        verify(filmPublishToMediaService, never()).publish(any(FilmPublishToMediaReq.class));

        // Approved episode + confirm_publish=true -> delegates to FilmPublishToMediaService
        ShortEpisode approvedEp = new ShortEpisode();
        approvedEp.setEpisodeId("e-101");
        approvedEp.setWorkId("w-101");
        approvedEp.setUserId(CALLER_USER);
        approvedEp.setReviewStatus("APPROVED");
        when(shortEpisodeMapper.selectOneByExample(any(ShortEpisodeExample.class))).thenReturn(approvedEp);

        Map<String, Object> mediaResult = new LinkedHashMap<>();
        mediaResult.put("episode_id", "e-101");
        mediaResult.put("work_id", "w-101");
        mediaResult.put("media_id", "med-9001");
        mediaResult.put("status", "PUBLISHED");
        mediaResult.put("commercial_ready", true);
        when(filmPublishToMediaService.publish(any(FilmPublishToMediaReq.class)))
                .thenReturn(Responses.ok(mediaResult));

        McpToolService.ToolResult pubRes = service.callTool("publish_episode_to_media",
                mapper.readTree("{\"episode_id\":\"e-101\",\"confirm_publish\":true}"));
        assertFalse(pubRes.isError());
        JsonNode pubJson = mapper.readTree(pubRes.text());
        assertEquals("med-9001", pubJson.get("media_id").asText());
        assertEquals("PUBLISHED", pubJson.get("status").asText());
    }

    @Test
    void mcpTokenService_rejectsInvalidOrRevokedPatAndResolvesActivePat() {
        McpTokenService tokenService = new McpTokenService(mcpAccessTokenMapper);
        assertNull(tokenService.verify(null));
        assertNull(tokenService.verify("invalid_prefix_token"));

        when(mcpAccessTokenMapper.selectOneByExample(any(McpAccessTokenExample.class))).thenReturn(null);
        assertNull(tokenService.verify("bgs_mcp_revoked_or_unknown_token"));

        McpAccessToken activeRow = new McpAccessToken();
        activeRow.setId(5L);
        activeRow.setUserId(CALLER_USER);
        activeRow.setTokenHash("bgs_mcp_active_valid_token");
        activeRow.setDelFlag(Boolean.FALSE);
        when(mcpAccessTokenMapper.selectOneByExample(any(McpAccessTokenExample.class))).thenReturn(activeRow);

        McpCaller caller = tokenService.verify("bgs_mcp_active_valid_token");
        assertNotNull(caller);
        assertEquals(CALLER_USER, caller.getUserId());
    }
}
