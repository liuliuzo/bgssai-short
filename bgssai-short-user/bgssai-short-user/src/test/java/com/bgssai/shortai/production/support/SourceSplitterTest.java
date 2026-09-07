package com.bgssai.shortai.production.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class SourceSplitterTest {

    @Test
    void splitsNovelByChineseChapterTitles() {
        String content = "第一章 退婚\n三年前那纸退婚书。\n\n第二章 归来\n机场落地那天。\n\n第三章 会议室\n谁批准的。";
        List<SourceSplitter.Chapter> chapters = SourceSplitter.split("NOVEL", content);
        assertEquals(3, chapters.size());
        assertEquals("第一章 退婚", chapters.get(0).getChapterTitle());
        assertEquals("第三章 会议室", chapters.get(2).getChapterTitle());
        assertTrue(chapters.get(1).getChapterText().contains("机场落地"));
    }

    @Test
    void keepsPrefaceBeforeFirstTitle() {
        String content = "楔子部分的正文，写在第一章之前。\n\n第一章 退婚\n正文一。\n\n第二章 归来\n正文二。";
        List<SourceSplitter.Chapter> chapters = SourceSplitter.split("NOVEL", content);
        // 第一处标题之前的正文不能丢，否则用户会发现导进来少了一段
        assertEquals(3, chapters.size());
        assertEquals("卷首", chapters.get(0).getChapterTitle());
        assertTrue(chapters.get(0).getChapterText().contains("楔子"));
    }

    @Test
    void splitsMarkdownHeadings() {
        String content = "# 第一幕\n内容一\n\n## 第二幕\n内容二";
        List<SourceSplitter.Chapter> chapters = SourceSplitter.split("NOVEL", content);
        assertEquals(2, chapters.size());
        assertEquals("第一幕", chapters.get(0).getChapterTitle());
        assertEquals("第二幕", chapters.get(1).getChapterTitle());
    }

    @Test
    void splitsScriptBySceneMarkers() {
        String content = "INT. 会议室 - 日\n沈知微推门。\n\nEXT. 停车场 - 夜\n雨很大。";
        List<SourceSplitter.Chapter> chapters = SourceSplitter.split("SCRIPT", content);
        assertEquals(2, chapters.size());
        assertTrue(chapters.get(0).getChapterTitle().startsWith("INT."));
        assertTrue(chapters.get(1).getChapterTitle().startsWith("EXT."));
    }

    @Test
    void splitsOutlineByEpisodeMarkers() {
        String content = "第1集 退婚\n梗概一\n\n第2集 归来\n梗概二\n\n第3集 夺权\n梗概三";
        List<SourceSplitter.Chapter> chapters = SourceSplitter.split("OUTLINE", content);
        assertEquals(3, chapters.size());
    }

    @Test
    void fallsBackToBlocksWhenNoTitleFound() {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            content.append("这一段没有任何章节标题，只是连续的正文内容，用来测试等长切块。".repeat(6));
            content.append("\n\n");
        }
        List<SourceSplitter.Chapter> chapters = SourceSplitter.split("NOVEL", content.toString());
        // 识别不到标题时不能把整本当一章：一章几十万字，按章选范围和送模型都没法做
        assertTrue(chapters.size() > 1, "expected fallback blocks, got " + chapters.size());
        for (SourceSplitter.Chapter chapter : chapters) {
            assertTrue(chapter.getChapterText().length() > 0);
        }
    }

    @Test
    void returnsEmptyForBlankContent() {
        assertTrue(SourceSplitter.split("NOVEL", "   ").isEmpty());
        assertTrue(SourceSplitter.split("NOVEL", null).isEmpty());
    }
}
