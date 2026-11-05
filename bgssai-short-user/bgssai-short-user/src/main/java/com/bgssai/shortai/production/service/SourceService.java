package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.aimodel.ProductModelAlias;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortSourceChapterMapper;
import com.bgssai.shortai.mapper.ShortSourceDocMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortSourceChapter;
import com.bgssai.shortai.model.ShortSourceChapterExample;
import com.bgssai.shortai.model.ShortSourceDoc;
import com.bgssai.shortai.model.ShortSourceDocExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.SourceDtos.SourceAdaptReq;
import com.bgssai.shortai.production.dto.SourceDtos.SourceChaptersSaveReq;
import com.bgssai.shortai.production.dto.SourceDtos.SourceDeleteReq;
import com.bgssai.shortai.production.dto.SourceDtos.SourceImportReq;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.SourceSplitter;
import com.bgssai.shortai.production.support.TaskPrices;
import com.github.pagehelper.PageHelper;

/**
 * 原著导入与改编：把小说全文、成品剧本、分集大纲收进来，自动分章，再按章改编成分集大纲。
 *
 * <p>产品上补的是「第一公里」：此前一部作品只有「输入一句话梗概」一个入口，
 * 手里握着网文 IP、写好剧本、做完大纲的人全被挡在外面，而这批人产量最高、也最舍得付费。
 *
 * <p>两条不能松的口径：权利确认是硬前置（导入他人作品改编再发行是本产品最现实的法律风险，
 * 至少要把责任边界与留痕做清楚）；改编稿与原创稿走同一套节奏体检（小说改编最典型的翻车
 * 就是照搬小说节奏——铺垫长、对白书面、一集讲不完一个爽点，给改编开口子，
 * 这个功能就变成批量生产小说腔短剧的机器）。
 */
@Service
public class SourceService {

    private static final Logger log = LoggerFactory.getLogger(SourceService.class);

    private static final int MAX_SOURCE_CHARS = 1000000;

    private static final int CHARS_PER_BILLING_UNIT = 10000;

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private AiTaskQueue aiTaskQueue;
    @Autowired
    private AiModelResolveService aiModelResolveService;
    @Autowired
    private ShortSourceDocMapper shortSourceDocMapper;
    @Autowired
    private ShortSourceChapterMapper shortSourceChapterMapper;

