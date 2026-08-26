package com.bgssai.shortai.recharge.service;

import java.text.SimpleDateFormat;
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

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.UnauthorizedException;
import com.bgssai.shortai.mapper.RechargeApplyMapper;
import com.bgssai.shortai.model.RechargeApply;
import com.bgssai.shortai.model.RechargeApplyExample;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.recharge.dto.ReqRechargeApply;

@Service
public class RechargeUserService {

    private static final Logger log = LoggerFactory.getLogger(RechargeUserService.class);

    @Autowired
    private RechargeApplyMapper rechargeApplyMapper;

    @Transactional
    public ApiResponse apply(ReqRechargeApply req) {
        String userId = requireUser();
        if (req == null || req.getApplyAmountCent() == null || req.getApplyAmountCent() <= 0) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "apply_amount_cent must be positive");
        }
        Date now = new Date();
        RechargeApply row = new RechargeApply();
        row.setApplyId(ShortIds.id("ra-"));
        row.setUserId(userId);
        row.setApplyAmountCent(req.getApplyAmountCent());
        row.setApplyStatus("SUBMITTED");
        row.setUserRemark(req.getRemark() == null ? null : req.getRemark().trim());
        row.setCreatedTime(now);
        row.setUpdatedTime(now);
        rechargeApplyMapper.insertSelective(row);
        log.info("recharge apply created applyId={} amount={}", row.getApplyId(), row.getApplyAmountCent());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("apply_id", row.getApplyId());
        result.put("apply_status", row.getApplyStatus());
        return Responses.ok(result);
    }

    public ApiResponse list() {
        String userId = requireUser();
        RechargeApplyExample ex = new RechargeApplyExample();
        ex.createCriteria().andUserIdEqualTo(userId);
        ex.setOrderByClause("created_time desc, id desc");
        List<RechargeApply> rows = rechargeApplyMapper.selectByExample(ex);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> list = new ArrayList<>();
        for (RechargeApply row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("apply_id", row.getApplyId());
            item.put("apply_amount_cent", row.getApplyAmountCent());
            item.put("apply_status", row.getApplyStatus());
            item.put("user_remark", row.getUserRemark());
            item.put("operator_remark", row.getOperatorRemark());
            item.put("created_time", row.getCreatedTime() == null ? null : fmt.format(row.getCreatedTime()));
            item.put("handled_time", row.getHandledTime() == null ? null : fmt.format(row.getHandledTime()));
            list.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("apply_list", list);
        return Responses.ok(result);
    }

    private static String requireUser() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new UnauthorizedException("no user in context");
        }
        return userId;
    }
}
