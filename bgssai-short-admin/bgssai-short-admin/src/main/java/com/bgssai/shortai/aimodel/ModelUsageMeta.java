package com.bgssai.shortai.aimodel;

import java.util.Locale;
import java.util.Set;

/**
 * 用户可见的模型区域与计点。区域按厂商产地（国内/国外），不计接入节点。
 * 计点是平台报价：1 点 = Flash 档约 1000 token；不向用户展示与原厂价的差额。
 */
public final class ModelUsageMeta {

    public static final String CN = "cn";
    public static final String INTL = "intl";

    private static final Set<String> INTL_PROVIDERS = Set.of(
            "openai", "anthropic", "claude", "google", "google_gemini", "gemini",
            "chatgpt", "spacexai", "xai", "grok", "cursor", "composer"
    );

    private ModelUsageMeta() {
    }

    public static String regionCode(String provider) {
        return regionCode(provider, null);
    }

    public static String regionCode(String provider, String modelId) {
        String p = norm(provider);
        if (INTL_PROVIDERS.contains(p)) {
            return INTL;
        }
        String m = norm(modelId);
        if (m.startsWith("gpt-") || m.startsWith("claude") || m.startsWith("gemini")
                || m.startsWith("grok") || m.contains("composer") || m.startsWith("o1")
                || m.startsWith("o3") || m.startsWith("o4")) {
            return INTL;
        }
        return CN;
    }

    public static String regionLabel(String regionOrProvider) {
        if (regionOrProvider == null || regionOrProvider.isBlank()) {
            return "国内";
        }
        String n = regionOrProvider.trim().toLowerCase(Locale.ROOT);
        if (INTL.equals(n) || "国外".equals(n) || INTL_PROVIDERS.contains(n)) {
            return "国外";
        }
        if (CN.equals(n) || "国内".equals(n)) {
            return "国内";
        }
        return INTL.equals(regionCode(regionOrProvider)) ? "国外" : "国内";
    }

    public static int tokenMultiplier(String modelId, String provider) {
        String s = norm(modelId);
        if (s.contains("fable")) {
            return 35;
        }
        if (s.contains("gpt-5.5")) {
            return 20;
        }
        if (s.contains("opus")) {
            return 18;
        }
        if (s.contains("sol") && s.contains("gpt")) {
            return 14;
        }
        if (s.contains("kimi-k3")) {
            return 10;
        }
        if (s.contains("terra")) {
            return 8;
        }
        if (s.contains("composer")) {
            return 8;
        }
        if (s.contains("sonnet")) {
            return 7;
        }
        if (s.contains("grok")) {
            return 5;
        }
        if (s.contains("gpt-5") && s.contains("mini")) {
            return 4;
        }
        if (s.contains("gpt-5") && s.contains("nano")) {
            return 3;
        }
        if (s.startsWith("gpt-5")) {
            return 16;
        }
        if (s.contains("gemini-3.7")) {
            return 3;
        }
        if (s.contains("deepseek-v4-pro") || s.contains("deepseek-r1")) {
            return 3;
        }
        if (s.startsWith("glm-5.3") && !s.contains("flash")) {
            return 3;
        }
        if (s.contains("kimi-k2.7")) {
            return 3;
        }
        if (s.startsWith("hy4") || (s.contains("qwen") && (s.contains("plus") || s.contains("max")))) {
            return 2;
        }
        if (s.contains("luna") || s.contains("flash") || s.contains("minimax-m3") || s.contains("turbo")) {
            return 1;
        }
        return INTL.equals(regionCode(provider, modelId)) ? 5 : 1;
    }

    public static String usageHint(int tokenMultiplier) {
        int w = Math.max(tokenMultiplier, 1);
        return w == 1 ? "按实际 token 计入额度" : ("高级模型：实际 token × " + w + " 计入额度");
    }

    public static int usageTokens(long tokens, int tokenMultiplier) {
        if (tokens <= 0) {
            return 0;
        }
        int w = Math.max(tokenMultiplier, 1);
        // 直接相乘可能溢出 int：一次超长对话 × 20 倍权重就够呛，封顶到 MAX_VALUE
        return (int) Math.min(Integer.MAX_VALUE, tokens * (long) w);
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}

