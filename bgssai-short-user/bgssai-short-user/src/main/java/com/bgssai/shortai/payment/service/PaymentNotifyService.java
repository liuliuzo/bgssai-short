package com.bgssai.shortai.payment.service;

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
        orderEx.createCriteria().andOrderNoEqualTo(result.getOrderNo()).andDeleteEqualTo(0);
        PaymentOrder order = paymentOrderMapper.selectOneByExample(orderEx);
        if (order == null) {
            log.warn("payment notify order not found channel={} orderNo={}", channelCode, result.getOrderNo());
            return channelConfirm(channelCode);
        }
        if (result.getAmountCent() != null && order.getAmountCent() != null
                && !result.getAmountCent().equals(order.getAmountCent())) {
            log.warn("payment notify amount mismatch orderNo={} notifyFen={} orderFen={}",
                    order.getOrderNo(), result.getAmountCent(), order.getAmountCent());
            throw new BizException("60005", "notify amount mismatch");
        }
        paidOrderGrantService.grantIfCreated(order, result.getChannelTradeNo());
        return channelConfirm(channelCode);
    }

    private String channelConfirm(String channelCode) {
        return "WECHAT".equals(channelCode) ? "{\"code\":\"SUCCESS\",\"message\":\"OK\"}" : "success";
    }
}
