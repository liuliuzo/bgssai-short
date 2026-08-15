package com.bgssai.shortai.quota.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.QuotaChangeLogMapper;
import com.bgssai.shortai.mapper.UserQuotaMapper;
import com.bgssai.shortai.model.UserQuota;

class QuotaConsumeServiceTest {

    private UserQuotaMapper userQuotaMapper;
    private QuotaWalletService quotaWalletService;
    private QuotaConsumeService service;

    @BeforeEach
    void setUp() {
        userQuotaMapper = org.mockito.Mockito.mock(UserQuotaMapper.class);
        quotaWalletService = org.mockito.Mockito.mock(QuotaWalletService.class);
        service = new QuotaConsumeService();
        ReflectionTestUtils.setField(service, "userQuotaMapper", userQuotaMapper);
        ReflectionTestUtils.setField(service, "quotaChangeLogMapper",
                org.mockito.Mockito.mock(QuotaChangeLogMapper.class));
        ReflectionTestUtils.setField(service, "quotaWalletService", quotaWalletService);
    }

    private UserQuota quota(int balance, int extra, int spent, int limit) {
        UserQuota q = new UserQuota();
        q.setId(1L);
        q.setBalanceCent(balance);
        q.setExtraBalanceCent(extra);
        q.setMonthSpentCent(spent);
        q.setMonthlyLimitCent(limit);
        q.setMonthResetDate(new java.util.Date());
        return q;
    }

    @Test
    void consumePeriodThenExtra() {
        UserQuota q = quota(30, 20, 0, 1000);
        when(quotaWalletService.requireWallet("u-1")).thenReturn(q);
        when(userQuotaMapper.updateByExampleSelective(any(), any())).thenReturn(1);
        service.consume("u-1", 40);
        assertEquals(30, q.getBalanceCent());
    }

    @Test
    void insufficientThrows4007() {
        when(quotaWalletService.requireWallet("u-1")).thenReturn(quota(10, 5, 0, 1000));
        BizException ex = assertThrows(BizException.class, () -> service.consume("u-1", 20));
        assertEquals("4007", ex.getCode());
    }

    @Test
    void monthCapThrows4015() {
        when(quotaWalletService.requireWallet("u-1")).thenReturn(quota(500, 500, 90, 100));
        BizException ex = assertThrows(BizException.class, () -> service.consume("u-1", 20));
        assertEquals("4015", ex.getCode());
    }
}
