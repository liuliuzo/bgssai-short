package com.bgssai.shortai.login.sms;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.login.oauth.OAuthIds;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;

@Component
public class TencentSmsSender {

    private static final Logger log = LoggerFactory.getLogger(TencentSmsSender.class);

    /** 与发码 API {@code expires_in=300} 对齐的正文「X 分钟内有效」。 */
    private static final int EXPIRE_MINUTES = 5;

    private final SmsSecrets smsSecrets;
    private final SmsOtpProperties smsOtpProperties;
    private final AtomicLong sendSeq = new AtomicLong(0L);

    public TencentSmsSender(SmsSecrets smsSecrets, SmsOtpProperties smsOtpProperties) {
        this.smsSecrets = smsSecrets;
        this.smsOtpProperties = smsOtpProperties;
    }

    /**
     * 发送登录 OTP。按 {@link SmsOtpProperties#getTemplateParamCount()} 组装模板参数；
     * 每次发码递增序号，并随结果回传产品名与序号。
     */
    public SmsSendResult send(String phone, String code) {
        if (OAuthIds.blank(smsSecrets.secretId()) || OAuthIds.blank(smsSecrets.secretKey())
                || OAuthIds.blank(smsSecrets.sdkAppId()) || OAuthIds.blank(smsSecrets.signName())
                || OAuthIds.blank(smsSecrets.templateId())) {
            throw new BizException("5203", "短信通道暂不可用");
        }
        String product = smsOtpProperties.getProductLabel();
        long seq = sendSeq.incrementAndGet();
        String[] templateParams = SmsTemplateParams.build(
                smsOtpProperties.getTemplateParamCount(), product, seq, code, EXPIRE_MINUTES);
        try {
            SmsClient client = new SmsClient(new Credential(smsSecrets.secretId(), smsSecrets.secretKey()), "ap-guangzhou");
            SendSmsRequest req = new SendSmsRequest();
            req.setSmsSdkAppId(smsSecrets.sdkAppId());
            req.setSignName(smsSecrets.signName());
            req.setTemplateId(smsSecrets.templateId());
            req.setPhoneNumberSet(new String[]{toE164(phone)});
            req.setTemplateParamSet(templateParams);
            SendSmsResponse resp = client.SendSms(req);
            SendStatus[] statusSet = resp.getSendStatusSet();
            if (statusSet == null || statusSet.length == 0 || !"Ok".equals(statusSet[0].getCode())) {
                throw new BizException("5203", "短信发送失败");
            }
            log.info("短信验证码已发送 phone={} product={} seq={} paramCount={} requestId={}",
                    mask(phone), product, seq, smsOtpProperties.getTemplateParamCount(), resp.getRequestId());
            return new SmsSendResult(product, seq);
        } catch (BizException e) {
            throw e;
        } catch (TencentCloudSDKException e) {
            log.warn("腾讯云短信发送异常 phone={} error={}", mask(phone), e.getErrorCode());
            throw new BizException("5203", "短信通道暂不可用");
        } catch (Exception e) {
            log.warn("腾讯云短信发送异常 phone={} error={}", mask(phone), e.getClass().getSimpleName());
            throw new BizException("5203", "短信通道暂不可用");
        }
    }

    static String toE164(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        return trimmed.startsWith("+") ? trimmed : "+86" + trimmed;
    }

    static String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
