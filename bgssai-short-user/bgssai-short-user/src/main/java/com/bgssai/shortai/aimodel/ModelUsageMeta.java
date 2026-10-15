package com.bgssai.shortai.aimodel;

import java.util.Locale;
import java.util.Set;

/**
 * 用户可见模型区域。计费倍率只接受模型目录真实配置，不按模型名称推算。
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

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}

