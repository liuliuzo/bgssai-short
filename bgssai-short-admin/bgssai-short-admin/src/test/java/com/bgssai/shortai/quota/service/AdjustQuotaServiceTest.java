package com.bgssai.shortai.quota.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.AdminAuthGuard;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.QuotaChangeLogMapper;
import com.bgssai.shortai.mapper.UserQuotaMapper;
import com.bgssai.shortai.quota.dto.ReqAdjustQuota;

class AdjustQuotaServiceTest {

    @Test
    void remarkRequired() {
        AdjustQuotaService service = new AdjustQuotaService();
        ReflectionTestUtils.setField(service, "userQuotaMapper",
                org.mockito.Mockito.mock(UserQuotaMapper.class));
        ReflectionTestUtils.setField(service, "quotaChangeLogMapper",
                org.mockito.Mockito.mock(QuotaChangeLogMapper.class));
        ReqAdjustQuota req = new ReqAdjustQuota();
        req.setUserId("u-1");
        req.setBalanceDeltaCent(100);
        try (MockedStatic<AdminAuthGuard> mocked = mockStatic(AdminAuthGuard.class)) {
            mocked.when(AdminAuthGuard::isAdmin).thenReturn(true);
            BizException ex = assertThrows(BizException.class, () -> service.adjust(req));
            assertEquals("50000001", ex.getCode());
        }
    }
}
