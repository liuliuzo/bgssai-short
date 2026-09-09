package com.bgssai.shortai.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.PaymentOrderMapper;
import com.bgssai.shortai.model.PaymentOrder;
import com.bgssai.shortai.model.PaymentOrderExample;
import com.bgssai.shortai.payment.channel.NotifyContext;
import com.bgssai.shortai.payment.channel.NotifyResult;
import com.bgssai.shortai.payment.channel.PaymentChannel;
import com.bgssai.shortai.payment.channel.PaymentChannelRouter;

class PaymentNotifyServiceTest {

    private PaymentChannelRouter router;
    private PaymentChannel channel;
    private PaymentOrderMapper paymentOrderMapper;
    private PaidOrderGrantService paidOrderGrantService;
    private PaymentNotifyService service;

    @BeforeEach
    void setUp() {
        router = mock(PaymentChannelRouter.class);
        channel = mock(PaymentChannel.class);
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        paidOrderGrantService = mock(PaidOrderGrantService.class);
        when(router.route("WECHAT")).thenReturn(channel);
        when(router.route("ALIPAY")).thenReturn(channel);
        service = new PaymentNotifyService();
        ReflectionTestUtils.setField(service, "paymentChannelRouter", router);
        ReflectionTestUtils.setField(service, "paymentOrderMapper", paymentOrderMapper);
        ReflectionTestUtils.setField(service, "paidOrderGrantService", paidOrderGrantService);
    }

    @Test
    void verifyFailureThrows60005() {
        when(channel.verifyAndParseNotify(any(NotifyContext.class))).thenReturn(NotifyResult.rejected());

        BizException ex = assertThrows(BizException.class,
                () -> service.applyNotify("WECHAT", NotifyContext.ofBody("{}", null)));

        assertEquals("60005", ex.getCode());
        verify(paidOrderGrantService, never()).grantIfCreated(any(), any());
    }

    @Test
    void amountMismatchThrows60005() {
        when(channel.verifyAndParseNotify(any(NotifyContext.class)))
                .thenReturn(new NotifyResult(true, true, "po-1", "tx-1", 1));
        PaymentOrder order = createdOrder(9900);
        when(paymentOrderMapper.selectOneByExample(any(PaymentOrderExample.class))).thenReturn(order);

        BizException ex = assertThrows(BizException.class,
                () -> service.applyNotify("WECHAT", NotifyContext.ofBody("{}", null)));

        assertEquals("60005", ex.getCode());
        verify(paidOrderGrantService, never()).grantIfCreated(any(), any());
    }

    @Test
    void nullAmountThrows60005() {
        when(channel.verifyAndParseNotify(any(NotifyContext.class)))
                .thenReturn(new NotifyResult(true, true, "po-1", "tx-1", null));
        PaymentOrder order = createdOrder(9900);
        when(paymentOrderMapper.selectOneByExample(any(PaymentOrderExample.class))).thenReturn(order);

        BizException ex = assertThrows(BizException.class,
                () -> service.applyNotify("ALIPAY", NotifyContext.ofForm(java.util.Map.of(), null)));

        assertEquals("60005", ex.getCode());
        verify(paidOrderGrantService, never()).grantIfCreated(any(), any());
    }

    @Test
    void matchingAmountGrants() {
        when(channel.verifyAndParseNotify(any(NotifyContext.class)))
                .thenReturn(new NotifyResult(true, true, "po-1", "tx-1", 9900));
        PaymentOrder order = createdOrder(9900);
        when(paymentOrderMapper.selectOneByExample(any(PaymentOrderExample.class))).thenReturn(order);

        String ack = service.applyNotify("WECHAT", NotifyContext.ofBody("{}", null));

        assertEquals("{\"code\":\"SUCCESS\",\"message\":\"OK\"}", ack);
        verify(paidOrderGrantService).grantIfCreated(eq(order), eq("tx-1"));
    }

    @Test
    void closedOrderAcksWithoutGrant() {
        when(channel.verifyAndParseNotify(any(NotifyContext.class)))
                .thenReturn(new NotifyResult(true, true, "po-1", "tx-1", 9900));
        PaymentOrder order = createdOrder(9900);
        order.setStatus("closed");
        when(paymentOrderMapper.selectOneByExample(any(PaymentOrderExample.class))).thenReturn(order);

        String ack = service.applyNotify("ALIPAY", NotifyContext.ofForm(java.util.Map.of(), null));

        assertEquals("success", ack);
        verify(paidOrderGrantService, never()).grantIfCreated(any(), any());
    }

    @Test
    void expiredOrderClosedWithoutGrant() {
        when(channel.verifyAndParseNotify(any(NotifyContext.class)))
                .thenReturn(new NotifyResult(true, true, "po-1", "tx-1", 9900));
        PaymentOrder order = createdOrder(9900);
        order.setExpiresAt(new Date(System.currentTimeMillis() - 60_000L));
        when(paymentOrderMapper.selectOneByExample(any(PaymentOrderExample.class))).thenReturn(order);
        when(paymentOrderMapper.updateByExampleSelective(any(PaymentOrder.class), any(PaymentOrderExample.class)))
                .thenReturn(1);

        String ack = service.applyNotify("WECHAT", NotifyContext.ofBody("{}", null));

        assertEquals("{\"code\":\"SUCCESS\",\"message\":\"OK\"}", ack);
        assertEquals("closed", order.getStatus());
        verify(paidOrderGrantService, never()).grantIfCreated(any(), any());
    }

    private static PaymentOrder createdOrder(int amountCent) {
        PaymentOrder order = new PaymentOrder();
        order.setId(1L);
        order.setOrderNo("po-1");
        order.setStatus("created");
        order.setAmountCent(amountCent);
        order.setDelFlag(0);
        order.setExpiresAt(new Date(System.currentTimeMillis() + 600_000L));
        return order;
    }
}
