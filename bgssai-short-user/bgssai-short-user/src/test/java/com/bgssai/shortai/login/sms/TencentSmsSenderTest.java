package com.bgssai.shortai.login.sms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.bgssai.shortai.common.exception.BizException;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;

class TencentSmsSenderTest {

    private static Map<String, String> baseDomestic() {
        Map<String, String> map = new HashMap<>();
        map.put(SmsSecrets.SECRET_ID, "dom-id");
        map.put(SmsSecrets.SECRET_KEY, "dom-key");
        map.put(SmsSecrets.SDK_APP_ID, "1401156379");
        map.put(SmsSecrets.SIGN_NAME, "昆山兵贵神速智能科技");
        map.put(SmsSecrets.TEMPLATE_ID, "2677885");
        return map;
    }

    private static SendSmsResponse okResponse() {
        SendSmsResponse resp = new SendSmsResponse();
        SendStatus status = new SendStatus();
        status.setCode("Ok");
        resp.setSendStatusSet(new SendStatus[] { status });
        resp.setRequestId("req-1");
        return resp;
    }

    @Test
    void domesticUsesDefaultEndpointRegionAndCanonicalParams() {
        Map<String, String> map = baseDomestic();
        map.put(SmsSecrets.DOMESTIC_TEMPLATE_PARAM_COUNT, "3");
        map.put(SmsSecrets.DOMESTIC_PRODUCT_LABEL, "Short");

        SendSmsRequest[] capturedReq = new SendSmsRequest[1];
        String[] capturedMeta = new String[4];
        TencentSmsSender sender = new TencentSmsSender(
                new SmsSecrets(map::get),
                new SmsOtpProperties("Short", 2),
                (ep, rg, id, key, req) -> {
                    capturedMeta[0] = ep;
                    capturedMeta[1] = rg;
                    capturedMeta[2] = id;
                    capturedMeta[3] = key;
                    capturedReq[0] = req;
                    return okResponse();
                });

        sender.send("+86 138-0013-8000", "654321");

        assertEquals("sms.tencentcloudapi.com", capturedMeta[0]);
        assertEquals("ap-guangzhou", capturedMeta[1]);
        assertEquals("dom-id", capturedMeta[2]);
        assertEquals("dom-key", capturedMeta[3]);
        assertArrayEquals(new String[] { "+8613800138000" }, capturedReq[0].getPhoneNumberSet());
        assertArrayEquals(new String[] { "Short", "654321", "5" }, capturedReq[0].getTemplateParamSet());
        assertEquals("昆山兵贵神速智能科技", capturedReq[0].getSignName());
        assertNull(capturedReq[0].getSenderId());
    }

    @Test
    void domesticCount4IncludesProduct4DigitSeqCodeMinutes() {
        Map<String, String> map = baseDomestic();
        map.put(SmsSecrets.DOMESTIC_TEMPLATE_PARAM_COUNT, "4");
        map.put(SmsSecrets.DOMESTIC_PRODUCT_LABEL, "Short");

        SendSmsRequest[] captured = new SendSmsRequest[1];
        TencentSmsSender sender = new TencentSmsSender(
                new SmsSecrets(map::get),
                new SmsOtpProperties("Short", 2),
                (ep, rg, id, key, req) -> {
                    captured[0] = req;
                    return okResponse();
                });

        sender.send("13800138000", "112233");
        String[] params = captured[0].getTemplateParamSet();
        assertEquals(4, params.length);
        assertEquals("Short", params[0]);
        assertTrue(params[1].matches("\\d{4}"));
        assertEquals("112233", params[2]);
        assertEquals("5", params[3]);
    }

    @Test
    void internationalDisabledFailsClosedWithoutFallback() {
        Map<String, String> map = baseDomestic();
        map.put(SmsSecrets.INTL_ENABLED, "0");
        map.put(SmsSecrets.INTL_SDK_APP_ID, "1409999999");
        map.put(SmsSecrets.INTL_TEMPLATE_ID, "999999");

        AtomicInteger calls = new AtomicInteger();
        TencentSmsSender sender = new TencentSmsSender(
                new SmsSecrets(map::get),
                new SmsOtpProperties("Short", 2),
                (ep, rg, id, key, req) -> {
                    calls.incrementAndGet();
                    return okResponse();
                });

        BizException ex = assertThrows(BizException.class, () -> sender.send("+85291234567", "123456"));
        assertEquals("5203", ex.getCode());
        assertEquals(0, calls.get());
    }

