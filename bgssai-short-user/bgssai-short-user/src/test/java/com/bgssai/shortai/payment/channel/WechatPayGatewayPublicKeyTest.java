package com.bgssai.shortai.payment.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.payment.merchant.WechatMerchant;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.auth.Validator;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;

class WechatPayGatewayPublicKeyTest {

    private static final KeyPair KEYS = generateKeys();
    private static final String PRIVATE_PEM = pem("PRIVATE KEY", KEYS.getPrivate().getEncoded());
    private static final String PUBLIC_PEM = pem("PUBLIC KEY", KEYS.getPublic().getEncoded());
    private static final String API_V3_KEY = "0123456789abcdef0123456789abcdef";

    @Test
    void publicKeyModeBuildsPublicKeyConfigAndSkipsPlatformCertificates() {
        RecordingGateway gateway = new RecordingGateway();
        WechatMerchant merchant = merchant("PUBLIC_KEY", "PUB_KEY_ID_TEST", PUBLIC_PEM);

        Config config = gateway.createConfig(merchant, PRIVATE_PEM);

        assertEquals(0, gateway.platformCertificateBuilds);
        assertInstanceOf(RSAPublicKeyConfig.class, config);
        assertInstanceOf(NotificationConfig.class, config);
        Validator validator = config.createValidator();
        assertEquals("PUB_KEY_ID_TEST", validator.getSerialNumber());
        new NotificationParser((NotificationConfig) config);
    }

    @Test
    void publicKeyModeAcceptsStoredEscapedNewlinesAndReusesCacheUntilKeyChanges() {
        WechatPayGateway gateway = new WechatPayGateway();
        WechatMerchant merchant = merchant("public_key", "PUB_KEY_ID_TEST", PUBLIC_PEM.replace("\n", "\\n"));
        merchant = new WechatMerchant(
                merchant.getMchId(), merchant.getAppId(), merchant.getMerchantSerialNo(),
                PRIVATE_PEM.replace("\n", "\\n"), merchant.getApiV3Key(), merchant.getNotifyUrl(),
                merchant.getKeyMode(), merchant.getWechatPublicKeyId(), merchant.getWechatPublicKey());

        Config first = require(gateway, merchant);
        Config second = require(gateway, merchant);
        assertSame(first, second);

        WechatMerchant rotated = merchant("PUBLIC_KEY", "PUB_KEY_ID_ROTATED", PUBLIC_PEM);
        Config third = require(gateway, rotated);
        assertNotSame(first, third);
        assertEquals("PUB_KEY_ID_ROTATED", third.createValidator().getSerialNumber());
    }

    @Test
    void publicKeyModeRejectsMissingOrUnreadablePublicKeyBeforeAnyCertificateDownload() {
        RecordingGateway gateway = new RecordingGateway();
        WechatMerchant missingKey = merchant("PUBLIC_KEY", "PUB_KEY_ID_TEST", " ");
        BizException missing = assertThrows(BizException.class, () -> require(gateway, missingKey));
        assertEquals("60013", missing.getCode());
        assertTrue(missing.getMessage().contains("wechat_public_key"));
        assertEquals(0, gateway.platformCertificateBuilds);

        WechatMerchant badKey = merchant("PUBLIC_KEY", "PUB_KEY_ID_TEST",
                "-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----");
        BizException unreadable = assertThrows(BizException.class,
                () -> gateway.createConfig(badKey, PRIVATE_PEM));
        assertEquals("60013", unreadable.getCode());
        assertTrue(unreadable.getMessage().contains("wechat_public_key unreadable"));
        assertEquals(0, gateway.platformCertificateBuilds);
    }

    @Test
    void notifyParserUsesPublicKeyConfigInsteadOfCastingToAutoCertificate() {
        WechatPayGateway gateway = new WechatPayGateway();
        WechatMerchant merchant = merchant("PUBLIC_KEY", "PUB_KEY_ID_TEST", PUBLIC_PEM);
        Map<String, String> headers = Map.of(
                "Wechatpay-Serial", "PUB_KEY_ID_TEST",
                "Wechatpay-Nonce", "nonce",
                "Wechatpay-Signature", "c2ln",
                "Wechatpay-Timestamp", "1700000000",
                "Wechatpay-Signature-Type", "WECHATPAY2-SHA256-RSA2048");
        NotifyContext ctx = NotifyContext.ofBody("{}", headers);

        assertThrows(ValidationException.class, () -> gateway.parseTransactionNotify(merchant, ctx));
    }

    @Test
    void platformCertificateModeStaysOnCertificateBuilderAndDoesNotRequirePublicKey() {
        RecordingGateway gateway = new RecordingGateway();
        WechatMerchant merchant = merchant("PLATFORM_CERT", null, null);

        assertFalse(merchant.usesPublicKeyMode());
        assertTrue(merchant.hasCompleteCredentials());
        assertEquals("none", merchant.missingFields());
        assertNull(gateway.createConfig(merchant, PRIVATE_PEM));
        assertEquals(1, gateway.platformCertificateBuilds);

        WechatMerchant unset = merchant(null, null, null);
        assertFalse(unset.usesPublicKeyMode());
        assertTrue(unset.hasCompleteCredentials());
    }

    private static Config require(WechatPayGateway gateway, WechatMerchant merchant) {
        return ReflectionTestUtils.invokeMethod(gateway, "requireConfig", merchant);
    }

    private static WechatMerchant merchant(String keyMode, String publicKeyId, String publicKey) {
        return new WechatMerchant(
                "1900000109",
                "wx-test-app",
                "TESTSERIAL",
                PRIVATE_PEM,
                API_V3_KEY,
                "https://dev.user.example.com/bgssai/user/payment/wechat/notify",
                keyMode,
                publicKeyId,
                publicKey);
    }

    private static KeyPair generateKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static String pem(String type, byte[] encoded) {
        String body = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----";
    }

    private static final class RecordingGateway extends WechatPayGateway {
        private int platformCertificateBuilds;

        @Override
        Config buildPlatformCertificateConfig(WechatMerchant merchant, String privateKeyPem) {
            platformCertificateBuilds++;
            return null;
        }
    }
}
