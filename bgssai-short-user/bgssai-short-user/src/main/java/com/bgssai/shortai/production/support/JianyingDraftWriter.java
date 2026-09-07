package com.bgssai.shortai.production.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 把 {@link EditTimeline} 序列化成剪映桌面端草稿的两份 JSON。
 *
 * <p>剪映草稿是逆向出来的格式、且随版本演进，本类按剪映 5.x 与 6.x 共同的结构生成：
 * 主文件（6.0 及以上叫 draft_info.json，5.x 叫 draft_content.json）承载素材与轨道，
 * draft_meta_info.json 承载草稿在草稿库里的登记信息。两个版本内容结构一致，只有主文件名不同。
 *
 * <p>路径口径：剪映按草稿里写死的绝对路径找素材，所以素材 path 必须是用户本机的绝对路径，
 * 由调用方把 draft_dir 拼成 {draft_dir}/{草稿名}/assets 传进来。
 */
public final class JianyingDraftWriter {

    private static final long US_PER_SEC = 1000000L;

    private JianyingDraftWriter() {
    }

    /** 大写带横线的 UUID，与剪映自身生成的 id 形态一致。 */
    private static String uid() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    /**
     * 生成草稿主文件内容。assetsAbsPath 为素材目录的本机绝对路径（正斜杠形式）。
     */
    public static Map<String, Object> draftContent(EditTimeline timeline, String assetsAbsPath) {
        List<Map<String, Object>> videoMaterials = new ArrayList<>();
        List<Map<String, Object>> audioMaterials = new ArrayList<>();
        List<Map<String, Object>> textMaterials = new ArrayList<>();
        List<Map<String, Object>> canvasMaterials = new ArrayList<>();
        List<Map<String, Object>> speedMaterials = new ArrayList<>();
        List<Map<String, Object>> channelMaterials = new ArrayList<>();
        List<Map<String, Object>> vocalMaterials = new ArrayList<>();
        List<Map<String, Object>> placeholderMaterials = new ArrayList<>();
        List<Map<String, Object>> videoSegments = new ArrayList<>();
        List<Map<String, Object>> audioSegments = new ArrayList<>();
        List<Map<String, Object>> textSegments = new ArrayList<>();

        int renderIndex = 0;
        for (EditTimeline.Clip clip : timeline.getClipList()) {
            String materialId = uid();
            Map<String, Object> material = new LinkedHashMap<>();
            material.put("audio_fade", null);
            material.put("cartoon_path", "");
            material.put("category_id", "");
            material.put("category_name", "local");
            material.put("check_flag", 63487);
            material.put("crop", crop());
            material.put("crop_ratio", "free");
            material.put("crop_scale", 1.0);
            material.put("duration", clip.getDurationUs());
            material.put("extra_type_option", 0);
            material.put("formula_id", "");
            material.put("freeze", null);
            material.put("has_audio", true);
            material.put("height", clip.getHeight());
            material.put("id", materialId);
            material.put("intensifies_audio_path", "");
            material.put("intensifies_path", "");
            material.put("is_ai_generate_content", false);
            material.put("is_copyright", false);
            material.put("is_text_edit_overdub", false);
            material.put("is_unified_beauty_mode", false);
            material.put("local_id", "");
            material.put("local_material_id", "");
            material.put("material_id", "");
            material.put("material_name", clip.getFileName());
            material.put("material_url", "");
            material.put("matting", matting());
            material.put("media_path", "");
            material.put("object_locked", null);
            material.put("origin_material_id", "");
            material.put("path", assetsAbsPath + "/" + clip.getFileName());
            material.put("picture_from", "none");
            material.put("picture_set_category_id", "");
            material.put("picture_set_category_name", "");
            material.put("request_id", "");
            material.put("reverse_intensifies_path", "");
            material.put("reverse_path", "");
            material.put("smart_motion", null);
            material.put("source", 0);
            material.put("source_platform", 0);
            material.put("stable", null);
            material.put("team_id", "");
            material.put("type", "video");
            material.put("video_algorithm", videoAlgorithm());
            material.put("width", clip.getWidth());
            videoMaterials.add(material);

            String speedId = uid();
            speedMaterials.add(speed(speedId));
            String canvasId = uid();
            canvasMaterials.add(canvas(canvasId));
            String channelId = uid();
            channelMaterials.add(soundChannelMapping(channelId));
            String vocalId = uid();
            vocalMaterials.add(vocalSeparation(vocalId));
            String placeholderId = uid();
            placeholderMaterials.add(placeholder(placeholderId));

            Map<String, Object> segment = new LinkedHashMap<>();
            segment.put("cartoon", false);
            segment.put("clip", clipSettings(0.0, 0.0));
            segment.put("common_keyframes", new ArrayList<>());
            segment.put("enable_adjust", true);
            segment.put("enable_color_correct_adjust", false);
            segment.put("enable_color_curves", true);
            segment.put("enable_color_match_adjust", false);
            segment.put("enable_color_wheels", true);
            segment.put("enable_lut", true);
            segment.put("enable_smart_color_adjust", false);
            segment.put("extra_material_refs",
                    List.of(speedId, placeholderId, canvasId, channelId, vocalId));
            segment.put("group_id", "");
            segment.put("hdr_settings", hdrSettings());
            segment.put("id", uid());
            segment.put("intensifies_audio", false);
            segment.put("is_placeholder", false);
            segment.put("is_tone_modify", false);
            segment.put("keyframe_refs", new ArrayList<>());
            segment.put("last_nonzero_volume", 1.0);
            segment.put("material_id", materialId);
            segment.put("render_index", renderIndex);
            segment.put("responsive_layout", responsiveLayout());
            segment.put("reverse", false);
            segment.put("source_timerange", timerange(0L, clip.getDurationUs()));
            segment.put("speed", 1.0);
            segment.put("target_timerange", timerange(clip.getStartUs(), clip.getDurationUs()));
            segment.put("template_id", "");
            segment.put("template_scene", "default");
            segment.put("track_attribute", 0);
            segment.put("track_render_index", 0);
            segment.put("uniform_scale", uniformScale());
            segment.put("visible", true);
            segment.put("volume", 1.0);
            videoSegments.add(segment);
            renderIndex++;
        }

        List<EditTimeline.Voice> voices = new ArrayList<>(timeline.getVoiceList());
        if (timeline.getBgmFileName() != null && !timeline.getBgmFileName().isBlank()) {
            EditTimeline.Voice bgm = new EditTimeline.Voice();
            bgm.setFileName(timeline.getBgmFileName());
            bgm.setStartUs(0L);
            bgm.setDurationUs(timeline.getBgmDurationUs() > 0
                    ? timeline.getBgmDurationUs() : timeline.getTotalUs());
            voices.add(bgm);
        }
        for (EditTimeline.Voice voice : voices) {
            String materialId = uid();
            Map<String, Object> material = new LinkedHashMap<>();
            material.put("app_id", 0);
            material.put("category_id", "");
            material.put("category_name", "local");
            material.put("check_flag", 1);
            material.put("duration", voice.getDurationUs());
            material.put("effect_id", "");
            material.put("formula_id", "");
            material.put("id", materialId);
            material.put("intensifies_path", "");
            material.put("is_ai_clone_tone", false);
            material.put("is_text_edit_overdub", false);
            material.put("is_ugc", false);
            material.put("local_material_id", "");
            material.put("music_id", "");
            material.put("name", voice.getFileName());
            material.put("path", assetsAbsPath + "/" + voice.getFileName());
            material.put("query", "");
            material.put("request_id", "");
            material.put("resource_id", "");
            material.put("search_id", "");
            material.put("source_from", "");
            material.put("source_platform", 0);
            material.put("team_id", "");
            material.put("text_id", "");
            material.put("tone_category_id", "");
            material.put("tone_category_name", "");
            material.put("tone_effect_id", "");
            material.put("tone_effect_name", "");
            material.put("tone_speaker", "");
            material.put("tone_type", "");
            material.put("type", "extract_music");
            material.put("video_id", "");
            material.put("wave_points", new ArrayList<>());
            audioMaterials.add(material);

            String speedId = uid();
            speedMaterials.add(speed(speedId));
            String channelId = uid();
            channelMaterials.add(soundChannelMapping(channelId));
            String vocalId = uid();
            vocalMaterials.add(vocalSeparation(vocalId));

            Map<String, Object> segment = new LinkedHashMap<>();
            segment.put("cartoon", false);
            segment.put("clip", null);
            segment.put("common_keyframes", new ArrayList<>());
            segment.put("enable_adjust", false);
            segment.put("enable_color_curves", true);
            segment.put("enable_color_wheels", true);
            segment.put("enable_lut", false);
            segment.put("extra_material_refs", List.of(speedId, channelId, vocalId));
            segment.put("group_id", "");
            segment.put("id", uid());
            segment.put("intensifies_audio", false);
            segment.put("is_placeholder", false);
            segment.put("is_tone_modify", false);
            segment.put("keyframe_refs", new ArrayList<>());
            segment.put("last_nonzero_volume", 1.0);
            segment.put("material_id", materialId);
            segment.put("render_index", 0);
            segment.put("reverse", false);
            segment.put("source_timerange", timerange(0L, voice.getDurationUs()));
            segment.put("speed", 1.0);
            segment.put("target_timerange", timerange(voice.getStartUs(), voice.getDurationUs()));
            segment.put("template_id", "");
            segment.put("template_scene", "default");
            segment.put("track_attribute", 0);
            segment.put("track_render_index", 0);
            segment.put("uniform_scale", null);
            segment.put("visible", true);
            segment.put("volume", 1.0);
            audioSegments.add(segment);
        }

        boolean portrait = timeline.getHeight() > timeline.getWidth();
        for (EditTimeline.Line line : timeline.getLineList()) {
            String materialId = uid();
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("text", line.getLineText());
            List<Map<String, Object>> styles = new ArrayList<>();
            Map<String, Object> style = new LinkedHashMap<>();
            style.put("fill", Map.of("content", Map.of("solid",
                    Map.of("color", List.of(1.0, 1.0, 1.0)))));
            style.put("font", Map.of("id", "", "path", ""));
            style.put("range", List.of(0, line.getLineText() == null ? 0 : line.getLineText().length()));
            style.put("size", portrait ? 12.0 : 8.0);
            style.put("bold", true);
            styles.add(style);
            content.put("styles", styles);

            Map<String, Object> material = new LinkedHashMap<>();
            material.put("add_type", 0);
            material.put("alignment", 1);
            material.put("background_alpha", 1.0);
            material.put("background_color", "");
            material.put("bold_width", 0.0);
            material.put("border_alpha", 1.0);
            material.put("border_color", "#000000");
            material.put("border_width", 0.08);
            material.put("check_flag", 7);
            material.put("content", ShortJson.toJson(content));
            material.put("font_category_id", "");
            material.put("font_category_name", "");
            material.put("font_id", "");
            material.put("font_name", "");
            material.put("font_path", "");
            material.put("font_resource_id", "");
            material.put("font_size", portrait ? 12.0 : 8.0);
            material.put("font_source_platform", 0);
            material.put("font_team_id", "");
            material.put("font_title", "none");
            material.put("font_url", "");
            material.put("fonts", new ArrayList<>());
            material.put("has_shadow", true);
            material.put("id", materialId);
            material.put("initial_scale", 1.0);
            material.put("is_rich_text", false);
            material.put("italic_degree", 0);
            material.put("ktv_color", "");
            material.put("language", "");
            material.put("layer_weight", 1);
            material.put("letter_spacing", 0.0);
            material.put("line_feed", 1);
            material.put("line_max_width", portrait ? 0.82 : 0.6);
            material.put("line_spacing", 0.02);
            material.put("multi_language_current", "none");
            material.put("name", "");
            material.put("shadow_alpha", 0.7);
            material.put("shadow_angle", -45.0);
            material.put("shadow_color", "#000000");
            material.put("shadow_distance", 3.0);
            material.put("shadow_smoothing", 0.08);
            material.put("shape_clip_x", false);
            material.put("shape_clip_y", false);
            material.put("style_name", "");
            material.put("sub_type", 0);
            material.put("text_alpha", 1.0);
            material.put("text_color", "#FFFFFF");
            material.put("text_preset_resource_id", "");
            material.put("text_size", 30);
            material.put("text_to_audio_ids", new ArrayList<>());
            material.put("tts_auto_update", false);
            material.put("type", "text");
            material.put("typesetting", 0);
            material.put("underline", false);
            material.put("use_effect_default_color", true);
            material.put("words", Map.of("end_time", new ArrayList<>(),
                    "start_time", new ArrayList<>(), "text", new ArrayList<>()));
            textMaterials.add(material);

            Map<String, Object> segment = new LinkedHashMap<>();
            segment.put("cartoon", false);
            segment.put("clip", clipSettings(0.0, portrait ? -0.75 : -0.8));
            segment.put("common_keyframes", new ArrayList<>());
            segment.put("enable_adjust", false);
            segment.put("enable_color_curves", true);
            segment.put("enable_color_wheels", true);
            segment.put("enable_lut", false);
            segment.put("extra_material_refs", new ArrayList<>());
            segment.put("group_id", "");
            segment.put("id", uid());
            segment.put("intensifies_audio", false);
            segment.put("is_placeholder", false);
            segment.put("is_tone_modify", false);
            segment.put("keyframe_refs", new ArrayList<>());
            segment.put("last_nonzero_volume", 1.0);
            segment.put("material_id", materialId);
            segment.put("render_index", 14000);
            segment.put("responsive_layout", responsiveLayout());
            segment.put("reverse", false);
            segment.put("source_timerange", null);
            segment.put("speed", 1.0);
            segment.put("target_timerange", timerange(line.getStartUs(), line.getDurationUs()));
            segment.put("template_id", "");
            segment.put("template_scene", "default");
            segment.put("track_attribute", 0);
            segment.put("track_render_index", 0);
            segment.put("uniform_scale", uniformScale());
            segment.put("visible", true);
            segment.put("volume", 1.0);
            textSegments.add(segment);
        }

        List<Map<String, Object>> tracks = new ArrayList<>();
        tracks.add(track("video", "", videoSegments));
        if (!audioSegments.isEmpty()) {
            tracks.add(track("audio", "配音", audioSegments));
        }
        if (!textSegments.isEmpty()) {
            tracks.add(track("text", "字幕", textSegments));
        }

        Map<String, Object> materials = new LinkedHashMap<>();
        materials.put("audio_balances", new ArrayList<>());
        materials.put("audio_effects", new ArrayList<>());
        materials.put("audio_fades", new ArrayList<>());
        materials.put("audio_track_indexes", new ArrayList<>());
        materials.put("audios", audioMaterials);
        materials.put("beats", new ArrayList<>());
        materials.put("canvases", canvasMaterials);
        materials.put("chromas", new ArrayList<>());
        materials.put("color_curves", new ArrayList<>());
        materials.put("digital_humans", new ArrayList<>());
        materials.put("drafts", new ArrayList<>());
        materials.put("effects", new ArrayList<>());
        materials.put("flowers", new ArrayList<>());
        materials.put("green_screens", new ArrayList<>());
        materials.put("hsl", new ArrayList<>());
        materials.put("images", new ArrayList<>());
        materials.put("log_color_wheels", new ArrayList<>());
        materials.put("loudnesses", new ArrayList<>());
        materials.put("manual_deformations", new ArrayList<>());
        materials.put("masks", new ArrayList<>());
        materials.put("material_animations", new ArrayList<>());
        materials.put("material_colors", new ArrayList<>());
        materials.put("multi_language_refs", new ArrayList<>());
        materials.put("placeholders", placeholderMaterials);
        materials.put("plugin_effects", new ArrayList<>());
        materials.put("primary_color_wheels", new ArrayList<>());
        materials.put("realtime_denoises", new ArrayList<>());
        materials.put("shapes", new ArrayList<>());
        materials.put("smart_crops", new ArrayList<>());
        materials.put("smart_relights", new ArrayList<>());
        materials.put("sound_channel_mappings", channelMaterials);
        materials.put("speeds", speedMaterials);
        materials.put("stickers", new ArrayList<>());
        materials.put("tail_leaders", new ArrayList<>());
        materials.put("text_templates", new ArrayList<>());
        materials.put("texts", textMaterials);
        materials.put("time_marks", new ArrayList<>());
        materials.put("transitions", new ArrayList<>());
        materials.put("video_effects", new ArrayList<>());
        materials.put("video_trackings", new ArrayList<>());
        materials.put("videos", videoMaterials);
        materials.put("vocal_beautifys", new ArrayList<>());
        materials.put("vocal_separations", vocalMaterials);

        Map<String, Object> keyframes = new LinkedHashMap<>();
        keyframes.put("adjusts", new ArrayList<>());
        keyframes.put("audios", new ArrayList<>());
        keyframes.put("effects", new ArrayList<>());
        keyframes.put("filters", new ArrayList<>());
        keyframes.put("handwrites", new ArrayList<>());
        keyframes.put("stickers", new ArrayList<>());
        keyframes.put("texts", new ArrayList<>());
        keyframes.put("videos", new ArrayList<>());

        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("canvas_config", Map.of("height", timeline.getHeight(),
                "ratio", "original", "width", timeline.getWidth()));
        draft.put("color_space", 0);
        draft.put("config", draftConfig());
        draft.put("cover", null);
        draft.put("create_time", 0);
        draft.put("duration", timeline.getTotalUs());
        draft.put("extra_info", null);
        draft.put("fps", (double) timeline.getFps());
        draft.put("free_render_index_mode_on", false);
        draft.put("group_container", null);
        draft.put("id", uid());
        draft.put("keyframe_graph_list", new ArrayList<>());
        draft.put("keyframes", keyframes);
        draft.put("materials", materials);
        draft.put("mutable_config", null);
        draft.put("name", "");
        draft.put("new_version", "110.0.0");
        draft.put("relationships", new ArrayList<>());
        draft.put("render_index_track_mode_on", true);
        draft.put("retouch_cover", null);
        draft.put("source", "default");
        draft.put("static_cover_image_path", "");
        draft.put("time_marks", null);
        draft.put("tracks", tracks);
        draft.put("update_time", 0);
        draft.put("version", 360000);
        return draft;
    }

