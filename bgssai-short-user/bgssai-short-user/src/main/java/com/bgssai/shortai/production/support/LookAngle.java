package com.bgssai.shortai.production.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;

/**
 * 定妆四面的角度口径。正脸、前面、背面、侧面各占一格。
 * 站姿、坐姿这类补充说明不能顶掉缺的那一面，也不能把没写角度的图全部塞进正脸。
 */
public final class LookAngle {

    public static final List<String> BODY = List.of("FRONT", "BACK", "SIDE");

    private LookAngle() {
    }

    public static String canonical(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if ("FRONT_FACE".equals(value) || "FRONT".equals(value) || "BACK".equals(value) || "SIDE".equals(value)) {
            return value;
        }
        return null;
    }

    /** One clear angle label in the caption. Two different labels means we do not guess. */
    public static String classify(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String raw = text.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<>();
        if (raw.contains("正脸") || lower.contains("front face") || lower.contains("front_face")) {
            hits.add("FRONT_FACE");
        }
        if (raw.contains("前面") || lower.contains("front view")) {
            hits.add("FRONT");
        }
        if (raw.contains("背面") || raw.contains("背影") || raw.contains("后背") || lower.contains("back view")) {
            hits.add("BACK");
        }
        if (raw.contains("侧面") || raw.contains("侧身") || lower.contains("side view")) {
            hits.add("SIDE");
        }
        return hits.size() == 1 ? hits.get(0) : null;
    }

    public static boolean poseExtra(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.contains("站姿") || text.contains("坐姿") || text.contains("行走") || text.contains("走动");
    }

    /**
     * The slot the caller picked wins. A blank slot follows the caption.
     * A caption that names 背面/侧面/前面 corrects a photo that was dumped into 正脸
     * only because no angle was sent.
     */
    public static String resolve(String explicit, String caption) {
        if (explicit != null && !explicit.isBlank() && canonical(explicit) == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "定妆角度只能是正脸、前面、背面、侧面");
        }
        String field = canonical(explicit);
        String labeled = classify(caption);
        if (field == null) {
            return labeled;
        }
        if (labeled == null || labeled.equals(field)) {
            return field;
        }
        if ("FRONT_FACE".equals(field) && (caption == null || !caption.contains("正脸"))) {
            return labeled;
        }
        return field;
    }

    /** Which body angles to generate. Front face is the reference and is never a target. */
    public static List<String> plan(Set<String> filled, String requested, boolean replace) {
        if (requested != null && !requested.isBlank() && canonical(requested) == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "定妆角度只能是正脸、前面、背面、侧面");
        }
        String one = canonical(requested);
        if (one != null) {
            if ("FRONT_FACE".equals(one)) {
                throw new BizException(CommonErrorCode.RULE_REJECT, "正脸用来当参考，不用再生成一张正脸");
            }
            if (!replace && filled != null && filled.contains(one)) {
                throw new BizException(CommonErrorCode.RULE_REJECT, "这一面已经有照片。要按正脸重画，请点重做。");
            }
            return List.of(one);
        }
        List<String> missing = new ArrayList<>();
        for (String angle : BODY) {
            if (filled == null || !filled.contains(angle)) {
                missing.add(angle);
            }
        }
        if (missing.isEmpty()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "缺的面都有了。要重画某一面，请点「按正脸重做」。");
        }
        return List.copyOf(missing);
    }
}
