package com.bgssai.shortai.quota.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.QuotaChangeLogMapper;
import com.bgssai.shortai.mapper.UserQuotaMapper;
import com.bgssai.shortai.model.QuotaChangeLog;
import com.bgssai.shortai.model.UserQuota;
import com.bgssai.shortai.model.UserQuotaExample;

@Service
public class QuotaConsumeService {

    private static final Logger log = LoggerFactory.getLogger(QuotaConsumeService.class);

    @Autowired
    private UserQuotaMapper userQuotaMapper;

    @Autowired
    private QuotaChangeLogMapper quotaChangeLogMapper;

    @Autowired
    private QuotaWalletService quotaWalletService;

    @Autowired
    private ShortDebitOutbox debitOutbox;

    @Transactional
    public void consume(String userId, int costCent) {
        consume(userId, costCent, "quota:" + java.util.UUID.randomUUID());
    }

    @Transactional
    public void consume(String userId, int costCent, String settlementId) {
        if (costCent <= 0) {
            return;
        }
        UserQuota quota = quotaWalletService.requireWallet(userId);
        quotaWalletService.resetMonthIfNeeded(quota);
        int limit = quota.getMonthlyLimitCent() == null ? 0 : quota.getMonthlyLimitCent();
        int spent = quota.getMonthSpentCent() == null ? 0 : quota.getMonthSpentCent();
        if (limit > 0 && (long) spent + costCent > limit) {
            log.warn("quota month cap userId={} cost={} spent={} limit={}", userId, costCent, spent, limit);
            throw new BizException(CommonErrorCode.MONTHLY_LIMIT);
        }
        int balance = quota.getBalanceCent() == null ? 0 : quota.getBalanceCent();
        int extra = quota.getExtraBalanceCent() == null ? 0 : quota.getExtraBalanceCent();
        if ((long) balance + extra < costCent) {
            log.warn("quota insufficient userId={} cost={} available={}", userId, costCent, balance + extra);
            throw new BizException(CommonErrorCode.QUOTA_INSUFFICIENT);
        }
        int fromBalance = Math.min(balance, costCent);
        int fromExtra = costCent - fromBalance;
        UserQuota patch = new UserQuota();
        patch.setBalanceCent(balance - fromBalance);
        patch.setExtraBalanceCent(extra - fromExtra);
        patch.setMonthSpentCent(spent + costCent);
        patch.setGmtModified(new Date());
        UserQuotaExample cas = new UserQuotaExample();
        cas.createCriteria()
                .andIdEqualTo(quota.getId())
                .andDelFlagEqualTo(0)
                .andBalanceCentEqualTo(balance)
                .andExtraBalanceCentEqualTo(extra);
        int n = userQuotaMapper.updateByExampleSelective(patch, cas);
        if (n != 1) {
            throw new BizException(CommonErrorCode.BIZ_ERROR, "quota concurrent update, retry");
        }
        QuotaChangeLog logRow = new QuotaChangeLog();
        logRow.setUserId(userId);
        logRow.setChangeType("consume");
        logRow.setBalanceDeltaCent(-fromBalance);
        logRow.setExtraDeltaCent(-fromExtra);
        logRow.setBalanceAfterCent(patch.getBalanceCent());
        logRow.setExtraAfterCent(patch.getExtraBalanceCent());
        logRow.setMonthlyLimitCent(limit);
        logRow.setRemark("task success");
        logRow.setDelFlag(0);
        logRow.setCreator(userId);
        quotaChangeLogMapper.insertSelective(logRow);
        log.info("quota consumed userId={} cost={} balanceAfter={} extraAfter={}",
                userId, costCent, patch.getBalanceCent(), patch.getExtraBalanceCent());
        debitOutbox.enqueue(settlementId, userId, costCent);
    }
}
