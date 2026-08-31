package com.bgssai.shortai.subscription.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.UnauthorizedException;
import com.bgssai.shortai.mapper.PaymentOrderMapper;
import com.bgssai.shortai.model.PaymentOrder;
import com.bgssai.shortai.model.PaymentOrderExample;

@Service
public class ListUserSubscriptionOrdersService {

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    public ApiResponse list() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new UnauthorizedException("no user in context");
        }
        PaymentOrderExample ex = new PaymentOrderExample();
        ex.createCriteria()
                .andUserIdEqualTo(userId)
                .andDelFlagEqualTo(0)
                .andOrderTypeIn(Arrays.asList("subscription", "credit_pack"));
        ex.setOrderByClause("gmt_create desc");
        List<PaymentOrder> rows = paymentOrderMapper.selectByExample(ex);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> orderList = new ArrayList<>();
        for (PaymentOrder row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("order_no", row.getOrderNo());
            item.put("order_type", row.getOrderType());
            item.put("biz_code", row.getBizCode());
            item.put("biz_name", row.getBizName());
            item.put("amount_cent", row.getAmountCent());
            item.put("status", row.getStatus());
            item.put("payment_channel", row.getPaymentChannel());
            item.put("paid_at", row.getPaidAt() == null ? null : fmt.format(row.getPaidAt()));
            item.put("created_at", row.getGmtCreate() == null ? null : fmt.format(row.getGmtCreate()));
            orderList.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order_list", orderList);
        ApiResponse resp = new ApiResponse();
        resp.setCode("0");
        resp.setSuccess(true);
        resp.setMessage("OK");
        resp.setResult(result);
        return resp;
    }
}
