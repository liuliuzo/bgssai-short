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
import com.bgssai.shortai.common.exception.UnauthorizedException;
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
        UserContext.setUserRole("USER");
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
        assertTrue(criteria.stream().anyMatch(c -> c.getCondition().equals("user_id =") && "current-user".equals(c.getValue())));
        assertEquals("create_time DESC, id DESC", example.getValue().getOrderByClause());
        verifyNoMoreInteractions(mapper);
        ObjectMapper json = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        assertEquals("美国", json.readTree(json.writeValueAsString(item)).get("ip_location").asText());
    }

    @Test
    void unauthorizedAccessDoesNotQueryOrResolveAnyIp() {
        assertThrows(UnauthorizedException.class, () -> service.list(1, 20));
        verifyNoInteractions(mapper, resolver);
    }

    @Test
    void exposesStoredDetailAndDerivesOlderRowsFromActionAndUserAgent() throws Exception {
        UserContext.setUserId("current-user");
        UserContext.setUserRole("USER");
        LoginLog row = new LoginLog();
        row.setId(9);
        row.setUserId("current-user");
        row.setAction("LOGIN_FAIL_PASSWORD");
        row.setIpAddress("203.0.113.7");
        row.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36");
        row.setLoginMethod("password");
        row.setIdentifierMasked("d***@example.com");
        row.setClientChannel("web");
        row.setSessionId("abc123sessionvalue");
        Page<LoginLog> rows = new Page<>(1, 20);
        rows.setTotal(1);
        rows.add(row);
        when(mapper.selectByExample(any())).thenReturn(rows);
        when(resolver.resolve("203.0.113.7")).thenReturn("非公网地址");
        when(resolver.lookup("203.0.113.7")).thenReturn(null);

        LoginLogItem item = (LoginLogItem) ((List<?>) service.list(1, 20).getResult()).get(0);
        assertEquals("FAILURE", item.getLoginResult());
        assertEquals("账号或密码不正确", item.getResultReason());
        assertEquals("password", item.getLoginMethod());
        assertEquals("d***@example.com", item.getIdentifierMasked());
        assertEquals("web", item.getClientChannel());
        assertEquals("Chrome 120", item.getBrowserName());
        assertEquals("Windows 10", item.getOsName());
        assertEquals("abc123sessionvalue", item.getSessionId());
        assertEquals("none", item.getIpGeoSource());
        ObjectMapper json = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        var tree = json.readTree(json.writeValueAsString(item));
        assertEquals("d***@example.com", tree.get("identifier_masked").asText());
        assertEquals("password", tree.get("login_method").asText());
        assertTrue(tree.get("logout_time").isNull());
    }
}
