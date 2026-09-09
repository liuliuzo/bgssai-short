package com.bgssai.shortai.payment.service;

import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.centralledger.dto.OwnSubscriptionReport;
import com.bgssai.shortai.centralledger.service.CentralLedgerService;
import com.bgssai.shortai.login.oauth.BgssaiWebsiteLedgerClient;
import com.bgssai.shortai.mapper.PaymentOrderMapper;
import com.bgssai.shortai.mapper.QuotaChangeLogMapper;
import com.bgssai.shortai.mapper.SubscriptionPlanMapper;
import com.bgssai.shortai.mapper.UserQuotaMapper;
import com.bgssai.shortai.mapper.UserSubscriptionMapper;
import com.bgssai.shortai.model.PaymentOrder;
import com.bgssai.shortai.model.PaymentOrderExample;
import com.bgssai.shortai.model.QuotaChangeLog;
import com.bgssai.shortai.model.SubscriptionPlan;
import com.bgssai.shortai.model.SubscriptionPlanExample;
import com.bgssai.shortai.model.UserQuota;
import com.bgssai.shortai.model.UserQuotaExample;
import com.bgssai.shortai.model.UserSubscription;
import com.bgssai.shortai.model.UserSubscriptionExample;
import com.bgssai.shortai.quota.service.QuotaWalletService;

@Service
public class PaidOrderGrantService {

    private static final Logger log = LoggerFactory.getLogger(PaidOrderGrantService.class);

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;
    @Autowired
    private UserSubscriptionMapper userSubscriptionMapper;
    @Autowired
    private UserQuotaMapper userQuotaMapper;
    @Autowired
    private SubscriptionPlanMapper subscriptionPlanMapper;
    @Autowired
    private QuotaChangeLogMapper quotaChangeLogMapper;
    @Autowired
    private QuotaWalletService quotaWalletService;
    /** 中央账本直连；单测不装配时为空，跳过上报。 */
    @Autowired(required = false)
    private BgssaiWebsiteLedgerClient websiteLedgerClient;

    @Transactional
    public void grantIfCreated(PaymentOrder order, String channelTradeNo) {
        if (order == null) {
            return;
        }
        if ("paid".equals(order.getStatus())) {
            log.info("payment already paid orderNo={}", order.getOrderNo());
            return;
        }
        if ("closed".equals(order.getStatus())) {
            log.warn("payment grant refused closed orderNo={}", order.getOrderNo());
            return;
        }
        Date now = new Date();
        if (order.getExpiresAt() != null && order.getExpiresAt().before(now)) {
            log.warn("payment grant refused expired orderNo={}", order.getOrderNo());
            PaymentOrder closePatch = new PaymentOrder();
            closePatch.setStatus("closed");
            closePatch.setGmtModified(now);
            PaymentOrderExample closeCas = new PaymentOrderExample();
            closeCas.createCriteria().andIdEqualTo(order.getId()).andStatusEqualTo("created").andDelFlagEqualTo(0);
            paymentOrderMapper.updateByExampleSelective(closePatch, closeCas);
            order.setStatus("closed");
            return;
        }
        PaymentOrder patch = new PaymentOrder();
        patch.setStatus("paid");
        patch.setChannelTradeNo(channelTradeNo);
        patch.setPaidAt(now);
        patch.setGmtModified(now);
        PaymentOrderExample cas = new PaymentOrderExample();
        cas.createCriteria().andIdEqualTo(order.getId()).andStatusEqualTo("created").andDelFlagEqualTo(0);
        int n = paymentOrderMapper.updateByExampleSelective(patch, cas);
        if (n != 1) {
            log.info("payment grant skip concurrent or already paid orderNo={}", order.getOrderNo());
            return;
        }
        if ("subscription".equals(order.getOrderType())) {
            grantSubscription(order.getUserId(), order.getBizCode(), order.getGrantCent());
            reportToCentralLedger(order);
        } else if ("credit_pack".equals(order.getOrderType())) {
            grantPack(order.getUserId(), order.getGrantCent());
        }
        log.info("payment granted orderNo={} type={} grantCent={}",
                order.getOrderNo(), order.getOrderType(), order.getGrantCent());
    }

    /** 本地订阅发放完成后，以独立事务同步官网中央账本；失败不回滚本地发放。 */
    private void reportToCentralLedger(PaymentOrder order) {
        if (websiteLedgerClient == null) {
            return;
        }
        UserSubscriptionExample ex = new UserSubscriptionExample();
        ex.createCriteria().andUserIdEqualTo(order.getUserId()).andDelFlagEqualTo(0);
        UserSubscription current = userSubscriptionMapper.selectOneByExample(ex);
        SubscriptionPlanExample pex = new SubscriptionPlanExample();
        pex.createCriteria().andPlanCodeEqualTo(order.getBizCode()).andDelFlagEqualTo(0);
        SubscriptionPlan plan = subscriptionPlanMapper.selectOneByExample(pex);
        OwnSubscriptionReport report = new OwnSubscriptionReport();
        report.setExternalOrderNo(order.getOrderNo());
        report.setPlanCode(order.getBizCode());
        report.setPlanName(plan == null ? order.getBizCode() : plan.getPlanName());
        report.setAmountCent(order.getAmountCent());
        report.setStatus(CentralLedgerService.SUB_ACTIVE);
        report.setPeriodStart(current == null ? null : current.getCurrentPeriodStart());
        report.setPeriodEnd(current == null ? null : current.getCurrentPeriodEnd());
        report.setRemark("grantCent=" + order.getGrantCent());
        websiteLedgerClient.reportOwnSubscription(order.getUserId(), report);
    }

