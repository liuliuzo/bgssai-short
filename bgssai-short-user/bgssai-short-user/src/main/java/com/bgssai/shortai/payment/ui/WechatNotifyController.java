package com.bgssai.shortai.payment.ui;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.payment.channel.NotifyContext;
import com.bgssai.shortai.payment.service.PaymentNotifyService;

@RestController
public class WechatNotifyController {

    @Autowired
    private PaymentNotifyService paymentNotifyService;

    @PostMapping(value = "/bgssai/user/payment/wechat/notify", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> handle(@RequestBody(required = false) String rawBody,
                                         @RequestHeader Map<String, String> headers) {
        try {
            String body = paymentNotifyService.applyNotify(
                    "WECHAT", NotifyContext.ofBody(rawBody, headers));
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
}
