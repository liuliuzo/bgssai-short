package com.bgssai.shortai.login.sms;

import java.security.SecureRandom;

/**
 * 腾讯云登录验证码短信 TemplateParamSet 构建。
 *
 * <p>推荐模板：您正在登录{1}，序号{2}，验证码{3}，{4}分钟内有效
 *
 * <ul>
 *   <li>2 → [验证码, 分钟]</li>
 *   <li>3 → [产品标签, 验证码, 分钟]</li>
 *   <li>4 → [产品标签, 序号, 验证码, 分钟]</li>
 * </ul>
 */
public final class TencentSmsTemplateParams {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TencentSmsTemplateParams() {
    }

    /** 生成四位发码序号（0000–9999）。 */
    public static String nextSeq() {
        return String.format("%04d", RANDOM.nextInt(10000));
    }

    /**
     * @param productLabel  产品标签；blank 时用 {@code fallbackLabel}
     * @param seq           发码序号
     * @param code          验证码明文（仅进模板，调用方勿打日志）
     * @param expireMinutes 有效分钟数（≥1）
     * @param paramCount    2 / 3 / 4；其它值回落 2
     * @param fallbackLabel 配置为空时的产品名回落
     */
    public static String[] build(String productLabel,
                                 String seq,
                                 String code,
                                 int expireMinutes,
                                 int paramCount,
                                 String fallbackLabel) {
        String label = (productLabel == null || productLabel.isBlank())
                ? (fallbackLabel == null || fallbackLabel.isBlank() ? "Short" : fallbackLabel.trim())
                : productLabel.trim();
        String safeSeq = (seq == null || seq.isBlank()) ? nextSeq() : seq.trim();
        String safeCode = code == null ? "" : code.trim();
        int minutes = Math.max(1, expireMinutes);
        int count = (paramCount == 3 || paramCount == 4) ? paramCount : 2;

        if (count == 4) {
            return new String[] { label, safeSeq, safeCode, String.valueOf(minutes) };
        }
        if (count == 3) {
            return new String[] { label, safeCode, String.valueOf(minutes) };
        }
        return new String[] { safeCode, String.valueOf(minutes) };
    }
}
