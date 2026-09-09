package com.bgssai.shortai.payment.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.payment.channel.NotifyContext;
import com.bgssai.shortai.payment.service.PaymentNotifyService;

@RestController
public class WechatNotifyController {

    @Autowired
    private PaymentNotifyService paymentNotifyService;

    @PostMapping(value = "/bgssai/user/payment/wechat/notify",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                    MediaType.ALL_VALUE},
            produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> handle(@RequestBody(required = false) String rawBody,
                                         @RequestParam Map<String, String> formParams,
                                         @RequestHeader Map<String, String> headers) {
        try {
            NotifyContext context;
            if (formParams != null && !formParams.isEmpty()
                    && (rawBody == null || rawBody.isBlank()
                    || looksLikeForm(rawBody))) {
                Map<String, String> params = new LinkedHashMap<>(formParams);
                context = NotifyContext.ofForm(params, headers);
            } else {
                context = NotifyContext.ofBody(rawBody, headers);
            }
            String body = paymentNotifyService.applyNotify("WECHAT", context);
            return ResponseEntity.ok(body);
        } catch (BizException e) {
            if ("60005".equals(e.getCode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"code\":\"FAIL\",\"message\":\"SIGN_ERROR\"}");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"code\":\"FAIL\",\"message\":\"ERROR\"}");
        }
    }

    private static boolean looksLikeForm(String rawBody) {
        return rawBody.contains("=") && !rawBody.trim().startsWith("{");
    }
}
