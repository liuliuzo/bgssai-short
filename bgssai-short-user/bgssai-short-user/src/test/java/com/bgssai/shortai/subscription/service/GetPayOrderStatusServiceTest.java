package com.bgssai.shortai.subscription.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.PaymentOrderMapper;
import com.bgssai.shortai.model.PaymentOrder;
import com.bgssai.shortai.model.PaymentOrderExample;
import com.bgssai.shortai.payment.channel.ChannelPaidQuery;
import com.bgssai.shortai.payment.channel.PaymentChannel;
import com.bgssai.shortai.payment.channel.PaymentChannelRouter;
import com.bgssai.shortai.payment.service.PaidOrderGrantService;

class GetPayOrderStatusServiceTest {

    private PaymentOrderMapper paymentOrderMapper;
    private PaymentChannel channel;
    private PaidOrderGrantService paidOrderGrantService;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void rejectsUnknownOrder() {
        GetPayOrderStatusService service = newService(null);

        BizException ex = assertThrows(BizException.class, () -> service.query("po-001"));

        assertEquals("404000", ex.getCode());
    }

    @Test
    void rejectsWrongOwner() {
        PaymentOrder order = createdOrder();
        order.setUserId("other");
        GetPayOrderStatusService service = newService(order);

        BizException ex = assertThrows(BizException.class, () -> service.query("po-001"));

        assertEquals("404000", ex.getCode());
    }

    @Test
    void channelQueryCompensationSettlesPaidCreatedOrder() {
        PaymentOrder order = agedCreatedOrder();
        GetPayOrderStatusService service = newService(order);
        when(channel.queryPaidOrder("po-001")).thenReturn(ChannelPaidQuery.of("4200000000000000001", 9900));
        doAnswer(invocation -> {
            PaymentOrder arg = invocation.getArgument(0);
            arg.setStatus("paid");
            return null;
        }).when(paidOrderGrantService).grantIfCreated(any(PaymentOrder.class), eq("4200000000000000001"));

        ApiResponse resp = service.query("po-001");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();
        assertEquals("paid", result.get("status"));
        verify(paidOrderGrantService).grantIfCreated(any(PaymentOrder.class), eq("4200000000000000001"));
    }

    @Test
    void channelQueryAmountMismatchDoesNotGrant() {
        PaymentOrder order = agedCreatedOrder();
        GetPayOrderStatusService service = newService(order);
        when(channel.queryPaidOrder("po-001")).thenReturn(ChannelPaidQuery.of("4200000000000000001", 1));

        ApiResponse resp = service.query("po-001");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();
        assertEquals("created", result.get("status"));
        verify(paidOrderGrantService, never()).grantIfCreated(any(PaymentOrder.class), anyString());
    }

    @Test
    void channelQueryReturningNullKeepsOrderCreated() {
        PaymentOrder order = agedCreatedOrder();
        GetPayOrderStatusService service = newService(order);
        when(channel.queryPaidOrder("po-001")).thenReturn(null);

        ApiResponse resp = service.query("po-001");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();
        assertEquals("created", result.get("status"));
        verify(paidOrderGrantService, never()).grantIfCreated(any(PaymentOrder.class), anyString());
    }

    @Test
    void expiredCreatedOrderIsClosedWithoutChannelQuery() {
        PaymentOrder order = agedCreatedOrder();
        order.setExpiresAt(new Date(System.currentTimeMillis() - 60_000L));
        GetPayOrderStatusService service = newService(order);
        when(paymentOrderMapper.updateByExampleSelective(any(PaymentOrder.class), any(PaymentOrderExample.class)))
                .thenReturn(1);

        ApiResponse resp = service.query("po-001");

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();
        assertEquals("closed", result.get("status"));
        verify(channel, never()).queryPaidOrder(anyString());
        verify(paidOrderGrantService, never()).grantIfCreated(any(PaymentOrder.class), anyString());
    }

    private GetPayOrderStatusService newService(PaymentOrder order) {
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        channel = mock(PaymentChannel.class);
        PaymentChannelRouter router = mock(PaymentChannelRouter.class);
        paidOrderGrantService = mock(PaidOrderGrantService.class);
        when(paymentOrderMapper.selectOneByExample(any(PaymentOrderExample.class))).thenReturn(order);
        when(router.route(anyString())).thenReturn(channel);

        GetPayOrderStatusService service = new GetPayOrderStatusService();
        ReflectionTestUtils.setField(service, "paymentOrderMapper", paymentOrderMapper);
        ReflectionTestUtils.setField(service, "paymentChannelRouter", router);
        ReflectionTestUtils.setField(service, "paidOrderGrantService", paidOrderGrantService);
        UserContext.setUserId("user-001");
        return service;
    }

    private static PaymentOrder createdOrder() {
        PaymentOrder order = new PaymentOrder();
        order.setId(1L);
        order.setOrderNo("po-001");
        order.setUserId("user-001");
        order.setOrderType("subscription");
        order.setStatus("created");
        order.setPaymentChannel("WECHAT");
        order.setAmountCent(9900);
        order.setDelFlag(0);
        order.setGmtCreate(new Date());
        return order;
    }

    private static PaymentOrder agedCreatedOrder() {
        PaymentOrder order = createdOrder();
        order.setGmtCreate(new Date(System.currentTimeMillis() - 60_000L));
        return order;
    }
}
