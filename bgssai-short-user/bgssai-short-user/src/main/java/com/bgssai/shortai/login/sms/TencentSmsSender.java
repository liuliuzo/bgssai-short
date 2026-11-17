package com.bgssai.shortai.login.sms;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;

/**
 * 腾讯云短信 2021-01-11 SendSms 发送器：严格按国内（+86）与国际/港澳台（非 +86 E.164）隔离路由与配置。
 */
@Component
public class TencentSmsSender {

    private static final Logger log = LoggerFactory.getLogger(TencentSmsSender.class);

    /** 与发码 API {@code expires_in=300} 对齐的正文「X 分钟内有效」。 */
    static final int EXPIRE_MINUTES = 5;

    private static final String DOMESTIC_ENDPOINT = "sms.tencentcloudapi.com";
    private static final String INTL_ENDPOINT = "sms.intl.tencentcloudapi.com";
    private static final Set<String> DOMESTIC_REGIONS = Set.of("ap-guangzhou", "ap-beijing", "ap-nanjing");
    private static final Set<String> INTL_REGIONS = Set.of("ap-singapore", "eu-frankfurt");

    @FunctionalInterface
    interface SmsGateway {
        SendSmsResponse send(String endpoint, String region, String secretId, String secretKey, SendSmsRequest req)
                throws TencentCloudSDKException;
    }

    private record ChannelSpec(
            boolean intl,
            String secretId,
            String secretKey,
            String endpoint,
            String region,
            String sdkAppId,
            String signName,
            String senderId,
            String templateId,
            int paramCount,
            String productLabel) {
    }

    private final SmsSecrets smsSecrets;
    private final SmsOtpProperties smsOtpProperties;
    private final SmsGateway gateway;

    @Autowired
    public TencentSmsSender(SmsSecrets smsSecrets, SmsOtpProperties smsOtpProperties) {
        this(smsSecrets, smsOtpProperties, defaultGateway());
    }

    TencentSmsSender(SmsSecrets smsSecrets, SmsOtpProperties smsOtpProperties, SmsGateway gateway) {
        this.smsSecrets = smsSecrets;
        this.smsOtpProperties = smsOtpProperties;
        this.gateway = gateway;
    }

    public void send(String rawPhone, String code) {
        TencentSmsPhoneRoute.Route route = TencentSmsPhoneRoute.routeOf(rawPhone);
        if (route == null) {
            throw new BizException("4301", "手机号格式不正确");
        }
        String e164 = TencentSmsPhoneRoute.toTencentE164(rawPhone);
        ChannelSpec spec = route == TencentSmsPhoneRoute.Route.INTERNATIONAL
                ? resolveInternationalSpec()
                : resolveDomesticSpec();

        String seq = TencentSmsTemplateParams.nextSeq();
        String[] templateParams = TencentSmsTemplateParams.build(
                spec.productLabel(), seq, code, EXPIRE_MINUTES, spec.paramCount(), "Short");

        SendSmsRequest req = new SendSmsRequest();
        req.setSmsSdkAppId(spec.sdkAppId());
        req.setTemplateId(spec.templateId());
        if (!blank(spec.signName())) {
            req.setSignName(spec.signName());
        }
        if (spec.intl() && !blank(spec.senderId())) {
            req.setSenderId(spec.senderId());
        }
        req.setPhoneNumberSet(new String[] { e164 });
        req.setTemplateParamSet(templateParams);

        try {
            SendSmsResponse resp = gateway.send(spec.endpoint(), spec.region(), spec.secretId(), spec.secretKey(), req);
            SendStatus[] statusSet = resp == null ? null : resp.getSendStatusSet();
            if (statusSet == null || statusSet.length == 0 || !"Ok".equals(statusSet[0].getCode())) {
                String statusCode = (statusSet != null && statusSet.length > 0) ? statusSet[0].getCode() : "EMPTY";
                log.warn("腾讯云短信发送失败 route={} phone={} status={}",
                        route, TencentSmsPhoneRoute.maskPhone(e164), statusCode);
                throw new BizException("5203", "短信发送失败");
            }
            log.info("短信验证码已发送 route={} phone={} endpoint={} region={} paramCount={} requestId={}",
                    route, TencentSmsPhoneRoute.maskPhone(e164), spec.endpoint(), spec.region(),
                    spec.paramCount(), resp.getRequestId());
        } catch (BizException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            log.warn("腾讯云短信发送异常 route={} phone={} error={}",
                    route, TencentSmsPhoneRoute.maskPhone(e164), e.getErrorCode());
            throw new BizException("5203", "短信通道暂不可用");
        } catch (Exception e) {
            log.warn("腾讯云短信发送异常 route={} phone={} error={}",
                    route, TencentSmsPhoneRoute.maskPhone(e164), e.getClass().getSimpleName());
            throw new BizException("5203", "短信通道暂不可用");
        }
    }

