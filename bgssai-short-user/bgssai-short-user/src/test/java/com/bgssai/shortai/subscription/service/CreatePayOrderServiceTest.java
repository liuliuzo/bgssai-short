package com.bgssai.shortai.subscription.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.mapper.CreditPackMapper;
import com.bgssai.shortai.mapper.PaymentOrderMapper;
import com.bgssai.shortai.mapper.SubscriptionPlanMapper;
import com.bgssai.shortai.model.SubscriptionPlan;
import com.bgssai.shortai.payment.config.PaymentProperties;
import com.bgssai.shortai.payment.service.PaidOrderGrantService;

class CreatePayOrderServiceTest {

    @Test
    void usesServerCatalogPriceNotClient() {
        CreatePayOrderService service = new CreatePayOrderService();
        SubscriptionPlanMapper planMapper = org.mockito.Mockito.mock(SubscriptionPlanMapper.class);
        PaymentOrderMapper orderMapper = org.mockito.Mockito.mock(PaymentOrderMapper.class);
        PaidOrderGrantService grant = org.mockito.Mockito.mock(PaidOrderGrantService.class);
        PaymentProperties props = org.mockito.Mockito.mock(PaymentProperties.class);
        when(props.getOrderTimeoutMinutes()).thenReturn(30);
        ReflectionTestUtils.setField(service, "subscriptionPlanMapper", planMapper);
        ReflectionTestUtils.setField(service, "creditPackMapper",
                org.mockito.Mockito.mock(CreditPackMapper.class));
        ReflectionTestUtils.setField(service, "paymentOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "paymentChannelRouter", null);
        ReflectionTestUtils.setField(service, "paymentProperties", props);
        ReflectionTestUtils.setField(service, "paidOrderGrantService", grant);
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setPlanCode("FREE");
        plan.setPlanName("免费");
        plan.setPriceCent(0);
        plan.setMonthlyBalanceCent(10000);
        when(planMapper.selectOneByExample(any())).thenReturn(plan);
        when(orderMapper.insertSelective(any())).thenReturn(1);
        try (MockedStatic<UserContext> mocked = mockStatic(UserContext.class)) {
            mocked.when(UserContext::getUserId).thenReturn("u-1");
            ApiResponse resp = service.createSubscription("FREE", "WECHAT");
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> result = (java.util.Map<String, Object>) resp.getResult();
            assertEquals(0, result.get("amount_cent"));
            assertEquals(10000, result.get("grant_cent"));
        }
    }
}
