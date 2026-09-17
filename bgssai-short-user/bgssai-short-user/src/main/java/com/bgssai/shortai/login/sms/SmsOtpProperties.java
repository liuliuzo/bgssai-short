package com.bgssai.shortai.login.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 短信 OTP 产品名与模板参数个数。
 *
 * <p>跨仓约定：
 * <ul>
 *   <li>{@code bgssai.sms.product-label} — 短信正文里的产品名，本仓默认 Short</li>
 *   <li>{@code bgssai.sms.template-param-count} — 2|3|4；默认 2（生产模板 2677885
 *       为双参，待审 2734311 通过后再切 3/4）</li>
 * </ul>
 */
@Component
public class SmsOtpProperties {

    private final String productLabel;
    private final int templateParamCount;

    public SmsOtpProperties(
            @Value("${bgssai.sms.product-label:Short}") String productLabel,
            @Value("${bgssai.sms.template-param-count:2}") int templateParamCount) {
        String trimmed = productLabel == null ? "" : productLabel.trim();
        this.productLabel = trimmed.isEmpty() ? "Short" : trimmed;
        if (templateParamCount != 2 && templateParamCount != 3 && templateParamCount != 4) {
            throw new IllegalArgumentException(
                    "bgssai.sms.template-param-count must be 2, 3, or 4; got " + templateParamCount);
        }
        this.templateParamCount = templateParamCount;
    }

    public String getProductLabel() {
        return productLabel;
    }

    public int getTemplateParamCount() {
        return templateParamCount;
    }
}
