package com.bgssai.shortai.loginlog.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.loginlog.dto.LoginLogItem;
import com.bgssai.shortai.loginlog.location.LoginIpLocationResolver;
import com.bgssai.shortai.mapper.LoginLogMapper;
import com.bgssai.shortai.model.LoginLog;
import com.bgssai.shortai.model.LoginLogExample;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

class ListLoginLogsServiceTest {
    private final LoginLogMapper mapper = mock(LoginLogMapper.class);
    private final LoginIpLocationResolver resolver = mock(LoginIpLocationResolver.class);
    private final ListLoginLogsService service = new ListLoginLogsService();

    ListLoginLogsServiceTest() {
        ReflectionTestUtils.setField(service, "loginLogMapper", mapper);
        ReflectionTestUtils.setField(service, "ipLocationResolver", resolver);
    }

    @AfterEach
    void cleanContext() {
        UserContext.clear();
        PageHelper.clearPage();
    }

    @Test
    void addsLocationWithoutChangingOwnershipPaginationOrStoredIp() throws Exception {
        UserContext.setUserId("current-user");
        UserContext.setUserRole("PLATFORM_ADMIN");
        LoginLog row = new LoginLog();
        row.setId(7);
        row.setUserId("current-user");
        row.setIpAddress("8.8.8.8");
        row.setCreateTime(new Date(0));
        Page<LoginLog> rows = new Page<>(2, 10);
        rows.setTotal(37);
        rows.add(row);
        when(mapper.selectByExample(any())).thenReturn(rows);
        when(resolver.resolve("8.8.8.8")).thenReturn("美国");

        PageResponse response = service.list(2, 10);
        LoginLogItem item = (LoginLogItem) ((List<?>) response.getResult()).get(0);
        assertEquals("8.8.8.8", item.getIpAddress());
        assertEquals("美国", item.getIpLocation());
        assertEquals("8.8.8.8", row.getIpAddress());
        assertEquals(2, response.getPageNum());
        assertEquals(10, response.getPageSize());
        assertEquals(37, response.getTotalSize());

        ArgumentCaptor<LoginLogExample> example = ArgumentCaptor.forClass(LoginLogExample.class);
        verify(mapper).selectByExample(example.capture());
        var criteria = example.getValue().getOredCriteria().get(0).getAllCriteria();
        assertTrue(criteria.stream().anyMatch(c -> c.getCondition().equals("del_flag =") && Boolean.FALSE.equals(c.getValue())));
        assertFalse(criteria.stream().anyMatch(c -> c.getCondition().equals("user_id =")));
        assertEquals("create_time DESC, id DESC", example.getValue().getOrderByClause());
        verifyNoMoreInteractions(mapper);
        ObjectMapper json = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        assertEquals("美国", json.readTree(json.writeValueAsString(item)).get("ip_location").asText());
    }

    @Test
    void unauthorizedAccessDoesNotQueryOrResolveAnyIp() {
        UserContext.setUserRole("USER");
        assertThrows(ForbiddenException.class, () -> service.list(1, 20));
        verifyNoInteractions(mapper, resolver);
    }

    @Test
    void exposesStoredDetailOnTheAdminList() throws Exception {
        UserContext.setUserId("admin");
        UserContext.setUserRole("PLATFORM_ADMIN");
        LoginLog row = new LoginLog();
        row.setId(3);
        row.setUserId("u-1");
        row.setAction("LOGIN_SUCCESS");
        row.setLoginResult("SUCCESS");
        row.setResultReason("登录成功");
        row.setLoginMethod("password");
        row.setIdentifierType("username");
        row.setIdentifierMasked("admin");
        row.setClientChannel("admin");
        row.setBrowserName("Edge 120");
        row.setOsName("Windows 10");
        row.setDeviceType("desktop");
        row.setDeviceLabel("Edge 120 · Windows 10 · 桌面");
        row.setIpAddress("8.8.8.8");
        row.setIpCountry("美国");
        row.setIpIsp("Example");
        row.setRiskNewDevice(Boolean.FALSE);
        row.setSessionId("adminsession123456");
        Page<LoginLog> rows = new Page<>(1, 20);
        rows.setTotal(1);
        rows.add(row);
        when(mapper.selectByExample(any())).thenReturn(rows);
        when(resolver.resolve("8.8.8.8")).thenReturn("美国");

        LoginLogItem item = (LoginLogItem) ((List<?>) service.list(1, 20).getResult()).get(0);
        assertEquals("SUCCESS", item.getLoginResult());
        assertEquals("admin", item.getClientChannel());
        assertEquals("Edge 120 · Windows 10 · 桌面", item.getDeviceLabel());
        assertEquals("美国", item.getIpCountry());
        assertEquals("Example", item.getIpIsp());
        assertEquals("snapshot", item.getIpGeoSource());
        assertEquals("曾用设备", item.getRiskSummary());
        ObjectMapper json = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        assertEquals("adminsession123456", json.readTree(json.writeValueAsString(item)).get("session_id").asText());
    }
}
