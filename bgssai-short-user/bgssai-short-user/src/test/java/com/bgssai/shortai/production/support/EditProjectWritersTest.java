package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class EditProjectWritersTest {

    private EditTimeline timeline() {
        EditTimeline timeline = new EditTimeline();
        timeline.setProjectTitle("退婚三年后她成了首富");
        timeline.setEpisodeNo(14);
        timeline.setWidth(1080);
        timeline.setHeight(1920);
        timeline.setFps(30);

        EditTimeline.Clip first = new EditTimeline.Clip();
        first.setShotNo(1);
        first.setShotDesc("女主推门");
        first.setFileName("shot_1.mp4");
        first.setStartUs(0L);
        first.setDurationUs(4000000L);
        first.setWidth(1080);
        first.setHeight(1920);
        timeline.getClipList().add(first);

        EditTimeline.Clip second = new EditTimeline.Clip();
        second.setShotNo(3);
        second.setShotDesc("雨中对峙");
        second.setFileName("shot_3.mp4");
        second.setStartUs(4000000L);
        second.setDurationUs(3000000L);
        second.setWidth(1080);
        second.setHeight(1920);
        timeline.getClipList().add(second);

        EditTimeline.Voice voice = new EditTimeline.Voice();
        voice.setShotNo(1);
        voice.setFileName("voice_1_1.mp3");
        voice.setStartUs(0L);
        voice.setDurationUs(3500000L);
        timeline.getVoiceList().add(voice);

        EditTimeline.Line line = new EditTimeline.Line();
        line.setLineText("退婚书我签，但有个条件。");
        line.setStartUs(300000L);
        line.setDurationUs(3200000L);
        timeline.getLineList().add(line);

        EditTimeline.Missing missing = new EditTimeline.Missing();
        missing.setShotNo(2);
        missing.setShotDesc("会议室全景");
        timeline.getMissingList().add(missing);

        timeline.setBgmFileName("bgm.mp3");
        timeline.setBgmDurationUs(7000000L);
        timeline.setTotalUs(7000000L);
        return timeline;
    }

    @Test
    void jianyingDraftCarriesThreeTracksAndAbsoluteAssetPaths() {
        Map<String, Object> draft = JianyingDraftWriter.draftContent(timeline(),
                "C:/Users/demo/JianyingPro/Projects/com.lveditor.draft/退婚三年后她成了首富_第14集/assets");

        assertEquals(7000000L, draft.get("duration"));
        @SuppressWarnings("unchecked")
        Map<String, Object> canvas = (Map<String, Object>) draft.get("canvas_config");
        assertEquals(1080, canvas.get("width"));
        assertEquals(1920, canvas.get("height"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tracks = (List<Map<String, Object>>) draft.get("tracks");
        assertEquals(3, tracks.size());
        assertEquals("video", tracks.get(0).get("type"));
        assertEquals("audio", tracks.get(1).get("type"));
        assertEquals("text", tracks.get(2).get("type"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> videoSegments = (List<Map<String, Object>>) tracks.get(0).get("segments");
        assertEquals(2, videoSegments.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> secondRange = (Map<String, Object>) videoSegments.get(1).get("target_timerange");
        // 缺片的镜 2 不占时间轴：镜 3 直接接在镜 1 之后，起点就是 4 秒而不是 7 秒
        assertEquals(4000000L, secondRange.get("start"));
        assertEquals(3000000L, secondRange.get("duration"));

        @SuppressWarnings("unchecked")
        Map<String, Object> materials = (Map<String, Object>) draft.get("materials");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> videos = (List<Map<String, Object>>) materials.get("videos");
        assertEquals(2, videos.size());
        assertTrue(String.valueOf(videos.get(0).get("path")).endsWith("/assets/shot_1.mp4"));
        // 剪映按绝对路径找素材，写成相对路径会让草稿打开后全部离线
        assertTrue(String.valueOf(videos.get(0).get("path")).startsWith("C:/Users/demo/"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> audios = (List<Map<String, Object>>) materials.get("audios");
        // 一条配音加一条 BGM
        assertEquals(2, audios.size());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> texts = (List<Map<String, Object>>) materials.get("texts");
        assertEquals(1, texts.size());
        assertTrue(String.valueOf(texts.get(0).get("content")).contains("退婚书我签"));
    }

    @Test
    void jianyingMetaInfoRecordsDraftFoldPath() {
        Map<String, Object> meta = JianyingDraftWriter.draftMetaInfo(timeline(), "退婚三年后她成了首富_第14集",
                "C:/Users/demo/draft", "C:/Users/demo/draft/退婚三年后她成了首富_第14集");
        assertEquals("退婚三年后她成了首富_第14集", meta.get("draft_name"));
        assertEquals("C:/Users/demo/draft", meta.get("draft_root_path"));
        assertEquals("C:/Users/demo/draft/退婚三年后她成了首富_第14集", meta.get("draft_fold_path"));
        assertEquals(7000000L, meta.get("tm_duration"));
    }

    @Test
    void fcpxmlUsesRationalFrameTimeAndCarriesTitles() {
        String xml = FcpxmlWriter.write(timeline());
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(xml.contains("<fcpxml version=\"1.9\">"));
        // 4 秒在 30 帧下必须写成 120/30s；写成 4.0s 遇到非整秒时长会因浮点舍入错帧
        assertTrue(xml.contains("duration=\"120/30s\""));
        assertTrue(xml.contains("duration=\"90/30s\""));
        assertTrue(xml.contains("<media-rep kind=\"original-media\" src=\"./assets/shot_1.mp4\"/>"));
        assertTrue(xml.contains("lane=\"-1\""));
        assertTrue(xml.contains("audioRole=\"music\""));
        assertTrue(xml.contains("退婚书我签，但有个条件。"));
        assertFalse(xml.contains("shot_2"));
    }

    @Test
    void fcpxmlEscapesMarkupInDialogue() {
        EditTimeline timeline = timeline();
        timeline.getLineList().get(0).setLineText("你说的 <b>条件</b> 是什么 & 为什么");
        String xml = FcpxmlWriter.write(timeline);
        assertTrue(xml.contains("&lt;b&gt;条件&lt;/b&gt;"));
        assertTrue(xml.contains("&amp;"));
    }

    @Test
    void edlWritesSequentialEventsWithTimecode() {
        String edl = EdlWriter.writeEdl(timeline());
        assertTrue(edl.startsWith("TITLE: 退婚三年后她成了首富_E14"));
        assertTrue(edl.contains("FCM: NON-DROP FRAME"));
        assertTrue(edl.contains("001  AX       V     C        "
                + "00:00:00:00 00:00:04:00 00:00:00:00 00:00:04:00"));
        assertTrue(edl.contains("002  AX       V     C        "
                + "00:00:00:00 00:00:03:00 00:00:04:00 00:00:07:00"));
        assertTrue(edl.contains("* FROM CLIP NAME: shot_1.mp4"));
        assertTrue(edl.contains("* COMMENT: SHOT 3 雨中对峙"));
    }

    @Test
    void srtCarriesDialogueWithMillisecondTimestamps() {
        String srt = EdlWriter.writeSrt(timeline());
        assertTrue(srt.startsWith("1\r\n"));
        assertTrue(srt.contains("00:00:00,300 --> 00:00:03,500"));
        assertTrue(srt.contains("退婚书我签，但有个条件。"));
    }
}
