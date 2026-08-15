package com.bgssai.shortai.subscription.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.mapper.CreditPackMapper;
import com.bgssai.shortai.model.CreditPack;
import com.bgssai.shortai.model.CreditPackExample;

@Service
public class ListUserCreditPacksService {

    @Autowired
    private CreditPackMapper creditPackMapper;

    public ApiResponse list() {
        CreditPackExample ex = new CreditPackExample();
        ex.createCriteria().andDeleteEqualTo(0).andActiveEqualTo(1);
        ex.setOrderByClause("sort_order asc, id asc");
        List<CreditPack> rows = creditPackMapper.selectByExample(ex);
        List<Map<String, Object>> packList = new ArrayList<>();
        for (CreditPack row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("pack_code", row.getPackCode());
            item.put("name", row.getName());
            item.put("credit_cent", row.getCreditCent());
            item.put("price_cent", row.getPriceCent());
            packList.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pack_list", packList);
        ApiResponse resp = new ApiResponse();
        resp.setCode("0");
        resp.setSuccess(true);
        resp.setMessage("OK");
        resp.setResult(result);
        return resp;
    }
}
