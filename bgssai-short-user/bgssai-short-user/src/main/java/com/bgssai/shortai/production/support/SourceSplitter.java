package com.bgssai.shortai.production.support;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把导入的原文切成章节。
 *
 * <p>切分只是给用户一个起点，不是最终答案：网文的章节标题写法五花八门（有人用「第一章」，
 * 有人只用空行，有人两者都不用），任何正则都会漏。因此本类的产出一律是**可编辑**的，
 * 前端允许合并、拆分与改标题——没有纠正入口的自动分章，等于逼用户回去改原文再传一遍。
 *
 * <p>识别不到章节标题时退回按段落聚合成等长块，而不是把整本当成一章：一章几十万字，
 * 后续按章选范围、按章送模型都没法做。
 */
public final class SourceSplitter {

    /** 识别不到标题时，每块目标字数。 */
    private static final int FALLBACK_BLOCK_CHARS = 3000;

    private static final Pattern NOVEL_TITLE = Pattern.compile(
            "^\\s*(?:第\\s*[0-9零一二三四五六七八九十百千万]+\\s*[章节回幕篇]|"
                    + "Chapter\\s+\\d+|CHAPTER\\s+\\d+|#{1,3}\\s+\\S).*$");

    private static final Pattern SCRIPT_TITLE = Pattern.compile(
            "^\\s*(?:第\\s*[0-9零一二三四五六七八九十百千万]+\\s*场|场景\\s*[0-9]+|"
                    + "INT\\.|EXT\\.|内景|外景|#{1,3}\\s+\\S).*$");

    private static final Pattern OUTLINE_TITLE = Pattern.compile(
            "^\\s*(?:第\\s*[0-9零一二三四五六七八九十百千万]+\\s*集|EP\\s*[0-9]+|E[0-9]+\\b|"
                    + "#{1,3}\\s+\\S).*$");

    private SourceSplitter() {
    }

    /** 一个切出来的章节：标题加正文。 */
    public static class Chapter {
        private String chapterTitle;
        private String chapterText;

        public Chapter(String chapterTitle, String chapterText) {
            this.chapterTitle = chapterTitle;
            this.chapterText = chapterText;
        }

        public String getChapterTitle() {
            return chapterTitle;
        }

        public void setChapterTitle(String chapterTitle) {
            this.chapterTitle = chapterTitle;
        }

        public String getChapterText() {
            return chapterText;
        }

        public void setChapterText(String chapterText) {
            this.chapterText = chapterText;
        }
    }

    public static List<Chapter> split(String sourceType, String content) {
        List<Chapter> chapters = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return chapters;
        }
        Pattern pattern = NOVEL_TITLE;
        if ("SCRIPT".equals(sourceType)) {
            pattern = SCRIPT_TITLE;
        } else if ("OUTLINE".equals(sourceType)) {
            pattern = OUTLINE_TITLE;
        }
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);

        List<Integer> titleLines = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.length() > 60) {
                continue;
            }
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                titleLines.add(i);
            }
        }

        // 只匹配到一处标题说明这套写法在本文里不成立，按等长块切更有用
        if (titleLines.size() < 2) {
            StringBuilder block = new StringBuilder();
            int index = 1;
            for (String line : lines) {
                block.append(line).append('\n');
                if (block.length() >= FALLBACK_BLOCK_CHARS && line.trim().isEmpty()) {
                    chapters.add(new Chapter("第 " + index + " 段", block.toString().trim()));
                    block.setLength(0);
                    index++;
                }
            }
            if (block.toString().trim().length() > 0) {
                chapters.add(new Chapter("第 " + index + " 段", block.toString().trim()));
            }
            return chapters;
        }

        for (int i = 0; i < titleLines.size(); i++) {
            int start = titleLines.get(i);
            int end = i + 1 < titleLines.size() ? titleLines.get(i + 1) : lines.length;
            String title = lines[start].trim().replaceFirst("^#{1,3}\\s+", "");
            StringBuilder text = new StringBuilder();
            for (int line = start + 1; line < end; line++) {
                text.append(lines[line]).append('\n');
            }
            chapters.add(new Chapter(title, text.toString().trim()));
        }
        // 第一处标题之前还有正文时（序、楔子等）补成第一章，不能丢
        int firstTitle = titleLines.get(0);
        if (firstTitle > 0) {
            StringBuilder preface = new StringBuilder();
            for (int line = 0; line < firstTitle; line++) {
                preface.append(lines[line]).append('\n');
            }
            if (!preface.toString().isBlank()) {
                chapters.add(0, new Chapter("卷首", preface.toString().trim()));
            }
        }
        return chapters;
    }
}
