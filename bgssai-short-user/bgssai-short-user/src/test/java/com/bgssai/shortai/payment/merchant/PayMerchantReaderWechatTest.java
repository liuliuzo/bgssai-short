package com.bgssai.shortai.payment.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.mapper.WechatPayConfigMapper;
import com.bgssai.shortai.model.WechatPayConfig;
import com.bgssai.shortai.payment.config.PaymentProperties;

class PayMerchantReaderWechatTest {

    @Test
    void activeMerchantKeepsStoredPublicKeyMode() {
        WechatPayConfigMapper mapper = mock(WechatPayConfigMapper.class);
        WechatPayConfig row = new WechatPayConfig();
        row.setMchId("1900000109");
        row.setAppId("wx-test-app");
        row.setMerchantSerialNo("TESTSERIAL");
        row.setMchPrivateKey("stored-private-key");
        row.setApiV3Key("stored-api-v3-key");
        row.setNotifyUrl("https://dev.user.example.com/bgssai/user/payment/wechat/notify");
        row.setKeyMode("PUBLIC_KEY");
        row.setWechatPublicKeyId("PUB_KEY_ID_TEST");
        row.setWechatPublicKey("-----BEGIN PUBLIC KEY-----\nTEST\n-----END PUBLIC KEY-----");
        when(mapper.selectOneByExample(any())).thenReturn(row);

        PayMerchantReader reader = new PayMerchantReader();
        ReflectionTestUtils.setField(reader, "wechatPayConfigMapper", mapper);
        ReflectionTestUtils.setField(reader, "paymentProperties", new PaymentProperties());

        WechatMerchant merchant = reader.requireActiveWechatMerchant();

        assertTrue(merchant.usesPublicKeyMode());
        assertEquals("PUB_KEY_ID_TEST", merchant.getWechatPublicKeyId());
        assertEquals(row.getWechatPublicKey(), merchant.getWechatPublicKey());
        assertEquals(row.getNotifyUrl(), merchant.getNotifyUrl());
    }
}
