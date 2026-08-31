package com.bgssai.shortai.subscriptionplan.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.bgssai.shortai.mapper.SubscriptionPlanMapper;
import com.bgssai.shortai.model.SubscriptionPlan;
import com.bgssai.shortai.model.SubscriptionPlanExample;
import com.bgssai.shortai.subscriptionplan.dto.ReqSaveSubscriptionPlan;

@Service
public class SubscriptionPlanAdminService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionPlanAdminService.class);

    @Autowired
    private SubscriptionPlanMapper subscriptionPlanMapper;

    public ApiResponse list() {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can list subscription plans");
        }
        SubscriptionPlanExample ex = new SubscriptionPlanExample();
        ex.createCriteria().andDelFlagEqualTo(0);
        ex.setOrderByClause("sort_order asc, id asc");
        List<SubscriptionPlan> rows = subscriptionPlanMapper.selectByExample(ex);
        List<Map<String, Object>> planList = new ArrayList<>();
        for (SubscriptionPlan row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.getId());
            item.put("plan_code", row.getPlanCode());
            item.put("plan_name", row.getPlanName());
            item.put("price_cent", row.getPriceCent());
            item.put("monthly_balance_cent", row.getMonthlyBalanceCent());
            item.put("monthly_limit_cent", row.getMonthlyLimitCent());
            item.put("is_active", row.getActive());
            item.put("sort_order", row.getSortOrder());
            planList.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("plan_list", planList);
        return ok(result);
    }

    @Transactional
    public ApiResponse save(ReqSaveSubscriptionPlan req) {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can save subscription plans");
        }
        if (req == null || req.getPlanCode() == null || req.getPlanCode().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "plan_code is required");
        }
        String operator = UserContext.getUserId() == null ? "admin" : UserContext.getUserId();
        Date now = new Date();
        SubscriptionPlanExample ex = new SubscriptionPlanExample();
        ex.createCriteria().andPlanCodeEqualTo(req.getPlanCode().trim());
        SubscriptionPlan existing = subscriptionPlanMapper.selectOneByExample(ex);
        SubscriptionPlan row = new SubscriptionPlan();
        row.setPlanCode(req.getPlanCode().trim());
        row.setPlanName(req.getPlanName());
        row.setPriceCent(req.getPriceCent());
        row.setMonthlyBalanceCent(req.getMonthlyBalanceCent());
        row.setMonthlyLimitCent(req.getMonthlyLimitCent());
        row.setActive(req.getActive() == null ? 1 : req.getActive());
        row.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        row.setDelFlag(0);
        row.setModifier(operator);
        row.setGmtModified(now);
        if (existing == null) {
            row.setCreator(operator);
            subscriptionPlanMapper.insertSelective(row);
            log.info("admin subscription plan created planCode={}", row.getPlanCode());
        } else {
            row.setId(existing.getId());
            subscriptionPlanMapper.updateByPrimaryKeySelective(row);
            log.info("admin subscription plan updated id={} planCode={}", existing.getId(), row.getPlanCode());
        }
        return ok(row.getId() == null ? existing.getId() : row.getId());
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
