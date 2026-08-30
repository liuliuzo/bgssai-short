package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.AdminAuthGuard;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.SysUserStatusMapper;
import com.bgssai.shortai.mapper.UserQuotaMapper;
import com.bgssai.shortai.model.UserQuota;
import com.bgssai.shortai.model.UserQuotaExample;
import com.bgssai.shortai.production.dto.CreatorRow;
import com.bgssai.shortai.production.dto.ToggleCreatorReq;
import com.github.pagehelper.PageHelper;

@Service
public class CreatorAdminService {

    @Autowired
    private SysUserStatusMapper sysUserStatusMapper;
    @Autowired
    private UserQuotaMapper userQuotaMapper;

    public PageResponse list(int pageNum, int pageSize, String keyword, Integer enabledFlag) {
        requireAdmin();
        int safePageNum = pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize < 1 || pageSize > 100 ? 20 : pageSize;
        Integer status = null;
        if (Integer.valueOf(1).equals(enabledFlag)) {
            status = 1;
        } else if (Integer.valueOf(0).equals(enabledFlag)) {
            status = 2;
        }
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        PageHelper.startPage(safePageNum, safePageSize);
        List<CreatorRow> rows = sysUserStatusMapper.selectCreators(kw, status);
        long total = ((com.github.pagehelper.Page<CreatorRow>) rows).getTotal();
        List<Map<String, Object>> items = new ArrayList<>();
        for (CreatorRow row : rows) {
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

    public ApiResponse toggleEnabled(ToggleCreatorReq req) {
        requireAdmin();
        if (req == null || req.getUserId() == null || req.getUserId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "user_id is required");
        }
        if (!Integer.valueOf(0).equals(req.getEnabledFlag()) && !Integer.valueOf(1).equals(req.getEnabledFlag())) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "enabled_flag must be 0 or 1");
        }
        int status = Integer.valueOf(1).equals(req.getEnabledFlag()) ? 1 : 2;
        String operator = UserContext.getUserId() == null ? "admin" : UserContext.getUserId();
        int n = sysUserStatusMapper.updateStatus(req.getUserId().trim(), status, operator);
        if (n != 1) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "creator not found");
        }
        return Responses.ok("ok");
    }

    private Map<String, Object> view(CreatorRow row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("user_id", row.getUserId());
        item.put("user_name", row.getUserName());
        item.put("real_name", row.getRealName());
        item.put("email", row.getEmail());
        item.put("phone", row.getPhone());
        int status = row.getStatus() == null ? 1 : row.getStatus();
        item.put("status", status);
        item.put("enabled_flag", status == 1 ? 1 : 0);
        item.put("create_time", row.getCreateTime());
        UserQuotaExample qx = new UserQuotaExample();
        qx.createCriteria().andUserIdEqualTo(row.getUserId()).andDelFlagEqualTo(0);
        UserQuota quota = userQuotaMapper.selectOneByExample(qx);
        item.put("balance_cent", quota == null ? 0 : quota.getBalanceCent());
        item.put("extra_balance_cent", quota == null ? 0 : quota.getExtraBalanceCent());
        item.put("monthly_limit_cent", quota == null ? 0 : quota.getMonthlyLimitCent());
        return item;
    }

    private void requireAdmin() {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can manage creators");
        }
    }
}