    private ChannelSpec resolveDomesticSpec() {
        String secretId = smsSecrets.secretId();
        String secretKey = smsSecrets.secretKey();
        String sdkAppId = smsSecrets.sdkAppId();
        String signName = smsSecrets.signName();
        String templateId = smsSecrets.templateId();
        if (blank(secretId) || blank(secretKey) || blank(sdkAppId) || blank(signName) || blank(templateId)) {
            throw new BizException("5203", "短信通道暂不可用");
        }
        String endpoint = defaultIfBlank(smsSecrets.endpoint(), DOMESTIC_ENDPOINT);
        String region = defaultIfBlank(smsSecrets.region(), SmsSecrets.DEFAULT_REGION);
        requireValidEndpointAndRegion(endpoint, region, "MAINLAND");
        String productLabel = firstNonBlank(smsSecrets.productLabel(), smsOtpProperties.getProductLabel(), "Short");
        int paramCount = parseParamCount(smsSecrets.templateParamCount(), smsOtpProperties.getTemplateParamCount(), "MAINLAND");
        return new ChannelSpec(false, secretId, secretKey, endpoint, region, sdkAppId, signName, "", templateId, paramCount, productLabel);
    }

    private ChannelSpec resolveInternationalSpec() {
        if (!smsSecrets.intlEnabled()) {
            throw new BizException("5203", "国际及港澳台短信通道未启用");
        }
        String intlSecretId = smsSecrets.intlSecretId();
        String intlSecretKey = smsSecrets.intlSecretKey();
        if (blank(intlSecretId) != blank(intlSecretKey)) {
            throw new BizException("5203", "国际短信密钥配置不完整");
        }
        String secretId = blank(intlSecretId) ? smsSecrets.secretId() : intlSecretId;
        String secretKey = blank(intlSecretKey) ? smsSecrets.secretKey() : intlSecretKey;
        String sdkAppId = smsSecrets.intlSdkAppId();
        String templateId = smsSecrets.intlTemplateId();
        if (blank(secretId) || blank(secretKey) || blank(sdkAppId) || blank(templateId)) {
            throw new BizException("5203", "国际短信通道暂不可用");
        }
        String endpoint = defaultIfBlank(smsSecrets.intlEndpoint(), DOMESTIC_ENDPOINT);
        String region = defaultIfBlank(smsSecrets.intlRegion(), SmsSecrets.DEFAULT_REGION);
        requireValidEndpointAndRegion(endpoint, region, "INTERNATIONAL");
        String signName = smsSecrets.intlSignName();
        String senderId = smsSecrets.intlSenderId();
        String productLabel = firstNonBlank(smsSecrets.intlProductLabel(), smsOtpProperties.getProductLabel(), "Short");
        int paramCount = parseParamCount(smsSecrets.intlTemplateParamCount(), 2, "INTERNATIONAL");
        return new ChannelSpec(true, secretId, secretKey, endpoint, region, sdkAppId, signName, senderId, templateId, paramCount, productLabel);
    }

    static boolean isValidEndpointAndRegion(String endpoint, String region) {
        if (DOMESTIC_ENDPOINT.equals(endpoint)) {
            return DOMESTIC_REGIONS.contains(region);
        }
        if (INTL_ENDPOINT.equals(endpoint)) {
            return INTL_REGIONS.contains(region);
        }
        return false;
    }

    private static void requireValidEndpointAndRegion(String endpoint, String region, String route) {
        if (!isValidEndpointAndRegion(endpoint, region)) {
            log.warn("腾讯云短信接入点与地域配置无效 route={} endpoint={} region={}", route, endpoint, region);
            throw new BizException("5203", "短信通道配置无效");
        }
    }

    private static int parseParamCount(String raw, int fallback, String route) {
        if (blank(raw)) {
            return (fallback == 2 || fallback == 3 || fallback == 4) ? fallback : 2;
        }
        String trimmed = raw.trim();
        if ("2".equals(trimmed)) {
            return 2;
        }
        if ("3".equals(trimmed)) {
            return 3;
        }
        if ("4".equals(trimmed)) {
            return 4;
        }
        log.warn("腾讯云短信模板参数数量配置无效 route={} value={}，按 2 回落", route, trimmed);
        return 2;
    }

    private static SmsGateway defaultGateway() {
        return (endpoint, region, secretId, secretKey, req) -> {
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(endpoint);
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            SmsClient client = new SmsClient(new Credential(secretId, secretKey), region, clientProfile);
            return client.SendSms(req);
        };
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String defaultIfBlank(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (!blank(v)) {
                return v.trim();
            }
        }
        return "";
    }

    static String toE164(String phone) {
        return TencentSmsPhoneRoute.toTencentE164(phone);
    }

    static String mask(String phone) {
        return TencentSmsPhoneRoute.maskPhone(phone);
    }
}
