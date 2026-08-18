package com.bgssai.shortai.production.support;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ShortLabels {

    private static final Map<String, String> GENRE = new LinkedHashMap<>();
    private static final Map<String, String> WORK_STATUS = new LinkedHashMap<>();

    static {
        GENRE.put("REVENGE", "复仇逆袭");
        GENRE.put("CEO", "豪门总裁");
        GENRE.put("TIME_TRAVEL", "穿越重生");
        GENRE.put("SWEET", "甜宠");
        GENRE.put("SUSPENSE", "悬疑");
        GENRE.put("FANTASY", "玄幻");
        WORK_STATUS.put("DRAFT", "草稿");
        WORK_STATUS.put("SCRIPTING", "剧本中");
        WORK_STATUS.put("PRODUCING", "制作中");
        WORK_STATUS.put("COMPOSING", "合成中");
        WORK_STATUS.put("REVIEWING", "审核中");
        WORK_STATUS.put("COMPLETED", "已完成");
        WORK_STATUS.put("ARCHIVED", "已归档");
    }

    private ShortLabels() {
    }

    public static String genreName(String code) {
        return GENRE.getOrDefault(code, code);
    }

    public static String workStatusName(String code) {
        return WORK_STATUS.getOrDefault(code, code);
    }
}