    @Test
    void internationalOptionalSignNameReusesDomesticCredentialsWhenBothBlank() {
        Map<String, String> map = baseDomestic();
        map.put(SmsSecrets.INTL_ENABLED, "1");
        map.put(SmsSecrets.INTL_ENDPOINT, "sms.intl.tencentcloudapi.com");
        map.put(SmsSecrets.INTL_REGION, "ap-singapore");
        map.put(SmsSecrets.INTL_SDK_APP_ID, "1402000001");
        map.put(SmsSecrets.INTL_TEMPLATE_ID, "3000001");
        map.put(SmsSecrets.INTL_SENDER_ID, "BGSSAI");
        map.put(SmsSecrets.INTL_TEMPLATE_PARAM_COUNT, "3");
        map.put(SmsSecrets.INTL_PRODUCT_LABEL, "BGSSAI Short");

        SendSmsRequest[] capturedReq = new SendSmsRequest[1];
        String[] capturedMeta = new String[4];
        TencentSmsSender sender = new TencentSmsSender(
                new SmsSecrets(map::get),
                new SmsOtpProperties("Short", 2),
                (ep, rg, id, key, req) -> {
                    capturedMeta[0] = ep;
                    capturedMeta[1] = rg;
                    capturedMeta[2] = id;
                    capturedMeta[3] = key;
                    capturedReq[0] = req;
                    return okResponse();
                });

        sender.send("+1 415-555-0100", "246810");

        assertEquals("sms.intl.tencentcloudapi.com", capturedMeta[0]);
        assertEquals("ap-singapore", capturedMeta[1]);
        assertEquals("dom-id", capturedMeta[2]);
        assertEquals("dom-key", capturedMeta[3]);
        assertArrayEquals(new String[] { "+14155550100" }, capturedReq[0].getPhoneNumberSet());
        assertArrayEquals(new String[] { "BGSSAI Short", "246810", "5" }, capturedReq[0].getTemplateParamSet());
        assertNull(capturedReq[0].getSignName());
        assertEquals("BGSSAI", capturedReq[0].getSenderId());
    }

    @Test
    void internationalRejectsHalfConfiguredCredentialsAndRegionMismatch() {
        Map<String, String> halfCred = baseDomestic();
        halfCred.put(SmsSecrets.INTL_ENABLED, "1");
        halfCred.put(SmsSecrets.INTL_SECRET_ID, "intl-only-id");
        halfCred.put(SmsSecrets.INTL_SDK_APP_ID, "1402000001");
        halfCred.put(SmsSecrets.INTL_TEMPLATE_ID, "3000001");

        TencentSmsSender halfSender = new TencentSmsSender(
                new SmsSecrets(halfCred::get),
                new SmsOtpProperties("Short", 2),
                (ep, rg, id, key, req) -> okResponse());
        assertEquals("5203", assertThrows(BizException.class, () -> halfSender.send("+85291234567", "123456")).getCode());

        Map<String, String> badRegion = baseDomestic();
        badRegion.put(SmsSecrets.INTL_ENABLED, "1");
        badRegion.put(SmsSecrets.INTL_ENDPOINT, "sms.tencentcloudapi.com");
        badRegion.put(SmsSecrets.INTL_REGION, "ap-singapore");
        badRegion.put(SmsSecrets.INTL_SDK_APP_ID, "1402000001");
        badRegion.put(SmsSecrets.INTL_TEMPLATE_ID, "3000001");

        TencentSmsSender badRegionSender = new TencentSmsSender(
                new SmsSecrets(badRegion::get),
                new SmsOtpProperties("Short", 2),
                (ep, rg, id, key, req) -> okResponse());
        assertEquals("5203", assertThrows(BizException.class, () -> badRegionSender.send("+85291234567", "123456")).getCode());
    }
}
