package com.bgssai.shortai.subscription.service;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.UnauthorizedException;
import com.bgssai.shortai.login.oauth.BgssaiWebsiteLedgerClient;
import com.bgssai.shortai.mapper.SubscriptionPlanMapper;
import com.bgssai.shortai.mapper.UserSubscriptionMapper;
import com.bgssai.shortai.model.SubscriptionPlan;
import com.bgssai.shortai.model.SubscriptionPlanExample;
import com.bgssai.shortai.model.UserQuota;
import com.bgssai.shortai.model.UserSubscription;
import com.bgssai.shortai.model.UserSubscriptionExample;
import com.bgssai.shortai.payment.service.PaidOrderGrantService;
import com.bgssai.shortai.quota.service.QuotaWalletService;

@Service
public class GetMySubscriptionService {

    @Autowired
    private UserSubscriptionMapper userSubscriptionMapper;
    @Autowired
    private QuotaWalletService quotaWalletService;
    @Autowired
    private PaidOrderGrantService paidOrderGrantService;
    @Autowired
    private SubscriptionPlanMapper subscriptionPlanMapper;
    @Autowired(required = false)
    private BgssaiWebsiteLedgerClient websiteLedgerClient;

    public ApiResponse me() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new UnauthorizedException("no user in context");
        }
        UserSubscriptionExample ex = new UserSubscriptionExample();
        ex.createCriteria().andUserIdEqualTo(userId).andDelFlagEqualTo(0);
        UserSubscription sub = userSubscriptionMapper.selectOneByExample(ex);
        if (sub == null) {
            int grant = 10000;
            SubscriptionPlanExample pex = new SubscriptionPlanExample();
            pex.createCriteria().andPlanCodeEqualTo("FREE").andDelFlagEqualTo(0);
            SubscriptionPlan plan = subscriptionPlanMapper.selectOneByExample(pex);
            if (plan != null && plan.getMonthlyBalanceCent() != null) {
                grant = plan.getMonthlyBalanceCent();
            }
            paidOrderGrantService.grantSubscription(userId, "FREE", grant);
            sub = userSubscriptionMapper.selectOneByExample(ex);
        }
        UserQuota quota = quotaWalletService.requireWallet(userId);
        quotaWalletService.resetMonthIfNeeded(quota);
        int balance = quota.getBalanceCent() == null ? 0 : quota.getBalanceCent();
        int extra = quota.getExtraBalanceCent() == null ? 0 : quota.getExtraBalanceCent();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> result = new LinkedHashMap<>();
        String planCode = sub == null ? "FREE" : sub.getPlanCode();
        boolean entitled = websiteLedgerClient != null && websiteLedgerClient.isEntitled(userId);
        if ("FREE".equals(planCode) && entitled) {
            planCode = "PRO";
        }
        result.put("plan_code", planCode);
        result.put("entitled", entitled);
        result.put("status", sub == null ? "active" : sub.getStatus());
        result.put("current_period_start", sub == null || sub.getCurrentPeriodStart() == null
                ? null : fmt.format(sub.getCurrentPeriodStart()));
        result.put("current_period_end", sub == null || sub.getCurrentPeriodEnd() == null
                ? null : fmt.format(sub.getCurrentPeriodEnd()));
        result.put("balance_cent", balance);
        result.put("extra_balance_cent", extra);
        result.put("monthly_limit_cent", quota.getMonthlyLimitCent());
        result.put("month_spent_cent", quota.getMonthSpentCent());
        result.put("available_cent", balance + extra);
        ApiResponse resp = new ApiResponse();
        resp.setCode("0");
        resp.setSuccess(true);
        resp.setMessage("OK");
        resp.setResult(result);
        return resp;
    }
}
