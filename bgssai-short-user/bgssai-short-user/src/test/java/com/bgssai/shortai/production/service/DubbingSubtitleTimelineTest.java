package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.*;
import com.bgssai.shortai.model.*;
import com.bgssai.shortai.production.dto.FeatureDtos.SubtitleRegenReq;
import com.bgssai.shortai.production.support.SubtitleSrtBuilder;

class DubbingSubtitleTimelineTest {
    private final DubbingService service = new DubbingService();
    private final WorkAccessService access = mock(WorkAccessService.class);
    private final ShortShotMapper shots = mock(ShortShotMapper.class);
    private final ShortDubbingMapper dubbings = mock(ShortDubbingMapper.class);
    private final ShortSubtitleMapper subtitles = mock(ShortSubtitleMapper.class);
    private final ShortEpisodeMapper episodes = mock(ShortEpisodeMapper.class);
    private final AiTaskQueue queue = mock(AiTaskQueue.class);
    private final AiTaskService ai = mock(AiTaskService.class);
    private final List<ShortSubtitle> written = new ArrayList<>();
    private final ShortEpisode episode = new ShortEpisode();

    @BeforeEach void setUp() {
        ReflectionTestUtils.setField(service, "workAccessService", access);
        ReflectionTestUtils.setField(service, "shortShotMapper", shots);
        ReflectionTestUtils.setField(service, "shortDubbingMapper", dubbings);
        ReflectionTestUtils.setField(service, "shortSubtitleMapper", subtitles);
        ReflectionTestUtils.setField(service, "shortEpisodeMapper", episodes);
        ReflectionTestUtils.setField(service, "aiTaskQueue", queue);
        ReflectionTestUtils.setField(service, "aiTaskService", ai);
        episode.setEpisodeId("episode-1"); episode.setWorkId("work-1"); episode.setUserId("owner-1");
        when(access.requireEpisode("episode-1")).thenReturn(episode);
        ShortWork work = new ShortWork(); work.setWorkId("work-1"); work.setSubtitleStyleJson("{}");
        when(access.requireWork("work-1")).thenReturn(work);
        when(subtitles.insertSelective(any())).thenAnswer(call -> { written.add(call.getArgument(0)); return 1; });
        when(subtitles.selectByExample(any())).thenAnswer(call -> List.copyOf(written));
    }

    @Test void longChineseDialogueAtShotTenCannotOverlapShotElevenOrLoseLastShot() {
        List<ShortShot> sequence = new ArrayList<>();
        List<ShortDubbing> lines = new ArrayList<>();
        String longLine = "事情终于弄清楚我们一起去寻找医生。";
        for (int no = 1; no <= 12; no++) {
            sequence.add(shot(no, 5));
            lines.add(line(no, no, no == 10 ? longLine : "第" + no + "镜台词。"));
        }
        configure(sequence, lines);
        assertTrue(service.subtitleRegenerate(request()).isSuccess());
        List<ShortSubtitle> tenth = forShot(10);
        assertEquals(2, tenth.size());
        assertWindow(tenth, 45_000, 50_000);
        assertEquals(longLine, joined(tenth));
        assertWindow(forShot(11), 50_000, 55_000);
        assertWindow(forShot(12), 55_000, 60_000);
        for (int i = 0; i < written.size(); i++) assertEquals(i + 1, written.get(i).getSubtitleNo());
        String srt = SubtitleSrtBuilder.fromCues(SubtitleSrtBuilder.cuesForEpisode(written, sequence));
        assertTrue(srt.contains("00:00:47,500 --> 00:00:50,000"));
        assertTrue(srt.contains("00:00:55,000 --> 00:01:00,000"));
        verifyNoInteractions(queue, ai);
        verify(dubbings, never()).updateByExampleSelective(any(), any());
        verify(shots, never()).updateByExampleSelective(any(), any());
        assertEquals("audio-10", lines.get(9).getAudioAssetId());
        assertEquals(longLine, lines.get(9).getLineText());
    }

    @Test void everyChunkAcrossMultipleDubbingLinesSharesTheSameShotWindow() {
        configure(List.of(shot(1, 1)), List.of(line(1, 1, "甲".repeat(31)), line(1, 2, "第二句，保留标点。")));
        service.regenerateSubtitles(episode);
        assertEquals(4, written.size());
        assertEquals(List.of(0, 250, 500, 750), written.stream().map(ShortSubtitle::getStartMs).toList());
        assertWindow(written, 0, 1000);
        assertEquals("甲".repeat(31) + "第二句，保留标点。", joined(written));
    }

    @Test void cumulativeRoundingEndsExactlyAtShotEndAndEmptyShotsKeepTheirTime() {
        configure(List.of(shot(1, 3), shot(2, 5), shot(3, 2), shot(4, 1)),
                List.of(line(1, 1, " "), line(2, 2, "中".repeat(31)), line(4, 3, "尾镜。")));
        service.regenerateSubtitles(episode);
        List<ShortSubtitle> split = forShot(2);
        assertEquals(List.of(3000, 4666, 6333), split.stream().map(ShortSubtitle::getStartMs).toList());
        assertWindow(split, 3000, 8000);
        assertWindow(forShot(4), 10_000, 11_000);
    }

