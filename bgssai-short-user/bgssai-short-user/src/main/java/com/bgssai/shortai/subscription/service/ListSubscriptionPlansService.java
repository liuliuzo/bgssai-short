package com.bgssai.shortai.subscription.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.mapper.SubscriptionPlanMapper;
import com.bgssai.shortai.model.SubscriptionPlan;
import com.bgssai.shortai.model.SubscriptionPlanExample;

@Service
public class ListSubscriptionPlansService {

    @Autowired
    private SubscriptionPlanMapper subscriptionPlanMapper;

    public ApiResponse list() {
        SubscriptionPlanExample ex = new SubscriptionPlanExample();
        ex.createCriteria().andDelFlagEqualTo(0).andActiveEqualTo(1);
        ex.setOrderByClause("sort_order asc, id asc");
        List<SubscriptionPlan> rows = subscriptionPlanMapper.selectByExample(ex);
        List<Map<String, Object>> planList = new ArrayList<>();
        for (SubscriptionPlan row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("plan_code", row.getPlanCode());
            item.put("plan_name", row.getPlanName());
            item.put("price_cent", row.getPriceCent());
            item.put("monthly_balance_cent", row.getMonthlyBalanceCent());
            item.put("monthly_limit_cent", row.getMonthlyLimitCent());
            planList.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("plan_list", planList);
        ApiResponse resp = new ApiResponse();
        resp.setCode("0");
        resp.setSuccess(true);
        resp.setMessage("OK");
        resp.setResult(result);
        return resp;
    }
}
