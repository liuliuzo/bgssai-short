package com.bgssai.shortai.login.sms;

/**
 * 腾讯云短信模板参数排列（统一委托 {@link TencentSmsTemplateParams}）。
 *
 * <ul>
 *   <li>2 → {@code [code, minutes]}</li>
 *   <li>3 → {@code [productLabel, code, minutes]}</li>
 *   <li>4 → {@code [productLabel, seq, code, minutes]}</li>
 * </ul>
 */
public final class SmsTemplateParams {

    private SmsTemplateParams() {
    }

    public static String[] build(int paramCount, String product, long seq, String code, int expireMinutes) {
        return TencentSmsTemplateParams.build(
                product, String.valueOf(seq), code, expireMinutes, paramCount, "Short");
    }
}