    @ParameterizedTest
    @ValueSource(strings = {"你好，医生！请等一等：这是完整台词。", "甲乙丙丁戊己庚辛壬癸甲乙丙丁𠮷医生。", "第一句……第二句；第三句？最后一句！"})
    void chinesePunctuationAndSupplementaryCharactersArePreserved(String text) {
        configure(List.of(shot(1, 5)), List.of(line(1, 1, text)));
        service.regenerateSubtitles(episode);
        assertEquals(text, joined(written));
        assertWindow(written, 0, 5000);
        for (ShortSubtitle subtitle : written) {
            String chunk = subtitle.getSubtitleText();
            assertTrue(chunk.codePointCount(0, chunk.length()) <= 15);
            assertFalse(Character.isLowSurrogate(chunk.charAt(0)));
            assertFalse(Character.isHighSurrogate(chunk.charAt(chunk.length() - 1)));
        }
    }

    @Test void asrSplitsStayInsideTheirShortWindowWithoutInvertedRanges() {
        ShortShot first = shot(1, 3); ShortShot current = shot(2, 5);
        ShortDubbing line = line(2, 2, "词".repeat(31));
        configure(List.of(first, current), List.of(line));
        when(shots.selectOneByExample(any())).thenReturn(current);
        service.finishTranscription(line, Map.of("sentences", List.of(
                Map.of("begin_ms", 4900, "end_ms", 8000, "text", line.getLineText()))));
        assertWindow(written, 7900, 8000);
        assertEquals(List.of(7900, 7933, 7966), written.stream().map(ShortSubtitle::getStartMs).toList());
        assertEquals(line.getLineText(), joined(written));
        verifyNoInteractions(queue, ai);
    }

    @ParameterizedTest @ValueSource(ints = {0, -1, Integer.MAX_VALUE})
    void invalidShotDurationDoesNotDeleteExistingSubtitles(int duration) {
        configure(List.of(shot(1, duration)), List.of(line(1, 1, "不改旧字幕")));
        assertThrows(BizException.class, () -> service.regenerateSubtitles(episode));
        verify(subtitles, never()).updateByExampleSelective(any(), any());
        verify(subtitles, never()).insertSelective(any());
    }

    @Test void impossibleDensityFailsBeforeReplacingExistingSubtitles() {
        configure(List.of(shot(1, 1)), List.of(line(1, 1, "字".repeat(15_001))));
        assertThrows(BizException.class, () -> service.regenerateSubtitles(episode));
        verify(subtitles, never()).updateByExampleSelective(any(), any());
        verify(subtitles, never()).insertSelective(any());
    }

    @Test void formalRegenerationRetainsOwnershipGateAndCannotStartPaidTasks() {
        when(access.requireEpisode("episode-1")).thenThrow(new ForbiddenException("episode not found"));
        assertThrows(ForbiddenException.class, () -> service.subtitleRegenerate(request()));
        verifyNoInteractions(shots, dubbings, subtitles, episodes, queue, ai);
    }

    private void configure(List<ShortShot> sequence, List<ShortDubbing> lines) {
        when(shots.selectByExample(any())).thenReturn(sequence);
        when(dubbings.selectByExample(any())).thenReturn(lines);
    }

    private SubtitleRegenReq request() {
        SubtitleRegenReq request = new SubtitleRegenReq(); request.setEpisodeId("episode-1"); return request;
    }

    private ShortShot shot(int no, int seconds) {
        ShortShot shot = new ShortShot(); shot.setShotId("shot-" + no); shot.setShotNo(no);
        shot.setEpisodeId("episode-1"); shot.setWorkId("work-1"); shot.setUserId("owner-1");
        shot.setShotDurationSec(seconds); return shot;
    }

    private ShortDubbing line(int shot, int no, String text) {
        ShortDubbing line = new ShortDubbing(); line.setDubbingId("dubbing-" + no); line.setLineNo(no);
        line.setShotId("shot-" + shot); line.setEpisodeId("episode-1"); line.setWorkId("work-1");
        line.setUserId("owner-1"); line.setLineText(text); line.setAudioAssetId("audio-" + no);
        line.setAudioDurationSec(5); line.setAlignStatus("ALIGNED"); return line;
    }

    private List<ShortSubtitle> forShot(int no) {
        return written.stream().filter(s -> s.getShotId().equals("shot-" + no)).toList();
    }

    private static String joined(List<ShortSubtitle> rows) {
        return rows.stream().map(ShortSubtitle::getSubtitleText).reduce("", String::concat);
    }

    private static void assertWindow(List<ShortSubtitle> rows, int begin, int end) {
        assertFalse(rows.isEmpty());
        assertEquals(begin, rows.get(0).getStartMs());
        int cursor = begin;
        for (ShortSubtitle row : rows) {
            assertEquals(cursor, row.getStartMs());
            assertTrue(row.getStartMs() < row.getEndMs());
            assertTrue(row.getEndMs() <= end);
            cursor = row.getEndMs();
        }
        assertEquals(end, cursor);
    }
}
