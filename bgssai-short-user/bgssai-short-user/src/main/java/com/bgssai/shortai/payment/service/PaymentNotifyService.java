package com.bgssai.shortai.payment.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.PaymentOrderMapper;
import com.bgssai.shortai.model.PaymentOrder;
import com.bgssai.shortai.model.PaymentOrderExample;
import com.bgssai.shortai.payment.channel.NotifyContext;
import com.bgssai.shortai.payment.channel.NotifyResult;
import com.bgssai.shortai.payment.channel.PaymentChannel;
import com.bgssai.shortai.payment.channel.PaymentChannelRouter;

@Service
public class PaymentNotifyService {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotifyService.class);

    @Autowired
    private PaymentChannelRouter paymentChannelRouter;

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    @Autowired
    private PaidOrderGrantService paidOrderGrantService;

    public String applyNotify(String channelCode, NotifyContext context) {
        log.info("payment notify received channel={}", channelCode);
        PaymentChannel channel = paymentChannelRouter.route(channelCode);
        NotifyResult result = channel.verifyAndParseNotify(context);
        if (!result.isVerified()) {
            log.warn("payment notify verify failed channel={}", channelCode);
            throw new BizException("60005", "notify signature verify failed");
        }
        if (!result.isSuccess()) {
            log.info("payment notify non-success channel={} orderNo={}", channelCode, result.getOrderNo());
            return channelConfirm(channelCode);
        }
        PaymentOrderExample orderEx = new PaymentOrderExample();
        orderEx.createCriteria().andOrderNoEqualTo(result.getOrderNo()).andDelFlagEqualTo(0);
        PaymentOrder order = paymentOrderMapper.selectOneByExample(orderEx);
        if (order == null) {
            log.warn("payment notify order not found channel={} orderNo={}", channelCode, result.getOrderNo());
            return channelConfirm(channelCode);
        }
        if ("paid".equals(order.getStatus())) {
            log.info("payment notify already paid orderNo={}", order.getOrderNo());
            return channelConfirm(channelCode);
        }
        if ("closed".equals(order.getStatus())) {
            log.warn("payment notify ignored closed order orderNo={}", order.getOrderNo());
            return channelConfirm(channelCode);
        }
        if (closeIfExpired(order)) {
            log.warn("payment notify ignored expired order orderNo={}", order.getOrderNo());
            return channelConfirm(channelCode);
        }
        PaymentAmountGuard.requireMatch(order.getOrderNo(), order.getAmountCent(), result.getAmountCent());
        paidOrderGrantService.grantIfCreated(order, result.getChannelTradeNo());
        return channelConfirm(channelCode);
    }

    private boolean closeIfExpired(PaymentOrder order) {
        if (order.getExpiresAt() == null || !order.getExpiresAt().before(new Date())) {
            return false;
        }
        if (!"created".equals(order.getStatus())) {
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
        PaymentOrderExample reload = new PaymentOrderExample();
        reload.createCriteria().andIdEqualTo(order.getId()).andDelFlagEqualTo(0);
        PaymentOrder latest = paymentOrderMapper.selectOneByExample(reload);
        if (latest != null) {
            order.setStatus(latest.getStatus());
        }
        return "closed".equals(order.getStatus());
    }

    private String channelConfirm(String channelCode) {
        return "WECHAT".equals(channelCode) ? "{\"code\":\"SUCCESS\",\"message\":\"OK\"}" : "success";
    }
}
