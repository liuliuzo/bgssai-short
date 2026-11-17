package com.bgssai.shortai.login.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.AdminAuthGuard;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.session.SingleSessionGuard;
import com.bgssai.shortai.login.dto.req.ReqAdminLogin;
import com.bgssai.shortai.login.dto.resp.RespAdminLogin;
import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.SysUser;
import com.bgssai.shortai.utils.JwtUtil;

class AdminLoginServiceTest {

    @BeforeAll
    static void initJwt() {
        ReflectionTestUtils.invokeMethod(JwtUtil.class, "configure",
                "test-only-admin-jwt-secret-key-minimum-32-bytes-0123", 3_600_000L);
    }

    @Test
    void excludesNonAdminAndIssuesPlatformAdminOnly() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        when(mapper.updateByExampleSelective(any(), any())).thenReturn(1);
        LoginAuditService audit = mock(LoginAuditService.class);

        SingleSessionGuard guard = new SingleSessionGuard();
        ReflectionTestUtils.setField(guard, "sysUserMapper", mapper);

        AdminLoginService service = new AdminLoginService();
        ReflectionTestUtils.setField(service, "sysUserMapper", mapper);
        ReflectionTestUtils.setField(service, "passwordService", new PasswordService());
        ReflectionTestUtils.setField(service, "loginAuditService", audit);
        ReflectionTestUtils.setField(service, "singleSessionGuard", guard);

        SysUser regularUser = new SysUser();
        regularUser.setUserId("u-user-1");
        regularUser.setUserName("demo");
        regularUser.setPassword("secret");
        regularUser.setOrgCode("USER");
        regularUser.setDelFlag(false);
        when(mapper.selectOneByExample(any())).thenReturn(regularUser);

        ReqAdminLogin userReq = new ReqAdminLogin();
        userReq.setIdentifier("demo");
        userReq.setPassword("secret");
        BizException denied = assertThrows(BizException.class, () -> service.login(userReq, null));
        assertEquals(CommonErrorCode.FORBIDDEN.code(), denied.getCode());

        SysUser adminUser = new SysUser();
        adminUser.setUserId("u-admin-1");
        adminUser.setUserName("admin");
        adminUser.setRealName("系统管理员");
        adminUser.setPassword("secret");
        adminUser.setOrgCode(AdminAuthGuard.ADMIN_ROLE);
        adminUser.setDelFlag(false);
        when(mapper.selectOneByExample(any())).thenReturn(adminUser);

        ReqAdminLogin adminReq = new ReqAdminLogin();
        adminReq.setIdentifier("admin");
        adminReq.setPassword("secret");
        RespAdminLogin ok = service.login(adminReq, null);
        assertEquals(AdminAuthGuard.ADMIN_ROLE, ok.getRoleCode());
    }

    @Test
    void adminExposesNoOAuthOrSelfRegistrationControllers() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        for (BeanDefinition bd : scanner.findCandidateComponents("com.bgssai.shortai")) {
            String name = bd.getBeanClassName();
            String lower = name == null ? "" : name.toLowerCase();
            assertFalse(lower.contains("oauth"), "Admin must not expose OAuth controllers: " + name);
            assertFalse(lower.contains("register"), "Admin must not expose self-registration controllers: " + name);
            assertFalse(lower.contains("smslogin") || lower.contains("sendsms") || lower.contains("emaillogin"),
                    "Admin must not expose OTP login/registration controllers: " + name);
        }
    }
}
