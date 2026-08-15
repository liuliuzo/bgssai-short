package com.bgssai.shortai.quota.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.common.AdminAuthGuard;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.QuotaChangeLogMapper;
import com.bgssai.shortai.mapper.UserQuotaMapper;
import com.bgssai.shortai.model.QuotaChangeLog;
import com.bgssai.shortai.model.UserQuota;
import com.bgssai.shortai.model.UserQuotaExample;
import com.bgssai.shortai.quota.dto.ReqAdjustQuota;

@Service
public class AdjustQuotaService {

    private static final Logger log = LoggerFactory.getLogger(AdjustQuotaService.class);

    @Autowired
    private UserQuotaMapper userQuotaMapper;
    @Autowired
    private QuotaChangeLogMapper quotaChangeLogMapper;

    @Transactional
    public ApiResponse adjust(ReqAdjustQuota req) {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can adjust quota");
        }
        if (req == null || req.getUserId() == null || req.getUserId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "user_id is required");
        }
        if (req.getRemark() == null || req.getRemark().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "remark is required");
        }
        String operator = UserContext.getUserId() == null ? "admin" : UserContext.getUserId();
        Date now = new Date();
        UserQuotaExample ex = new UserQuotaExample();
        ex.createCriteria().andUserIdEqualTo(req.getUserId().trim()).andDeleteEqualTo(0);
        UserQuota quota = userQuotaMapper.selectOneByExample(ex);
        if (quota == null) {
            quota = new UserQuota();
            quota.setUserId(req.getUserId().trim());
            quota.setBalanceCent(0);
            quota.setExtraBalanceCent(0);
            quota.setMonthlyLimitCent(0);
            quota.setMonthSpentCent(0);
            quota.setMonthResetDate(now);
            quota.setDelete(0);
            quota.setCreator(operator);
            quota.setModifier(operator);
            userQuotaMapper.insertSelective(quota);
        }
        int balDelta = req.getBalanceDeltaCent() == null ? 0 : req.getBalanceDeltaCent();
        int extraDelta = req.getExtraDeltaCent() == null ? 0 : req.getExtraDeltaCent();
        int newBal = Math.max(0, (quota.getBalanceCent() == null ? 0 : quota.getBalanceCent()) + balDelta);
        int newExtra = Math.max(0, (quota.getExtraBalanceCent() == null ? 0 : quota.getExtraBalanceCent()) + extraDelta);
        UserQuota patch = new UserQuota();
        patch.setBalanceCent(newBal);
        patch.setExtraBalanceCent(newExtra);
        if (req.getMonthlyLimitCent() != null) {
            patch.setMonthlyLimitCent(req.getMonthlyLimitCent());
        }
        patch.setModifier(operator);
        patch.setGmtModified(now);
        UserQuotaExample up = new UserQuotaExample();
        up.createCriteria().andIdEqualTo(quota.getId()).andDeleteEqualTo(0);
        userQuotaMapper.updateByExampleSelective(patch, up);
        QuotaChangeLog logRow = new QuotaChangeLog();
        logRow.setUserId(req.getUserId().trim());
        logRow.setOperatorUserId(operator);
        logRow.setChangeType("adjust");
        logRow.setBalanceDeltaCent(balDelta);
        logRow.setExtraDeltaCent(extraDelta);
        logRow.setBalanceAfterCent(newBal);
        logRow.setExtraAfterCent(newExtra);
        logRow.setMonthlyLimitCent(req.getMonthlyLimitCent() != null ? req.getMonthlyLimitCent() : quota.getMonthlyLimitCent());
        logRow.setRemark(req.getRemark().trim());
        logRow.setDelete(0);
        logRow.setCreator(operator);
        quotaChangeLogMapper.insertSelective(logRow);
        log.info("admin adjust quota userId={} balDelta={} extraDelta={}", req.getUserId(), balDelta, extraDelta);
        ApiResponse resp = new ApiResponse();
        resp.setCode("0");
        resp.setSuccess(true);
        resp.setMessage("OK");
        resp.setResult("ok");
        return resp;
    }
}
