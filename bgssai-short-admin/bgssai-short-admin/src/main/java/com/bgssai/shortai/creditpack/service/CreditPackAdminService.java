package com.bgssai.shortai.creditpack.service;

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
import com.bgssai.shortai.creditpack.dto.ReqSaveCreditPack;
import com.bgssai.shortai.mapper.CreditPackMapper;
import com.bgssai.shortai.model.CreditPack;
import com.bgssai.shortai.model.CreditPackExample;

@Service
public class CreditPackAdminService {

    private static final Logger log = LoggerFactory.getLogger(CreditPackAdminService.class);

    @Autowired
    private CreditPackMapper creditPackMapper;

    public ApiResponse list() {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can list credit packs");
        }
        CreditPackExample ex = new CreditPackExample();
        ex.createCriteria().andDeleteEqualTo(0);
        ex.setOrderByClause("sort_order asc, id asc");
        List<CreditPack> rows = creditPackMapper.selectByExample(ex);
        List<Map<String, Object>> packList = new ArrayList<>();
        for (CreditPack row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.getId());
            item.put("pack_code", row.getPackCode());
            item.put("name", row.getName());
            item.put("credit_cent", row.getCreditCent());
            item.put("price_cent", row.getPriceCent());
            item.put("is_active", row.getActive());
            item.put("sort_order", row.getSortOrder());
            packList.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pack_list", packList);
        return ok(result);
    }

    @Transactional
    public ApiResponse save(ReqSaveCreditPack req) {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can save credit packs");
        }
        if (req == null || req.getPackCode() == null || req.getPackCode().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "pack_code is required");
        }
        String operator = UserContext.getUserId() == null ? "admin" : UserContext.getUserId();
        Date now = new Date();
        CreditPackExample ex = new CreditPackExample();
        ex.createCriteria().andPackCodeEqualTo(req.getPackCode().trim());
        CreditPack existing = creditPackMapper.selectOneByExample(ex);
        CreditPack row = new CreditPack();
        row.setPackCode(req.getPackCode().trim());
        row.setName(req.getName());
        row.setCreditCent(req.getCreditCent());
        row.setPriceCent(req.getPriceCent());
        row.setActive(req.getActive() == null ? 1 : req.getActive());
        row.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        row.setDelete(0);
        row.setModifier(operator);
        row.setGmtModified(now);
        if (existing == null) {
            row.setCreator(operator);
            creditPackMapper.insertSelective(row);
            log.info("admin credit pack created packCode={}", row.getPackCode());
        } else {
            row.setId(existing.getId());
            creditPackMapper.updateByPrimaryKeySelective(row);
            log.info("admin credit pack updated id={} packCode={}", existing.getId(), row.getPackCode());
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
