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
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;
import com.bgssai.shortai.production.dto.ProductionDtos.EpisodeReq;
import com.bgssai.shortai.production.dto.ProductionDtos.WorkArchiveReq;
import com.bgssai.shortai.production.dto.ProductionDtos.WorkCreateReq;
import com.bgssai.shortai.production.dto.ProductionDtos.WorkUpdateReq;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortLabels;
import com.github.pagehelper.PageHelper;

@Service
public class WorkService {

    private static final Logger log = LoggerFactory.getLogger(WorkService.class);

    @Autowired
    private ShortWorkMapper shortWorkMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;
    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private WorkStatusService workStatusService;
    @Autowired
    private AiTaskQueue aiTaskQueue;

    public PageResponse list(int pageNum, int pageSize, String workStatus, String genre, String keyword) {
        String userId = workAccessService.requireUser();
        int safePageNum = pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize < 1 || pageSize > 100 ? 20 : pageSize;
        ShortWorkExample ex = new ShortWorkExample();
        ShortWorkExample.Criteria c = ex.createCriteria().andUserIdEqualTo(userId).andDeletedFlagEqualTo(0);
        if (workStatus != null && !workStatus.isBlank()) {
            c.andWorkStatusEqualTo(workStatus.trim());
        }
        if (genre != null && !genre.isBlank()) {
            c.andGenreEqualTo(genre.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            c.andWorkTitleLike("%" + keyword.trim() + "%");
        }
        ex.setOrderByClause("updated_time desc, id desc");
        PageHelper.startPage(safePageNum, safePageSize);
        List<ShortWork> rows = shortWorkMapper.selectByExample(ex);
        long total = ((com.github.pagehelper.Page<ShortWork>) rows).getTotal();
        List<Map<String, Object>> items = new ArrayList<>();
        for (ShortWork row : rows) {
            items.add(summary(row));
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

    @Transactional
    public ApiResponse create(WorkCreateReq req) {
        String userId = workAccessService.requireUser();
        if (req == null || req.getWorkTitle() == null || req.getWorkTitle().isBlank()
                || req.getOneLinePitch() == null || req.getOneLinePitch().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "work_title and one_line_pitch are required");
        }
        int duration = req.getEpisodeDurationSec() == null ? 90 : req.getEpisodeDurationSec();
        if (duration != 60 && duration != 90 && duration != 120) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "episode_duration_sec must be 60/90/120");
        }
        int count = req.getEpisodeCount() == null ? 24 : req.getEpisodeCount();
        if (count < 1 || count > 80) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "episode_count out of range");
        }
        Date now = new Date();
        ShortWork work = new ShortWork();
        work.setWorkId(ShortIds.id("w-"));
        work.setUserId(userId);
        work.setWorkTitle(req.getWorkTitle().trim());
        work.setOneLinePitch(req.getOneLinePitch().trim());
        work.setGenre(blank(req.getGenre(), "REVENGE"));
        work.setWorkStatus("DRAFT");
        work.setEpisodeCount(count);
        work.setEpisodeDurationSec(duration);
        work.setVisualStyle(blank(req.getVisualStyle(), "MODERN_URBAN"));
        work.setAspectRatio("VERTICAL_9_16");
        work.setCostSpentCent(0);
        work.setSkipLevelCheckFlag(0);
        work.setArchivedFlag(0);
        work.setDeletedFlag(0);
        work.setCreatedTime(now);
        work.setUpdatedTime(now);
        shortWorkMapper.insertSelective(work);
        for (int i = 1; i <= count; i++) {
            ShortEpisode ep = new ShortEpisode();
            ep.setEpisodeId(ShortIds.id("e-"));
            ep.setWorkId(work.getWorkId());
            ep.setUserId(userId);
            ep.setEpisodeNo(i);
            ep.setEpisodeTitle("第" + i + "集");
            ep.setScriptStatus("NONE");
            ep.setStoryboardStatus("NONE");
            ep.setDubbingStatus("NONE");
            ep.setComposeStatus("NONE");
            ep.setReviewStatus("NOT_SUBMITTED");
            ep.setDeletedFlag(0);
            ep.setCreatedTime(now);
            ep.setUpdatedTime(now);
            shortEpisodeMapper.insertSelective(ep);
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("work_id", work.getWorkId());
        ShortAiTask task = aiTaskQueue.enqueue(userId, work.getWorkId(), null, "TEXT", "OUTLINE",
                work.getWorkId(), request, null, 0);
        log.info("created work workId={} episodes={}", work.getWorkId(), count);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("work_id", work.getWorkId());
        result.put("task_id", task.getTaskId());
        return Responses.ok(result);
    }

    public ApiResponse detail(String workId) {
        ShortWork work = workAccessService.requireWork(workId);
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("episode_no asc");
        List<ShortEpisode> episodes = shortEpisodeMapper.selectByExample(ex);
        List<Map<String, Object>> episodeList = new ArrayList<>();
        for (ShortEpisode ep : episodes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("episode_id", ep.getEpisodeId());
            item.put("episode_no", ep.getEpisodeNo());
            item.put("episode_title", ep.getEpisodeTitle());
            item.put("episode_synopsis", ep.getEpisodeSynopsis());
            item.put("ending_hook", ep.getEndingHook());
            item.put("script_status", ep.getScriptStatus());
            item.put("storyboard_status", ep.getStoryboardStatus());
            item.put("dubbing_status", ep.getDubbingStatus());
            item.put("compose_status", ep.getComposeStatus());
            item.put("review_status", ep.getReviewStatus());
            item.put("film_duration_sec", ep.getFilmDurationSec());
            episodeList.add(item);
        }
        Map<String, Object> result = summary(work);
        result.put("one_line_pitch", work.getOneLinePitch());
        result.put("visual_style", work.getVisualStyle());
        result.put("aspect_ratio", work.getAspectRatio());
        result.put("episode_duration_sec", work.getEpisodeDurationSec());
        result.put("cost_budget_cent", work.getCostBudgetCent());
        result.put("skip_level_check_flag", work.getSkipLevelCheckFlag());
        result.put("quality_threshold_score", work.getQualityThresholdScore());
        result.put("episode_list", episodeList);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse update(WorkUpdateReq req) {
        if (req == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "body required");
        }
        ShortWork work = workAccessService.requireWork(req.getWorkId());
        ShortWork patch = new ShortWork();
        if (req.getWorkTitle() != null) {
            patch.setWorkTitle(req.getWorkTitle());
        }
        if (req.getOneLinePitch() != null) {
            patch.setOneLinePitch(req.getOneLinePitch());
        }
        if (req.getGenre() != null) {
            patch.setGenre(req.getGenre());
        }
        if (req.getVisualStyle() != null) {
            patch.setVisualStyle(req.getVisualStyle());
        }
        if (req.getCostBudgetCent() != null) {
            patch.setCostBudgetCent(req.getCostBudgetCent());
        }
        if (req.getQualityThresholdScore() != null) {
            patch.setQualityThresholdScore(req.getQualityThresholdScore());
        }
        if (req.getSkipLevelCheckFlag() != null) {
            patch.setSkipLevelCheckFlag(req.getSkipLevelCheckFlag());
        }
        patch.setUpdatedTime(new Date());
        ShortWorkExample up = new ShortWorkExample();
        up.createCriteria().andIdEqualTo(work.getId());
        shortWorkMapper.updateByExampleSelective(patch, up);
        return Responses.ok("ok");
    }

    @Transactional
    public ApiResponse archive(WorkArchiveReq req) {
        if (req == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "body required");
        }
        ShortWork work = workAccessService.requireWork(req.getWorkId());
        ShortWork patch = new ShortWork();
        int flag = req.getArchivedFlag() == null ? 1 : req.getArchivedFlag();
        patch.setArchivedFlag(flag);
        patch.setWorkStatus(flag == 1 ? "ARCHIVED" : "DRAFT");
        patch.setUpdatedTime(new Date());
        ShortWorkExample up = new ShortWorkExample();
        up.createCriteria().andIdEqualTo(work.getId());
        shortWorkMapper.updateByExampleSelective(patch, up);
        if (flag == 0) {
            workStatusService.refresh(work.getWorkId());
        }
        return Responses.ok("ok");
    }

    @Transactional
    public ApiResponse createEpisode(EpisodeReq req) {
        if (req == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "body required");
        }
        ShortWork work = workAccessService.requireWork(req.getWorkId());
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        int next = (int) shortEpisodeMapper.countByExample(ex) + 1;
        Date now = new Date();
        ShortEpisode ep = new ShortEpisode();
        ep.setEpisodeId(ShortIds.id("e-"));
        ep.setWorkId(work.getWorkId());
        ep.setUserId(work.getUserId());
        ep.setEpisodeNo(next);
        ep.setEpisodeTitle(req.getEpisodeTitle() == null || req.getEpisodeTitle().isBlank()
                ? ("第" + next + "集") : req.getEpisodeTitle().trim());
        ep.setScriptStatus("NONE");
        ep.setStoryboardStatus("NONE");
        ep.setDubbingStatus("NONE");
        ep.setComposeStatus("NONE");
        ep.setReviewStatus("NOT_SUBMITTED");
        ep.setDeletedFlag(0);
        ep.setCreatedTime(now);
        ep.setUpdatedTime(now);
        shortEpisodeMapper.insertSelective(ep);
        ShortWork patch = new ShortWork();
        patch.setEpisodeCount(next);
        patch.setUpdatedTime(now);
        ShortWorkExample up = new ShortWorkExample();
        up.createCriteria().andIdEqualTo(work.getId());
        shortWorkMapper.updateByExampleSelective(patch, up);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("episode_id", ep.getEpisodeId());
        result.put("episode_no", next);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse updateEpisode(EpisodeReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        ShortEpisode patch = new ShortEpisode();
        if (req.getEpisodeTitle() != null) {
            patch.setEpisodeTitle(req.getEpisodeTitle());
        }
        if (req.getEpisodeSynopsis() != null) {
            patch.setEpisodeSynopsis(req.getEpisodeSynopsis());
        }
        if (req.getEndingHook() != null) {
            patch.setEndingHook(req.getEndingHook());
        }
        patch.setUpdatedTime(new Date());
        ShortEpisodeExample up = new ShortEpisodeExample();
        up.createCriteria().andIdEqualTo(ep.getId());
        shortEpisodeMapper.updateByExampleSelective(patch, up);
        return Responses.ok("ok");
    }

    @Transactional
    public ApiResponse deleteEpisode(EpisodeReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        if ("READY".equals(ep.getComposeStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "composed episode cannot be deleted");
        }
        ShortEpisode patch = new ShortEpisode();
        patch.setDeletedFlag(1);
        patch.setUpdatedTime(new Date());
        ShortEpisodeExample up = new ShortEpisodeExample();
        up.createCriteria().andIdEqualTo(ep.getId());
        shortEpisodeMapper.updateByExampleSelective(patch, up);
        workStatusService.refresh(ep.getWorkId());
        return Responses.ok("ok");
    }

    public ApiResponse emptyTemplates(String genre) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("template_list", new ArrayList<>());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse batchEpisodes(com.bgssai.shortai.production.dto.FeatureDtos.EpisodeBatchReq req) {
        ShortWork work = workAccessService.requireWork(req == null ? null : req.getWorkId());
        if (req.getEpisodeIdList() == null || req.getEpisodeIdList().isEmpty()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "episode_id_list required");
        }
        String action = req.getAction() == null ? "ARCHIVE" : req.getAction().trim();
        int updated = 0;
        for (String episodeId : req.getEpisodeIdList()) {
            ShortEpisode ep = workAccessService.requireEpisode(episodeId);
            if (!work.getWorkId().equals(ep.getWorkId())) {
                throw new ForbiddenException("episode not found");
            }
            ShortEpisode patch = new ShortEpisode();
            patch.setUpdatedTime(new Date());
            if ("ARCHIVE".equals(action) || "DELETE".equals(action)) {
                patch.setDeletedFlag(1);
            } else if ("RESET_COMPOSE".equals(action)) {
                patch.setComposeStatus("NONE");
            } else if ("MARK_READY".equals(action)) {
                patch.setStoryboardStatus("READY");
            } else {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "unsupported batch action");
            }
            ShortEpisodeExample up = new ShortEpisodeExample();
            up.createCriteria().andIdEqualTo(ep.getId());
            shortEpisodeMapper.updateByExampleSelective(patch, up);
            updated++;
        }
        workStatusService.refresh(work.getWorkId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated_count", updated);
        result.put("action", action);
        return Responses.ok(result);
    }

    public ApiResponse batchNotImplemented() {
        throw new BizException(CommonErrorCode.RULE_REJECT, "episodes/batch is not in this slice");
    }

    private Map<String, Object> summary(ShortWork work) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("work_id", work.getWorkId());
        item.put("work_title", work.getWorkTitle());
        item.put("one_line_pitch", work.getOneLinePitch());
        item.put("genre", work.getGenre());
        item.put("genre_name", ShortLabels.genreName(work.getGenre()));
        item.put("work_status", work.getWorkStatus());
        item.put("work_status_name", ShortLabels.workStatusName(work.getWorkStatus()));
        item.put("episode_count", work.getEpisodeCount());
        item.put("updated_time", work.getUpdatedTime());
        item.put("cost_spent_cent", work.getCostSpentCent());
        return item;
    }

    private String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