    /**
     * 生成草稿登记文件。draftFoldPath 为 {draft_dir}/{草稿名} 的绝对路径。
     */
    public static Map<String, Object> draftMetaInfo(EditTimeline timeline, String draftName,
            String draftRootPath, String draftFoldPath) {
        long nowUs = System.currentTimeMillis() * 1000L;
        List<Map<String, Object>> draftMaterials = new ArrayList<>();
        for (int type = 0; type <= 8; type++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", type);
            entry.put("value", new ArrayList<>());
            draftMaterials.add(entry);
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("cloud_package_completed_time", "");
        meta.put("draft_cloud_capcut_purchase_info", "");
        meta.put("draft_cloud_last_action_download", false);
        meta.put("draft_cloud_materials", new ArrayList<>());
        meta.put("draft_cloud_purchase_info", "");
        meta.put("draft_cloud_template_id", "");
        meta.put("draft_cloud_tutorial_info", "");
        meta.put("draft_cloud_videocut_purchase_info", "");
        meta.put("draft_cover", "draft_cover.jpg");
        meta.put("draft_deeplink_url", "");
        meta.put("draft_enterprise_info", Map.of("draft_enterprise_extra", "",
                "draft_enterprise_id", "", "draft_enterprise_name", "",
                "enterprise_material", new ArrayList<>()));
        meta.put("draft_fold_path", draftFoldPath);
        meta.put("draft_id", uid());
        meta.put("draft_is_ai_packaging_used", false);
        meta.put("draft_is_ai_shorts", false);
        meta.put("draft_is_ai_translate", false);
        meta.put("draft_is_article_video_draft", false);
        meta.put("draft_is_from_deeplink", "false");
        meta.put("draft_is_invisible", false);
        meta.put("draft_materials", draftMaterials);
        meta.put("draft_materials_copied_info", new ArrayList<>());
        meta.put("draft_name", draftName);
        meta.put("draft_new_version", "");
        meta.put("draft_removable_storage_device", "");
        meta.put("draft_root_path", draftRootPath);
        meta.put("draft_segment_extra_info", new ArrayList<>());
        meta.put("draft_timeline_materials_size_", 0);
        meta.put("draft_type", "");
        meta.put("tm_draft_cloud_completed", "");
        meta.put("tm_draft_cloud_modified", 0);
        meta.put("tm_draft_create", nowUs);
        meta.put("tm_draft_modified", nowUs);
        meta.put("tm_draft_removed", 0);
        meta.put("tm_duration", timeline.getTotalUs());
        return meta;
    }

    private static Map<String, Object> track(String type, String name,
            List<Map<String, Object>> segments) {
        Map<String, Object> track = new LinkedHashMap<>();
        track.put("attribute", 0);
        track.put("flag", 0);
        track.put("id", uid());
        track.put("is_default_name", name == null || name.isEmpty());
        track.put("name", name == null ? "" : name);
        track.put("segments", segments);
        track.put("type", type);
        return track;
    }

    private static Map<String, Object> timerange(long start, long duration) {
        Map<String, Object> range = new LinkedHashMap<>();
        range.put("duration", duration);
        range.put("start", start);
        return range;
    }

    private static Map<String, Object> crop() {
        Map<String, Object> crop = new LinkedHashMap<>();
        crop.put("lower_left_x", 0.0);
        crop.put("lower_left_y", 1.0);
        crop.put("lower_right_x", 1.0);
        crop.put("lower_right_y", 1.0);
        crop.put("upper_left_x", 0.0);
        crop.put("upper_left_y", 0.0);
        crop.put("upper_right_x", 1.0);
        crop.put("upper_right_y", 0.0);
        return crop;
    }

    private static Map<String, Object> matting() {
        Map<String, Object> matting = new LinkedHashMap<>();
        matting.put("flag", 0);
        matting.put("has_use_quick_brush", false);
        matting.put("has_use_quick_eraser", false);
        matting.put("interactiveTime", new ArrayList<>());
        matting.put("path", "");
        matting.put("strokes", new ArrayList<>());
        return matting;
    }

    private static Map<String, Object> videoAlgorithm() {
        Map<String, Object> algorithm = new LinkedHashMap<>();
        algorithm.put("algorithms", new ArrayList<>());
        algorithm.put("complement_frame_config", null);
        algorithm.put("deflicker", null);
        algorithm.put("gameplay_configs", new ArrayList<>());
        algorithm.put("motion_blur_config", null);
        algorithm.put("noise_reduction", null);
        algorithm.put("path", "");
        algorithm.put("quality_enhance", null);
        algorithm.put("time_range", null);
        return algorithm;
    }

    private static Map<String, Object> speed(String id) {
        Map<String, Object> speed = new LinkedHashMap<>();
        speed.put("curve_speed", null);
        speed.put("id", id);
        speed.put("mode", 0);
        speed.put("speed", 1.0);
        speed.put("type", "speed");
        return speed;
    }

    private static Map<String, Object> canvas(String id) {
        Map<String, Object> canvas = new LinkedHashMap<>();
        canvas.put("album_image", "");
        canvas.put("blur", 0.0);
        canvas.put("color", "");
        canvas.put("id", id);
        canvas.put("image", "");
        canvas.put("image_id", "");
        canvas.put("image_name", "");
        canvas.put("source_platform", 0);
        canvas.put("team_id", "");
        canvas.put("type", "canvas_color");
        return canvas;
    }

    private static Map<String, Object> soundChannelMapping(String id) {
        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("audio_channel_mapping", 0);
        mapping.put("id", id);
        mapping.put("is_config_open", false);
        mapping.put("type", "none");
        return mapping;
    }

    private static Map<String, Object> vocalSeparation(String id) {
        Map<String, Object> separation = new LinkedHashMap<>();
        separation.put("choice", 0);
        separation.put("id", id);
        separation.put("production_path", "");
        separation.put("removed_sounds", new ArrayList<>());
        separation.put("time_range", null);
        separation.put("type", "vocal_separation");
        return separation;
    }

    private static Map<String, Object> placeholder(String id) {
        Map<String, Object> holder = new LinkedHashMap<>();
        holder.put("error_path", "");
        holder.put("error_text", "");
        holder.put("id", id);
        holder.put("meta_type", "none");
        holder.put("res_path", "");
        holder.put("res_text", "");
        holder.put("type", "placeholder_info");
        return holder;
    }

    private static Map<String, Object> clipSettings(double transformX, double transformY) {
        Map<String, Object> clip = new LinkedHashMap<>();
        clip.put("alpha", 1.0);
        clip.put("flip", Map.of("horizontal", false, "vertical", false));
        clip.put("rotation", 0.0);
        clip.put("scale", Map.of("x", 1.0, "y", 1.0));
        clip.put("transform", Map.of("x", transformX, "y", transformY));
        return clip;
    }

    private static Map<String, Object> hdrSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("intensity", 1.0);
        settings.put("mode", 1);
        settings.put("nits", 1000);
        return settings;
    }