    public ApiResponse importSource(SourceImportReq req) {
        if (req == null || req.getWorkId() == null || req.getWorkId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "work_id required");
        }
        String sourceType = req.getSourceType() == null ? "" : req.getSourceType().trim().toUpperCase();
        if (!"NOVEL".equals(sourceType) && !"SCRIPT".equals(sourceType) && !"OUTLINE".equals(sourceType)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM,
                    "source_type must be NOVEL, SCRIPT or OUTLINE");
        }
        if (!Integer.valueOf(1).equals(req.getRightsConfirmFlag())) {
            throw new BizException(CommonErrorCode.INVALID_PARAM,
                    "请先勾选「我确认对本内容拥有合法使用权或已获授权」");
        }
        String content = req.getSourceContent() == null ? "" : req.getSourceContent();
        if (content.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "source_content required");
        }
        if (content.length() > MAX_SOURCE_CHARS) {
            throw new BizException(CommonErrorCode.INVALID_PARAM,
                    "原文 " + content.length() + " 字符，超过上限 " + MAX_SOURCE_CHARS + " 字符");
        }
        ShortWork work = workAccessService.requireWork(req.getWorkId());
        List<SourceSplitter.Chapter> chapters = SourceSplitter.split(sourceType, content);
        if (chapters.isEmpty()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "原文切不出任何章节，请检查内容");
        }

        Date now = new Date();
        ShortSourceDoc doc = new ShortSourceDoc();
        doc.setSourceId(ShortIds.id("src-"));
        doc.setWorkId(work.getWorkId());
        doc.setUserId(work.getUserId());
        doc.setSourceType(sourceType);
        String title = req.getSourceTitle() == null || req.getSourceTitle().isBlank()
                ? work.getWorkTitle() : req.getSourceTitle().trim();
        doc.setSourceTitle(title);
        doc.setCharCount(content.length());
        doc.setChapterCount(chapters.size());
        doc.setRightsConfirmFlag(1);
        doc.setRightsConfirmTime(now);
        doc.setImportStatus("IMPORTED");
        doc.setAdaptedFlag(0);
        doc.setDeletedFlag(0);
        doc.setCreatedTime(now);
        shortSourceDocMapper.insertSelective(doc);

        int chapterNo = 1;
        for (SourceSplitter.Chapter chapter : chapters) {
            ShortSourceChapter row = new ShortSourceChapter();
            row.setChapterId(ShortIds.id("ch-"));
            row.setSourceId(doc.getSourceId());
            row.setWorkId(work.getWorkId());
            row.setUserId(work.getUserId());
            row.setChapterNo(chapterNo);
            row.setChapterTitle(chapter.getChapterTitle());
            row.setChapterText(chapter.getChapterText());
            row.setCharCount(chapter.getChapterText() == null ? 0 : chapter.getChapterText().length());
            row.setDeletedFlag(0);
            row.setCreatedTime(now);
            shortSourceChapterMapper.insertSelective(row);
            chapterNo++;
        }
        log.info("source imported sourceId={} type={} chars={} chapters={}",
                doc.getSourceId(), sourceType, content.length(), chapters.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source_id", doc.getSourceId());
        result.put("chapter_count", chapters.size());
        result.put("char_count", content.length());
        return Responses.ok(result);
    }

    public PageResponse list(String workId, Integer pageNum, Integer pageSize) {
        ShortWork work = workAccessService.requireWork(workId);
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 || pageSize > 100 ? 20 : pageSize;
        ShortSourceDocExample ex = new ShortSourceDocExample();
        ex.createCriteria().andWorkIdEqualTo(work.getWorkId()).andUserIdEqualTo(work.getUserId())
                .andDeletedFlagEqualTo(0);
        ex.setOrderByClause("created_time desc, id desc");
        PageHelper.startPage(safePageNum, safePageSize);
        List<ShortSourceDoc> rows = shortSourceDocMapper.selectByExample(ex);
        long total = ((com.github.pagehelper.Page<ShortSourceDoc>) rows).getTotal();
        List<Map<String, Object>> items = new ArrayList<>();
        for (ShortSourceDoc row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("source_id", row.getSourceId());
            item.put("source_type", row.getSourceType());
            item.put("source_title", row.getSourceTitle());
            item.put("char_count", row.getCharCount());
            item.put("chapter_count", row.getChapterCount());
            item.put("import_status", row.getImportStatus());
            item.put("adapted_flag", row.getAdaptedFlag());
            item.put("created_time", row.getCreatedTime());
            items.add(item);
        }
        PageResponse response = new PageResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setMessage("OK");
        response.setResult(items);
        response.setPageNum(safePageNum);
        response.setPageSize(safePageSize);
        response.setTotalSize(total);
        return response;
    }

    public PageResponse detail(String sourceId, Integer pageNum, Integer pageSize) {
        ShortSourceDoc doc = requireSource(sourceId);
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 || pageSize > 200 ? 50 : pageSize;
        ShortSourceChapterExample ex = new ShortSourceChapterExample();
        ex.createCriteria().andSourceIdEqualTo(doc.getSourceId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("chapter_no asc");
        PageHelper.startPage(safePageNum, safePageSize);
        List<ShortSourceChapter> rows = shortSourceChapterMapper.selectByExample(ex);
        long total = ((com.github.pagehelper.Page<ShortSourceChapter>) rows).getTotal();
        List<Map<String, Object>> items = new ArrayList<>();
        for (ShortSourceChapter row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("chapter_id", row.getChapterId());
            item.put("chapter_no", row.getChapterNo());
            item.put("chapter_title", row.getChapterTitle());
            item.put("char_count", row.getCharCount());
            item.put("chapter_text", row.getChapterText());
            items.add(item);
        }
        PageResponse response = new PageResponse();
        response.setCode("0");
        response.setSuccess(true);
        response.setMessage("OK");
        response.setResult(items);
        response.setPageNum(safePageNum);
        response.setPageSize(safePageSize);
        response.setTotalSize(total);
        return response;
    }

    /**
     * 整份替换分章结果。自动分章在网文里出错是常态，没有这个入口就等于逼用户回去改原文再传一遍。
     */
    public ApiResponse chaptersSave(SourceChaptersSaveReq req) {
        if (req == null || req.getChapterList() == null || req.getChapterList().isEmpty()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "chapter_list required");
        }
        ShortSourceDoc doc = requireSource(req.getSourceId());
        Date now = new Date();
        ShortSourceChapter clear = new ShortSourceChapter();
        clear.setDeletedFlag(1);
        clear.setUpdatedTime(now);
        ShortSourceChapterExample clearEx = new ShortSourceChapterExample();
        clearEx.createCriteria().andSourceIdEqualTo(doc.getSourceId()).andDeletedFlagEqualTo(0);
        shortSourceChapterMapper.updateByExampleSelective(clear, clearEx);

        int chapterNo = 1;
        int totalChars = 0;
        for (SourceChaptersSaveReq.ChapterItem item : req.getChapterList()) {
            String text = item.getChapterText() == null ? "" : item.getChapterText();
            ShortSourceChapter row = new ShortSourceChapter();
            row.setChapterId(ShortIds.id("ch-"));
            row.setSourceId(doc.getSourceId());
            row.setWorkId(doc.getWorkId());
            row.setUserId(doc.getUserId());
            row.setChapterNo(chapterNo);
            row.setChapterTitle(item.getChapterTitle());
            row.setChapterText(text);
            row.setCharCount(text.length());
            row.setDeletedFlag(0);
            row.setCreatedTime(now);
            shortSourceChapterMapper.insertSelective(row);
            totalChars += text.length();
            chapterNo++;
        }
        ShortSourceDoc patch = new ShortSourceDoc();
        patch.setChapterCount(chapterNo - 1);
        patch.setCharCount(totalChars);
        patch.setUpdatedTime(now);
        ShortSourceDocExample up = new ShortSourceDocExample();
        up.createCriteria().andIdEqualTo(doc.getId());
        shortSourceDocMapper.updateByExampleSelective(patch, up);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source_id", doc.getSourceId());
        result.put("chapter_count", chapterNo - 1);
        result.put("char_count", totalChars);
        return Responses.ok(result);
    }

    public ApiResponse adaptEstimate(SourceAdaptReq req) {
        ShortSourceDoc doc = requireSource(req == null ? null : req.getSourceId());
        List<ShortSourceChapter> chapters = rangeChapters(doc, req);
        int charCount = 0;
        for (ShortSourceChapter chapter : chapters) {
            charCount += chapter.getCharCount() == null ? 0 : chapter.getCharCount();
        }
        AiModelConfig model = aiModelResolveService.resolve("TEXT", req.getModelCode());
        int usage = Math.max(1, (charCount + CHARS_PER_BILLING_UNIT - 1) / CHARS_PER_BILLING_UNIT);
        int cost = aiModelResolveService.costCent(model, usage);
        if (cost <= 0) {
            cost = TaskPrices.cost("TEXT") * usage;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source_id", doc.getSourceId());
        result.put("chapter_count", chapters.size());
        result.put("char_count", charCount);
        result.put("estimated_cost_cent", cost);
        ProductModelAlias.hideStoredModel(result, "TEXT", model.getModelCode());
        result.put("unit_price_cent", model.getUnitPriceCent());
        return Responses.ok(result);
    }

    public ApiResponse adapt(SourceAdaptReq req) {
        ShortSourceDoc doc = requireSource(req == null ? null : req.getSourceId());
        List<ShortSourceChapter> chapters = rangeChapters(doc, req);
        int targetCount = req.getTargetEpisodeCount() == null ? 0 : req.getTargetEpisodeCount();
        if (targetCount < 1 || targetCount > 200) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "target_episode_count must be 1-200");
        }
        ShortWork work = workAccessService.requireWork(doc.getWorkId());
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("source_id", doc.getSourceId());
        request.put("chapter_no_from", chapters.get(0).getChapterNo());
        request.put("chapter_no_to", chapters.get(chapters.size() - 1).getChapterNo());
        request.put("target_episode_count", targetCount);
        request.put("model_code", req.getModelCode());
        ShortAiTask task = aiTaskQueue.enqueue(work.getUserId(), work.getWorkId(), null, "TEXT",
                "ADAPT_OUTLINE", doc.getSourceId(), request, null, 0, req.getModelCode());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_id", task.getTaskId());
        return Responses.ok(result);
    }

    /**
     * 软删源。不级联删作品与剧集：删掉原文不该毁掉已经做出来的片子，
     * 出处位置退化成「原文已删除」即可。
     */
    public ApiResponse delete(SourceDeleteReq req) {
        ShortSourceDoc doc = requireSource(req == null ? null : req.getSourceId());
        ShortSourceDoc patch = new ShortSourceDoc();
        patch.setDeletedFlag(1);
        patch.setUpdatedTime(new Date());
        ShortSourceDocExample up = new ShortSourceDocExample();
        up.createCriteria().andIdEqualTo(doc.getId());
        shortSourceDocMapper.updateByExampleSelective(patch, up);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source_id", doc.getSourceId());
        return Responses.ok(result);
    }

    private List<ShortSourceChapter> rangeChapters(ShortSourceDoc doc, SourceAdaptReq req) {
        int from = req == null || req.getChapterNoFrom() == null ? 1 : req.getChapterNoFrom();
        int to = req == null || req.getChapterNoTo() == null ? Integer.MAX_VALUE : req.getChapterNoTo();
        if (from < 1 || to < from) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "chapter range invalid");
        }
        ShortSourceChapterExample ex = new ShortSourceChapterExample();
        ex.createCriteria().andSourceIdEqualTo(doc.getSourceId()).andDeletedFlagEqualTo(0)
                .andChapterNoGreaterThanOrEqualTo(from).andChapterNoLessThanOrEqualTo(to);
        ex.setOrderByClause("chapter_no asc");
        List<ShortSourceChapter> chapters = shortSourceChapterMapper.selectByExample(ex);
        if (chapters.isEmpty()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "选中的章节范围内没有可用章节");
        }
        return chapters;
    }

    private ShortSourceDoc requireSource(String sourceId) {
        String userId = workAccessService.requireUser();
        if (sourceId == null || sourceId.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "source_id required");
        }
        ShortSourceDocExample ex = new ShortSourceDocExample();
        ex.createCriteria().andSourceIdEqualTo(sourceId.trim()).andUserIdEqualTo(userId)
                .andDeletedFlagEqualTo(0);
        ShortSourceDoc doc = shortSourceDocMapper.selectOneByExample(ex);
        if (doc == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "源不存在或已删除");
        }
        return doc;
    }
}
