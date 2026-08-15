package com.bgssai.shortai.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.mapper.PaymentOrderMapper;
import com.bgssai.shortai.mapper.QuotaChangeLogMapper;
import com.bgssai.shortai.mapper.SubscriptionPlanMapper;
import com.bgssai.shortai.mapper.UserQuotaMapper;
import com.bgssai.shortai.mapper.UserSubscriptionMapper;
import com.bgssai.shortai.model.PaymentOrder;
import com.bgssai.shortai.model.PaymentOrderExample;
import com.bgssai.shortai.model.UserQuota;
import com.bgssai.shortai.quota.service.QuotaWalletService;

class PaidOrderGrantServiceTest {

    private PaymentOrderMapper paymentOrderMapper;
    private UserQuotaMapper userQuotaMapper;
    private QuotaWalletService quotaWalletService;
    private PaidOrderGrantService service;

    @BeforeEach
    void setUp() {
        paymentOrderMapper = org.mockito.Mockito.mock(PaymentOrderMapper.class);
        userQuotaMapper = org.mockito.Mockito.mock(UserQuotaMapper.class);
        quotaWalletService = org.mockito.Mockito.mock(QuotaWalletService.class);
        service = new PaidOrderGrantService();
        ReflectionTestUtils.setField(service, "paymentOrderMapper", paymentOrderMapper);
        ReflectionTestUtils.setField(service, "userSubscriptionMapper",
                org.mockito.Mockito.mock(UserSubscriptionMapper.class));
        ReflectionTestUtils.setField(service, "userQuotaMapper", userQuotaMapper);
        ReflectionTestUtils.setField(service, "subscriptionPlanMapper",
                org.mockito.Mockito.mock(SubscriptionPlanMapper.class));
        ReflectionTestUtils.setField(service, "quotaChangeLogMapper",
                org.mockito.Mockito.mock(QuotaChangeLogMapper.class));
        ReflectionTestUtils.setField(service, "quotaWalletService", quotaWalletService);
    }

    @Test
    void alreadyPaidIsIdempotent() {
        PaymentOrder order = new PaymentOrder();
        order.setId(1L);
        order.setOrderNo("O-1");
        order.setStatus("paid");
        service.grantIfCreated(order, "tx");
        verify(paymentOrderMapper, never()).updateByExampleSelective(any(PaymentOrder.class), any(PaymentOrderExample.class));
    }

    @Test
    void creditPackAddsExtraBalance() {
        PaymentOrder order = new PaymentOrder();
        order.setId(2L);
        order.setOrderNo("PKG-1");
        order.setUserId("u-1");
        order.setOrderType("credit_pack");
        order.setStatus("created");
        order.setGrantCent(5000);
        when(paymentOrderMapper.updateByExampleSelective(any(PaymentOrder.class), any(PaymentOrderExample.class)))
                .thenReturn(1);
        UserQuota quota = new UserQuota();
        quota.setId(9L);
        quota.setBalanceCent(100);
        quota.setExtraBalanceCent(200);
        when(quotaWalletService.requireWallet("u-1")).thenReturn(quota);
        when(userQuotaMapper.updateByExampleSelective(any(UserQuota.class), any())).thenReturn(1);
        service.grantIfCreated(order, "tx-1");
        verify(userQuotaMapper).updateByExampleSelective(any(UserQuota.class), any());
        assertEquals("credit_pack", order.getOrderType());
    }
}