    private static Map<String, Object> responsiveLayout() {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("enable", false);
        layout.put("horizontal_pos_layout", 0);
        layout.put("size_layout", 0);
        layout.put("target_follow", "");
        layout.put("vertical_pos_layout", 0);
        return layout;
    }

    private static Map<String, Object> uniformScale() {
        Map<String, Object> scale = new LinkedHashMap<>();
        scale.put("on", true);
        scale.put("value", 1.0);
        return scale;
    }

    private static Map<String, Object> draftConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("adjust_max_index", 1);
        config.put("attachment_info", new ArrayList<>());
        config.put("combination_max_index", 1);
        config.put("export_range", null);
        config.put("extract_audio_last_index", 1);
        config.put("lyrics_recognition_id", "");
        config.put("lyrics_sync", true);
        config.put("lyrics_taskinfo", new ArrayList<>());
        config.put("maintrack_adsorb", true);
        config.put("material_save_mode", 0);
        config.put("multi_language_current", "none");
        config.put("multi_language_list", new ArrayList<>());
        config.put("multi_language_main", "none");
        config.put("multi_language_mode", "none");
        config.put("original_sound_last_index", 1);
        config.put("record_audio_last_index", 1);
        config.put("sticker_max_index", 1);
        config.put("subtitle_keywords_config", null);
        config.put("subtitle_recognition_id", "");
        config.put("subtitle_sync", true);
        config.put("subtitle_taskinfo", new ArrayList<>());
        config.put("system_font_list", new ArrayList<>());
        config.put("video_mute", false);
        config.put("zoom_info_params", null);
        return config;
    }

    /** 秒转微秒，供调用方组装时间轴时使用。 */
    public static long secToUs(int sec) {
        return sec * US_PER_SEC;
    }
}
