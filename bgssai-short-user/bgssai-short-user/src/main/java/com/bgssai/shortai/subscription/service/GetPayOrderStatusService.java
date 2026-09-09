package com.bgssai.shortai.subscription.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.UnauthorizedException;
import com.bgssai.shortai.mapper.PaymentOrderMapper;
import com.bgssai.shortai.model.PaymentOrder;
import com.bgssai.shortai.model.PaymentOrderExample;
import com.bgssai.shortai.payment.channel.ChannelPaidQuery;
import com.bgssai.shortai.payment.channel.PaymentChannel;
import com.bgssai.shortai.payment.channel.PaymentChannelRouter;
import com.bgssai.shortai.payment.config.PaymentProperties;
import com.bgssai.shortai.payment.service.PaidOrderGrantService;
import com.bgssai.shortai.payment.service.PaymentAmountGuard;

@Service
public class GetPayOrderStatusService {

    private static final Logger log = LoggerFactory.getLogger(GetPayOrderStatusService.class);
    private static final long CHANNEL_QUERY_MIN_AGE_MS = 5000L;

    @Autowired private PaymentOrderMapper paymentOrderMapper;
    @Autowired private PaymentChannelRouter paymentChannelRouter;
    @Autowired private PaymentProperties paymentProperties;
    @Autowired private PaidOrderGrantService paidOrderGrantService;

    public ApiResponse query(String orderNo) {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new UnauthorizedException("no user in context");
        }
        if (orderNo == null || orderNo.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "order_no is required");
        }
        log.info("query pay order status userId={} orderNo={}", userId, orderNo.trim());
        PaymentOrder order = loadOwned(userId, orderNo.trim());
        if ("created".equals(order.getStatus()) && closeIfExpired(order)) {
            log.info("pay order closed by expiry on status query orderNo={} userId={}",
                    order.getOrderNo(), userId);
            return ok(toResult(order));
        }
        if ("created".equals(order.getStatus())
                && !paymentProperties.isSandboxEnabled()
                && order.getGmtCreate() != null
                && System.currentTimeMillis() - order.getGmtCreate().getTime() > CHANNEL_QUERY_MIN_AGE_MS) {
            try {
                PaymentChannel channel = paymentChannelRouter.route(order.getPaymentChannel());
                ChannelPaidQuery paid = channel.queryPaidOrder(order.getOrderNo());
                if (paid != null) {
                    if (!PaymentAmountGuard.matches(order.getAmountCent(), paid.getAmountCent())) {
                        log.warn("channel query amount mismatch orderNo={} orderFen={} channelFen={}",
                                order.getOrderNo(), order.getAmountCent(), paid.getAmountCent());
                    } else {
                        paidOrderGrantService.grantIfCreated(order, paid.getChannelTradeNo());
                        order = loadOwned(userId, order.getOrderNo());
                        log.info("pending pay order settled by channel query orderNo={} userId={}",
                                order.getOrderNo(), userId);
                    }
                }
            } catch (RuntimeException e) {
                log.warn("channel query compensation failed orderNo={} error={}",
                        order.getOrderNo(), e.getClass().getSimpleName());
            }
        }
        return ok(toResult(order));
    }

    private boolean closeIfExpired(PaymentOrder order) {
        if (order.getExpiresAt() == null || !order.getExpiresAt().before(new Date())) {
            return false;
        }
        Date now = new Date();
        PaymentOrder patch = new PaymentOrder();
        patch.setStatus("closed");
        patch.setGmtModified(now);
        PaymentOrderExample cas = new PaymentOrderExample();
        cas.createCriteria().andIdEqualTo(order.getId()).andStatusEqualTo("created").andDelFlagEqualTo(0);
        int n = paymentOrderMapper.updateByExampleSelective(patch, cas);
        if (n == 1) {
            order.setStatus("closed");
            return true;
        }
        PaymentOrder latest = loadOwned(order.getUserId(), order.getOrderNo());
        order.setStatus(latest.getStatus());
        return "closed".equals(order.getStatus());
    }

    private PaymentOrder loadOwned(String userId, String orderNo) {
        PaymentOrderExample ex = new PaymentOrderExample();
        ex.createCriteria().andOrderNoEqualTo(orderNo).andDelFlagEqualTo(0);
        PaymentOrder order = paymentOrderMapper.selectOneByExample(ex);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BizException(CommonErrorCode.RESOURCE_NOT_FOUND, "order not found");
        }
        return order;
    }

    private static Map<String, Object> toResult(PaymentOrder row) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order_no", row.getOrderNo());
        m.put("order_type", row.getOrderType());
        m.put("status", row.getStatus());
        m.put("amount_cent", row.getAmountCent());
        m.put("payment_channel", row.getPaymentChannel());
        m.put("expires_at", row.getExpiresAt() == null ? null : fmt.format(row.getExpiresAt()));
        return m;
    }

    private static ApiResponse ok(Object result) {
        ApiResponse resp = new ApiResponse();
        resp.setCode("0");
        resp.setSuccess(true);
        resp.setMessage("OK");
        resp.setResult(result);
        return resp;
    }
}
