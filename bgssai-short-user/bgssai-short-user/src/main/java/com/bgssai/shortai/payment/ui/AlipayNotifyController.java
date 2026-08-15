package com.bgssai.shortai.payment.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.payment.channel.NotifyContext;
import com.bgssai.shortai.payment.service.PaymentNotifyService;

@RestController
public class AlipayNotifyController {

    @Autowired
    private PaymentNotifyService paymentNotifyService;

    @PostMapping(value = "/bgssai/user/payment/alipay/notify",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.ALL_VALUE},
            produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> handle(@RequestParam Map<String, String> formParams,
                                         @RequestHeader Map<String, String> headers) {
        try {
            Map<String, String> params = formParams == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(formParams);
            String body = paymentNotifyService.applyNotify(
                    "ALIPAY", NotifyContext.ofForm(params, headers));
            return ResponseEntity.ok(body);
        } catch (BizException e) {
            return ResponseEntity.ok("failure");
        }
    }
}
