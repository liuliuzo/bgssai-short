package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.common.AdminAuthGuard;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortFilmMapper;
import com.bgssai.shortai.mapper.ShortReviewRecordMapper;
import com.bgssai.shortai.mapper.ShortScriptMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortFilm;
import com.bgssai.shortai.model.ShortFilmExample;
import com.bgssai.shortai.model.ShortReviewRecord;
import com.bgssai.shortai.model.ShortReviewRecordExample;
import com.bgssai.shortai.model.ShortScript;
import com.bgssai.shortai.model.ShortScriptExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;
import com.bgssai.shortai.production.dto.ReviewActionReq;
import com.bgssai.shortai.production.support.ShortJson;
import com.github.pagehelper.PageHelper;

@Service
public class ReviewAdminService {

    @Autowired
    private ShortReviewRecordMapper shortReviewRecordMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;
    @Autowired
    private ShortWorkMapper shortWorkMapper;
    @Autowired
    private ShortScriptMapper shortScriptMapper;
    @Autowired
    private ShortFilmMapper shortFilmMapper;

    public PageResponse list(int pageNum, int pageSize, String reviewStatus, String keyword) {
        requireAdmin();
        int safePageNum = pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize < 1 || pageSize > 100 ? 20 : pageSize;
        ShortReviewRecordExample ex = new ShortReviewRecordExample();
        ShortReviewRecordExample.Criteria c = ex.createCriteria().andDeletedFlagEqualTo(0);
        if (reviewStatus != null && !reviewStatus.isBlank()) {
            c.andReviewStatusEqualTo(reviewStatus.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            applyKeyword(c, keyword.trim());
        }
        ex.setOrderByClause("submitted_time desc, id desc");
        PageHelper.startPage(safePageNum, safePageSize);
        List<ShortReviewRecord> rows = shortReviewRecordMapper.selectByExample(ex);
        long total = ((com.github.pagehelper.Page<ShortReviewRecord>) rows).getTotal();
        List<Map<String, Object>> items = new ArrayList<>();
        for (ShortReviewRecord row : rows) {
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

    public ApiResponse detail(String reviewId) {
        requireAdmin();
        ShortReviewRecord row = requireReview(reviewId);
        Map<String, Object> result = summary(row);
        result.put("review_comment", row.getReviewComment());
        result.put("reject_item_list", ShortJson.list(row.getRejectItemJson()));
        result.put("reviewed_time", row.getReviewedTime());
        result.put("reviewer_user_id", row.getReviewerUserId());
        result.put("risk_level", row.getRiskLevel());
        result.put("script_excerpt", scriptExcerpt(row.getEpisodeId()));
        attachFilm(result, row.getEpisodeId());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse approve(ReviewActionReq req) {
        requireAdmin();
        ShortReviewRecord row = requirePending(req);
        Date now = new Date();
        ShortReviewRecord patch = new ShortReviewRecord();
        patch.setReviewStatus("APPROVED");
        patch.setReviewComment(req.getReviewComment());
        patch.setReviewerUserId(UserContext.getUserId());
        patch.setReviewedTime(now);
        patch.setUpdatedTime(now);
        ShortReviewRecordExample up = new ShortReviewRecordExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortReviewRecordMapper.updateByExampleSelective(patch, up);
        patchEpisodeReview(row.getEpisodeId(), "APPROVED");
        refreshWork(row.getWorkId());
        return Responses.ok("ok");
    }

    @Transactional
    public ApiResponse reject(ReviewActionReq req) {
        requireAdmin();
        ShortReviewRecord row = requirePending(req);
        if (req.getRejectItemList() == null || req.getRejectItemList().isEmpty()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "reject_item_list is required");
        }
        Date now = new Date();
        ShortReviewRecord patch = new ShortReviewRecord();
        patch.setReviewStatus("REJECTED");
        patch.setReviewComment(req.getReviewComment());
        patch.setRejectItemJson(ShortJson.toJson(req.getRejectItemList()));
        patch.setReviewerUserId(UserContext.getUserId());
        patch.setReviewedTime(now);
        patch.setUpdatedTime(now);
        ShortReviewRecordExample up = new ShortReviewRecordExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortReviewRecordMapper.updateByExampleSelective(patch, up);
        patchEpisodeReview(row.getEpisodeId(), "REJECTED");
        refreshWork(row.getWorkId());
        return Responses.ok("ok");
    }

    void refreshWork(String workId) {
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        if (work == null) {
            return;
        }
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        List<ShortEpisode> episodes = shortEpisodeMapper.selectByExample(ex);
        boolean allApproved = !episodes.isEmpty();
        boolean anyPending = false;
        for (ShortEpisode ep : episodes) {
            if (!"APPROVED".equals(ep.getReviewStatus())) {
                allApproved = false;
            }
            if ("PENDING".equals(ep.getReviewStatus())) {
                anyPending = true;
            }
        }
        String status = allApproved ? "COMPLETED" : (anyPending ? "REVIEWING" : work.getWorkStatus());
        if (status.equals(work.getWorkStatus())) {
            return;
        }
        ShortWork patch = new ShortWork();
        patch.setWorkStatus(status);
        patch.setUpdatedTime(new Date());
        ShortWorkExample up = new ShortWorkExample();
        up.createCriteria().andIdEqualTo(work.getId());
        shortWorkMapper.updateByExampleSelective(patch, up);
    }

    private void applyKeyword(ShortReviewRecordExample.Criteria c, String keyword) {
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkTitleLike("%" + keyword + "%").andDeletedFlagEqualTo(0);
        List<ShortWork> works = shortWorkMapper.selectByExample(wx);
        List<String> workIds = new ArrayList<>();
        for (ShortWork work : works) {
            workIds.add(work.getWorkId());
        }
        if (workIds.isEmpty()) {
            c.andReviewIdEqualTo(keyword);
        } else {
            c.andWorkIdIn(workIds);
        }
    }

    private Map<String, Object> summary(ShortReviewRecord row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("review_id", row.getReviewId());
        item.put("work_id", row.getWorkId());
        item.put("episode_id", row.getEpisodeId());
        item.put("creator_user_id", row.getCreatorUserId());
        item.put("review_status", row.getReviewStatus());
        item.put("risk_level", row.getRiskLevel());
        item.put("submitted_time", row.getSubmittedTime());
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(row.getWorkId()).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        item.put("work_title", work == null ? null : work.getWorkTitle());
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andEpisodeIdEqualTo(row.getEpisodeId()).andDeletedFlagEqualTo(0);
        ShortEpisode ep = shortEpisodeMapper.selectOneByExample(ex);
        item.put("episode_no", ep == null ? null : ep.getEpisodeNo());
        item.put("episode_title", ep == null ? null : ep.getEpisodeTitle());
        return item;
    }

    private String scriptExcerpt(String episodeId) {
        if (episodeId == null || episodeId.isBlank() || shortScriptMapper == null) {
            return null;
        }
        ShortScriptExample ex = new ShortScriptExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId);
        ex.setOrderByClause("id desc");
        ShortScript script = shortScriptMapper.selectOneByExample(ex);
        if (script == null || script.getSceneJson() == null || script.getSceneJson().isBlank()) {
            return null;
        }
        String text = script.getSceneJson().trim();
        return text.length() > 800 ? text.substring(0, 800) : text;
    }

    private void attachFilm(Map<String, Object> result, String episodeId) {
        if (episodeId == null || episodeId.isBlank() || shortFilmMapper == null) {
            result.put("media_play_url", null);
            result.put("compose_status", null);
            return;
        }
        ShortFilmExample ex = new ShortFilmExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        ShortFilm film = shortFilmMapper.selectOneByExample(ex);
        result.put("media_play_url", film == null ? null : film.getMediaPlayUrl());
        result.put("compose_status", film == null ? null : film.getComposeStatus());
    }

    private ShortReviewRecord requirePending(ReviewActionReq req) {
        if (req == null || req.getReviewId() == null || req.getReviewId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "review_id is required");
        }
        ShortReviewRecord row = requireReview(req.getReviewId());
        if (!"PENDING".equals(row.getReviewStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "only PENDING can be decided");
        }
        return row;
    }

    private ShortReviewRecord requireReview(String reviewId) {
        if (reviewId == null || reviewId.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "review_id is required");
        }
        ShortReviewRecordExample ex = new ShortReviewRecordExample();
        ex.createCriteria().andReviewIdEqualTo(reviewId.trim()).andDeletedFlagEqualTo(0);
        ShortReviewRecord row = shortReviewRecordMapper.selectOneByExample(ex);
        if (row == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "review not found");
        }
        return row;
    }

    private void patchEpisodeReview(String episodeId, String status) {
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        ShortEpisode ep = shortEpisodeMapper.selectOneByExample(ex);
        if (ep == null) {
            return;
        }
        ShortEpisode patch = new ShortEpisode();
        patch.setReviewStatus(status);
        patch.setUpdatedTime(new Date());
        ShortEpisodeExample up = new ShortEpisodeExample();
        up.createCriteria().andIdEqualTo(ep.getId());
        shortEpisodeMapper.updateByExampleSelective(patch, up);
    }

    private void requireAdmin() {
        if (!AdminAuthGuard.isAdmin()) {
            throw new ForbiddenException("only admin can review");
        }
    }
}
