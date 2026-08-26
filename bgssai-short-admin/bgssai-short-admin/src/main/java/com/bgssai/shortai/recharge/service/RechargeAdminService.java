package com.bgssai.shortai.recharge.service;

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
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.RechargeApplyMapper;
import com.bgssai.shortai.mapper.SysUserMapper;
import com.bgssai.shortai.model.RechargeApply;
import com.bgssai.shortai.model.RechargeApplyExample;
import com.bgssai.shortai.model.SysUser;
import com.bgssai.shortai.model.SysUserExample;
import com.bgssai.shortai.quota.dto.ReqAdjustQuota;
import com.bgssai.shortai.quota.service.AdjustQuotaService;
import com.bgssai.shortai.recharge.dto.ReqHandleRecharge;
import com.github.pagehelper.PageHelper;

@Service
public class RechargeAdminService {

    private static final Logger log = LoggerFactory.getLogger(RechargeAdminService.class);

    @Autowired
    private RechargeApplyMapper rechargeApplyMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private AdjustQuotaService adjustQuotaService;

    public PageResponse list(int pageNum, int pageSize, String applyStatus) {
        requireAdmin();
        int safePageNum = pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize < 1 || pageSize > 100 ? 20 : pageSize;
        RechargeApplyExample ex = new RechargeApplyExample();
        RechargeApplyExample.Criteria c = ex.createCriteria();
        if (applyStatus != null && !applyStatus.isBlank()) {
            c.andApplyStatusEqualTo(applyStatus.trim());
        }
        ex.setOrderByClause("created_time desc, id desc");
        PageHelper.startPage(safePageNum, safePageSize);
        List<RechargeApply> rows = rechargeApplyMapper.selectByExample(ex);
        long total = ((com.github.pagehelper.Page<RechargeApply>) rows).getTotal();
        List<Map<String, Object>> items = new ArrayList<>();
        for (RechargeApply row : rows) {
            items.add(view(row));
        }
        PageResponse response = new PageResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setMessage("OK");
        response.setResult(items);
        response.setPageNum(safePageNum);
        response.setPageSize(safePageSize);
        response.setTotalSize(total);
        return response;
    }

    @Transactional
    public ApiResponse handle(ReqHandleRecharge req) {
        requireAdmin();
        if (req == null || req.getApplyId() == null || req.getApplyId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "apply_id is required");
        }
        if (req.getRemark() == null || req.getRemark().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "remark is required");
        }
        String action = req.getHandleAction() == null ? "" : req.getHandleAction().trim();
        if (!"COMPLETE".equals(action) && !"REJECT".equals(action)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "handle_action must be COMPLETE or REJECT");
        }
        RechargeApplyExample ex = new RechargeApplyExample();
        ex.createCriteria().andApplyIdEqualTo(req.getApplyId().trim());
        RechargeApply row = rechargeApplyMapper.selectOneByExample(ex);
        if (row == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "apply not found");
        }
        if (!"SUBMITTED".equals(row.getApplyStatus()) && !"PROCESSING".equals(row.getApplyStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "apply status illegal");
        }
        String operator = UserContext.getUserId() == null ? "admin" : UserContext.getUserId();
        Date now = new Date();
        if ("COMPLETE".equals(action)) {
            ReqAdjustQuota adjust = new ReqAdjustQuota();
            adjust.setUserId(row.getUserId());
            adjust.setExtraDeltaCent(row.getApplyAmountCent());
            adjust.setRemark(req.getRemark().trim());
            adjustQuotaService.adjust(adjust);
            RechargeApply patch = new RechargeApply();
            patch.setApplyStatus("COMPLETED");
            patch.setOperatorUserId(operator);
            patch.setOperatorRemark(req.getRemark().trim());
            patch.setHandledTime(now);
            patch.setUpdatedTime(now);
            RechargeApplyExample up = new RechargeApplyExample();
            up.createCriteria().andIdEqualTo(row.getId());
            rechargeApplyMapper.updateByExampleSelective(patch, up);
            log.info("recharge completed applyId={} amount={}", row.getApplyId(), row.getApplyAmountCent());
        } else {
            RechargeApply patch = new RechargeApply();
            patch.setApplyStatus("REJECTED");
            patch.setOperatorUserId(operator);
            patch.setOperatorRemark(req.getRemark().trim());
            patch.setHandledTime(now);
            patch.setUpdatedTime(now);
            RechargeApplyExample up = new RechargeApplyExample();
            up.createCriteria().andIdEqualTo(row.getId());
            rechargeApplyMapper.updateByExampleSelective(patch, up);
            log.info("recharge rejected applyId={}", row.getApplyId());
        }
        return Responses.ok("ok");
    }

    private Map<String, Object> view(RechargeApply row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("apply_id", row.getApplyId());
        item.put("user_id", row.getUserId());
        SysUserExample ux = new SysUserExample();
        ux.createCriteria().andUserIdEqualTo(row.getUserId());
        SysUser user = sysUserMapper.selectOneByExample(ux);
        item.put("user_name", user == null ? null : user.getUserName());
        item.put("apply_amount_cent", row.getApplyAmountCent());
        item.put("apply_status", row.getApplyStatus());
        item.put("user_remark", row.getUserRemark());
        item.put("operator_remark", row.getOperatorRemark());
        item.put("handled_time", row.getHandledTime());
        item.put("created_time", row.getCreatedTime());
        return item;
    }

    private void requireAdmin() {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can handle recharge");
        }
    }
}
