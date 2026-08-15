package com.bgssai.shortai.subscription.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.UnauthorizedException;
import com.bgssai.shortai.mapper.CreditPackMapper;
import com.bgssai.shortai.mapper.PaymentOrderMapper;
import com.bgssai.shortai.mapper.SubscriptionPlanMapper;
import com.bgssai.shortai.model.CreditPack;
import com.bgssai.shortai.model.CreditPackExample;
import com.bgssai.shortai.model.PaymentOrder;
import com.bgssai.shortai.model.SubscriptionPlan;
import com.bgssai.shortai.model.SubscriptionPlanExample;
import com.bgssai.shortai.payment.channel.PaymentChannel;
import com.bgssai.shortai.payment.channel.PaymentChannelRouter;
import com.bgssai.shortai.payment.channel.PaymentChannels;
import com.bgssai.shortai.payment.channel.PaymentIntent;
import com.bgssai.shortai.payment.channel.PrepayResult;
import com.bgssai.shortai.payment.config.PaymentProperties;
import com.bgssai.shortai.payment.service.PaidOrderGrantService;

@Service
public class CreatePayOrderService {

    private static final Logger log = LoggerFactory.getLogger(CreatePayOrderService.class);

    @Autowired
    private SubscriptionPlanMapper subscriptionPlanMapper;
    @Autowired
    private CreditPackMapper creditPackMapper;
    @Autowired
    private PaymentOrderMapper paymentOrderMapper;
    @Autowired
    private PaymentChannelRouter paymentChannelRouter;
    @Autowired
    private PaymentProperties paymentProperties;
    @Autowired
    private PaidOrderGrantService paidOrderGrantService;

    @Transactional
    public ApiResponse createSubscription(String planCode, String paymentChannel) {
        String userId = requireUser();
        if (planCode == null || planCode.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "plan_code is required");
        }
        SubscriptionPlanExample ex = new SubscriptionPlanExample();
        ex.createCriteria().andPlanCodeEqualTo(planCode.trim()).andDeleteEqualTo(0).andActiveEqualTo(1);
        SubscriptionPlan plan = subscriptionPlanMapper.selectOneByExample(ex);
        if (plan == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "unknown plan_code");
        }
        int price = plan.getPriceCent() == null ? 0 : plan.getPriceCent();
        int grant = plan.getMonthlyBalanceCent() == null ? 0 : plan.getMonthlyBalanceCent();
        return place(userId, "subscription", plan.getPlanCode(), plan.getPlanName(), null,
                price, grant, paymentChannel);
    }

    @Transactional
    public ApiResponse createCreditPack(String packCode, String paymentChannel) {
        String userId = requireUser();
        if (packCode == null || packCode.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "pack_code is required");
        }
        CreditPackExample ex = new CreditPackExample();
        ex.createCriteria().andPackCodeEqualTo(packCode.trim()).andDeleteEqualTo(0).andActiveEqualTo(1);
        CreditPack pack = creditPackMapper.selectOneByExample(ex);
        if (pack == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "unknown pack_code");
        }
        int price = pack.getPriceCent() == null ? 0 : pack.getPriceCent();
        int grant = pack.getCreditCent() == null ? 0 : pack.getCreditCent();
        return place(userId, "credit_pack", pack.getPackCode(), pack.getName(), null,
                price, grant, paymentChannel);
    }

    private ApiResponse place(String userId, String orderType, String bizCode, String bizName,
                              String bizRefId, int amountCent, int grantCent, String paymentChannel) {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.MINUTE, paymentProperties.getOrderTimeoutMinutes());
        String orderNo = ("subscription".equals(orderType) ? "SUB" : "PKG")
                + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(now)
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        PaymentOrder row = new PaymentOrder();
        row.setOrderNo(orderNo);
        row.setUserId(userId);
        row.setOrderType(orderType);
        row.setBizCode(bizCode);
        row.setBizName(bizName);
        row.setBizRefId(bizRefId);
        row.setAmountCent(amountCent);
        row.setGrantCent(grantCent);
        row.setStatus("created");
        row.setExpiresAt(cal.getTime());
        row.setDelete(0);
        row.setCreator(userId);
        row.setModifier(userId);
        row.setGmtCreate(now);
        row.setGmtModified(now);
        if (amountCent <= 0) {
            row.setPaymentChannel(null);
            paymentOrderMapper.insertSelective(row);
            paidOrderGrantService.grantIfCreated(row, "FREE");
            row.setStatus("paid");
            log.info("zero-price order paid immediately orderNo={} type={}", orderNo, orderType);
            return ok(toResult(row, null));
        }
        String channel = PaymentChannels.normalize(paymentChannel);
        if (channel == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "payment_channel is required");
        }
        row.setPaymentChannel(channel);
        paymentOrderMapper.insertSelective(row);
        PaymentChannel payChannel = paymentChannelRouter.route(channel);
        PrepayResult prepay = payChannel.createPayment(
                new PaymentIntent(orderNo, amountCent, bizName), orderNo);
        PaymentOrder patch = new PaymentOrder();
        patch.setCodeUrl(prepay.getPrepayInfo());
        patch.setPayMerchantNo(prepay.getPayMerchantNo());
        patch.setGmtModified(new Date());
        com.bgssai.shortai.model.PaymentOrderExample up = new com.bgssai.shortai.model.PaymentOrderExample();
        up.createCriteria().andOrderNoEqualTo(orderNo).andDeleteEqualTo(0);
        paymentOrderMapper.updateByExampleSelective(patch, up);
        row.setCodeUrl(prepay.getPrepayInfo());
        row.setPayMerchantNo(prepay.getPayMerchantNo());
        log.info("pay order created orderNo={} type={} amountCent={} channel={}",
                orderNo, orderType, amountCent, channel);
        return ok(toResult(row, prepay.getPrepayInfo()));
    }

    private Map<String, Object> toResult(PaymentOrder row, String codeUrl) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order_no", row.getOrderNo());
        m.put("order_type", row.getOrderType());
        m.put("status", row.getStatus());
        m.put("plan_code", "subscription".equals(row.getOrderType()) ? row.getBizCode() : null);
        m.put("pack_code", "credit_pack".equals(row.getOrderType()) ? row.getBizCode() : null);
        m.put("amount_cent", row.getAmountCent());
        m.put("grant_cent", row.getGrantCent());
        m.put("payment_channel", row.getPaymentChannel());
        m.put("code_url", codeUrl != null ? codeUrl : row.getCodeUrl());
        m.put("pay_form", row.getPayForm());
        m.put("expires_at", row.getExpiresAt() == null ? null : fmt.format(row.getExpiresAt()));
        return m;
    }

    private static String requireUser() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new UnauthorizedException("no user in context");
        }
        return userId;
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
