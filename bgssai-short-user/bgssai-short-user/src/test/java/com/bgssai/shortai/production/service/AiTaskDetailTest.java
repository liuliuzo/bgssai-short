package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.model.ShortAiTask;

class AiTaskDetailTest {
    @Test void taskDetailRejectsForeignOwnerAndReturnsPublicSuccessfulResult() {
        AiTaskService service = new AiTaskService();
        WorkAccessService access = mock(WorkAccessService.class);
        ShortAiTaskMapper mapper = mock(ShortAiTaskMapper.class);
        ReflectionTestUtils.setField(service, "workAccessService", access);
        ReflectionTestUtils.setField(service, "shortAiTaskMapper", mapper);
        when(access.requireUser()).thenReturn("owner");
        ShortAiTask task = new ShortAiTask(); task.setTaskId("t-1"); task.setUserId("other");
        when(mapper.selectOneByExample(any())).thenReturn(task);
        assertThrows(ForbiddenException.class, () -> service.detail("t-1"));
        task.setUserId("owner"); task.setTaskType("COMPOSE"); task.setTaskStatus("SUCCEEDED");
        task.setResultJson("{\"asset_id\":\"film-real\"}");
        task.setRequestJson("{\"private_request\":\"must-not-appear\"}");
        Map<?, ?> result = (Map<?, ?>) service.detail("t-1").getResult();
        assertEquals("SUCCEEDED", result.get("task_status"));
        assertEquals("film-real", ((Map<?, ?>) result.get("result_json")).get("asset_id"));
        assertFalse(result.containsKey("request_json")); assertFalse(result.containsKey("user_id"));
        assertNull(result.get("fail_reason"));
    }
}