    public void grantSubscription(String userId, String planCode, Integer grantCent) {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.MONTH, 1);
        Date end = cal.getTime();
        UserSubscriptionExample ex = new UserSubscriptionExample();
        ex.createCriteria().andUserIdEqualTo(userId).andDelFlagEqualTo(0);
        UserSubscription existing = userSubscriptionMapper.selectOneByExample(ex);
        if (existing == null) {
            UserSubscription row = new UserSubscription();
            row.setUserId(userId);
            row.setPlanCode(planCode);
            row.setStatus("active");
            row.setCurrentPeriodStart(now);
            row.setCurrentPeriodEnd(end);
            row.setSource("order");
            row.setDelFlag(0);
            row.setCreator(userId);
            row.setModifier(userId);
            row.setGmtCreate(now);
            row.setGmtModified(now);
            userSubscriptionMapper.insertSelective(row);
        } else {
            UserSubscription patch = new UserSubscription();
            patch.setPlanCode(planCode);
            patch.setStatus("active");
            patch.setCurrentPeriodStart(now);
            patch.setCurrentPeriodEnd(end);
            patch.setSource("order");
            patch.setGmtModified(now);
            UserSubscriptionExample up = new UserSubscriptionExample();
            up.createCriteria().andIdEqualTo(existing.getId()).andDelFlagEqualTo(0);
            userSubscriptionMapper.updateByExampleSelective(patch, up);
        }
        int monthlyLimit = 0;
        SubscriptionPlanExample pex = new SubscriptionPlanExample();
        pex.createCriteria().andPlanCodeEqualTo(planCode).andDelFlagEqualTo(0);
        SubscriptionPlan plan = subscriptionPlanMapper.selectOneByExample(pex);
        if (plan != null && plan.getMonthlyLimitCent() != null) {
            monthlyLimit = plan.getMonthlyLimitCent();
        }
        int grant = grantCent == null ? 0 : grantCent;
        UserQuota quota = quotaWalletService.requireWallet(userId);
        UserQuota qPatch = new UserQuota();
        qPatch.setBalanceCent(grant);
        qPatch.setMonthlyLimitCent(monthlyLimit);
        qPatch.setMonthSpentCent(0);
        qPatch.setMonthResetDate(now);
        qPatch.setGmtModified(now);
        UserQuotaExample qex = new UserQuotaExample();
        qex.createCriteria().andIdEqualTo(quota.getId()).andDelFlagEqualTo(0);
        userQuotaMapper.updateByExampleSelective(qPatch, qex);
        insertLog(userId, "grant_plan", grant - nz(quota.getBalanceCent()), 0,
                grant, nz(quota.getExtraBalanceCent()), monthlyLimit, "plan " + planCode);
    }

    public void grantPack(String userId, Integer grantCent) {
        int add = grantCent == null ? 0 : grantCent;
        UserQuota quota = quotaWalletService.requireWallet(userId);
        int extra = nz(quota.getExtraBalanceCent()) + add;
        UserQuota qPatch = new UserQuota();
        qPatch.setExtraBalanceCent(extra);
        qPatch.setGmtModified(new Date());
        UserQuotaExample qex = new UserQuotaExample();
        qex.createCriteria().andIdEqualTo(quota.getId()).andDelFlagEqualTo(0);
        userQuotaMapper.updateByExampleSelective(qPatch, qex);
        insertLog(userId, "grant_pack", 0, add, nz(quota.getBalanceCent()), extra,
                quota.getMonthlyLimitCent(), "credit pack");
    }

    private void insertLog(String userId, String type, int balDelta, int extraDelta,
                           Integer balAfter, Integer extraAfter, Integer limit, String remark) {
        QuotaChangeLog row = new QuotaChangeLog();
        row.setUserId(userId);
        row.setChangeType(type);
        row.setBalanceDeltaCent(balDelta);
        row.setExtraDeltaCent(extraDelta);
        row.setBalanceAfterCent(balAfter);
        row.setExtraAfterCent(extraAfter);
        row.setMonthlyLimitCent(limit);
        row.setRemark(remark);
        row.setDelFlag(0);
        row.setCreator(userId);
        quotaChangeLogMapper.insertSelective(row);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
