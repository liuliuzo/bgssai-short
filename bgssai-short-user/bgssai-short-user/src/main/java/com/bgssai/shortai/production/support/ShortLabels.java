package com.bgssai.shortai.production.support;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ShortLabels {

    private static final Map<String, String> GENRE = new LinkedHashMap<>();
    private static final Map<String, String> WORK_STATUS = new LinkedHashMap<>();
    private static final Map<String, String> EPISODE_STAGE = new LinkedHashMap<>();
    private static final Map<String, String> REVIEW_STATUS = new LinkedHashMap<>();
    private static final Map<String, String> TARGET_TYPE = new LinkedHashMap<>();

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
        EPISODE_STAGE.put("NONE", "未开始");
        EPISODE_STAGE.put("DRAFT", "草稿");
        EPISODE_STAGE.put("READY", "已就绪");
        EPISODE_STAGE.put("GENERATING", "生成中");
        EPISODE_STAGE.put("FAILED", "失败");
        REVIEW_STATUS.put("NOT_SUBMITTED", "未提审");
        REVIEW_STATUS.put("PENDING", "审核中");
        REVIEW_STATUS.put("APPROVED", "已通过");
        REVIEW_STATUS.put("REJECTED", "已驳回");
        TARGET_TYPE.put("OUTLINE", "大纲");
        TARGET_TYPE.put("SCRIPT", "剧本");
        TARGET_TYPE.put("ADAPT_OUTLINE", "改编大纲");
        TARGET_TYPE.put("REWRITE", "改写");
        TARGET_TYPE.put("PORTRAIT", "定妆");
        TARGET_TYPE.put("SCENE_PORTRAIT", "场景卡");
        TARGET_TYPE.put("SHOT", "分镜");
        TARGET_TYPE.put("LAST_FRAME", "尾帧");
        TARGET_TYPE.put("COMPOSE", "成片合成");
        TARGET_TYPE.put("DUBBING", "配音");
        TARGET_TYPE.put("SUBTITLE_TRANSCRIBE", "字幕识别");
    }

    private ShortLabels() {
    }

    public static String genreName(String code) {
        return GENRE.getOrDefault(code, code);
    }

    public static String workStatusName(String code) {
        return WORK_STATUS.getOrDefault(code, code);
    }

    public static String episodeStageName(String code) {
        return EPISODE_STAGE.getOrDefault(code, code == null ? "-" : code);
    }

    public static String reviewStatusName(String code) {
        return REVIEW_STATUS.getOrDefault(code, code == null ? "-" : code);
    }

    public static String targetTypeName(String code) {
        return TARGET_TYPE.getOrDefault(code, code == null ? "-" : code);
    }
}
