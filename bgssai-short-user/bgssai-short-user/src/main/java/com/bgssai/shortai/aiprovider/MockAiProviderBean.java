package com.bgssai.shortai.aiprovider;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.production.support.MockAiProvider;
import com.bgssai.shortai.production.support.ShortJson;

/**
 * provider_code=MOCK — local / no-key fallback wrapping static MockAiProvider.
 */
@Component
public class MockAiProviderBean implements AiProvider {

    public static final String CODE = "MOCK";

    private static final String[] COLORS = { "#2563eb", "#7c3aed", "#db2777", "#0f766e", "#b45309" };

    @Override
    public String providerCode() {
        return CODE;
    }

    @Override
    public AiProviderResult execute(AiProviderRequest request) {
        if (request == null || request.getAbilityType() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "ability_type required");
        }
        String ability = request.getAbilityType();
        String op = request.getOperation() == null ? "" : request.getOperation();
        if ("TEXT".equals(ability)) {
            return text(op, request);
        }
        if ("IMAGE".equals(ability) || "VIDEO".equals(ability) || "COMPOSE".equals(ability)) {
            return imageLike(op, request, ability);
        }
        if ("VOICE".equals(ability)) {
            String svg = MockAiProvider.svg("VOICE", ShortJson.text(request.getPrompt()), "#0f766e");
            return AiProviderResult.binary(svg.getBytes(StandardCharsets.UTF_8), "image/svg+xml");
        }
        throw new BizException(CommonErrorCode.RULE_REJECT, "MOCK unsupported ability: " + ability);
    }

    private AiProviderResult text(String op, AiProviderRequest request) {
        Map<String, Object> p = request.getParams();
        if ("OUTLINE".equals(op)) {
            List<Map<String, Object>> outline = MockAiProvider.outline(
                    ShortJson.text(p.get("title")),
                    ShortJson.text(p.get("pitch")),
                    ShortJson.integer(p.get("episode_count"), 24),
                    ShortJson.text(p.get("genre")));
            return AiProviderResult.text(ShortJson.toJson(outline));
        }
        if ("SCRIPT".equals(op)) {
            List<Map<String, Object>> script = MockAiProvider.script(
                    ShortJson.text(p.get("episode_title")),
                    ShortJson.text(p.get("synopsis")),
                    ShortJson.text(p.get("extra_prompt")));
            return AiProviderResult.text(ShortJson.toJson(script));
        }
        if ("REWRITE".equals(op)) {
            String rewritten = MockAiProvider.rewriteLine(
                    ShortJson.text(p.get("original")),
                    ShortJson.text(p.get("instruction")));
            return AiProviderResult.text(rewritten);
        }
        String prompt = request.getPrompt() == null ? "" : request.getPrompt();
        return AiProviderResult.text(prompt);
    }

    private AiProviderResult imageLike(String op, AiProviderRequest request, String ability) {
        Map<String, Object> p = request.getParams();
        String title = ShortJson.text(p.get("title"));
        if (title.isBlank()) {
            title = ShortJson.text(request.getPrompt());
        }
        String subtitle = ShortJson.text(p.get("subtitle"));
        int colorIdx = ShortJson.integer(p.get("color_index"), 0);
        String color = ShortJson.text(p.get("color"));
        if (color.isBlank()) {
            color = COLORS[Math.floorMod(colorIdx, COLORS.length)];
        }
        if ("COMPOSE".equals(ability) || "COMPOSE".equals(op)) {
            subtitle = subtitle.isBlank() ? "成片" : subtitle;
            color = "#111827";
        }
        String svg = MockAiProvider.svg(title, subtitle, color);
        AiProviderResult result = AiProviderResult.binary(svg.getBytes(StandardCharsets.UTF_8), "image/svg+xml");
        result.setWidth(1080);
        result.setHeight(1920);
        if (p.get("duration_sec") != null) {
            result.setDurationSec(ShortJson.integer(p.get("duration_sec"), 0));
        }
        return result;
    }
}
