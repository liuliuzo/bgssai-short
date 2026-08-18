package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortReviewRecordMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortReviewRecord;
import com.bgssai.shortai.model.ShortReviewRecordExample;
import com.bgssai.shortai.production.dto.ProductionDtos.ReviewUserReq;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;

@Service
public class ReviewUserService {

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private WorkStatusService workStatusService;
    @Autowired
    private ShortReviewRecordMapper shortReviewRecordMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;

    @Transactional
    public ApiResponse submit(ReviewUserReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        if (!"READY".equals(ep.getComposeStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "compose must be READY");
        }
        if ("PENDING".equals(ep.getReviewStatus())) {
            throw new BizException(CommonErrorCode.DUPLICATE_SUBMIT, "already in review");
        }
        ShortReviewRecord row = new ShortReviewRecord();
        row.setReviewId(ShortIds.id("r-"));
        row.setWorkId(ep.getWorkId());
        row.setEpisodeId(ep.getEpisodeId());
        row.setCreatorUserId(ep.getUserId());
        row.setReviewStatus("PENDING");
        row.setRiskLevel("PASS");
        row.setSubmittedTime(new Date());
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        shortReviewRecordMapper.insertSelective(row);
        patchReview(ep, "PENDING");
        workStatusService.refresh(ep.getWorkId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("review_id", row.getReviewId());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse withdraw(ReviewUserReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        if (!"PENDING".equals(ep.getReviewStatus())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "only PENDING can withdraw");
        }
        ShortReviewRecordExample ex = new ShortReviewRecordExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andReviewStatusEqualTo("PENDING").andDeletedFlagEqualTo(0);
        ShortReviewRecord row = shortReviewRecordMapper.selectOneByExample(ex);
        if (row != null) {
            ShortReviewRecord patch = new ShortReviewRecord();
            patch.setDeletedFlag(1);
            patch.setUpdatedTime(new Date());
            ShortReviewRecordExample up = new ShortReviewRecordExample();
            up.createCriteria().andIdEqualTo(row.getId());
            shortReviewRecordMapper.updateByExampleSelective(patch, up);
        }
        patchReview(ep, "NOT_SUBMITTED");
        workStatusService.refresh(ep.getWorkId());
        return Responses.ok("ok");
    }

    public ApiResponse list(String workId) {
        workAccessService.requireWork(workId);
        ShortReviewRecordExample ex = new ShortReviewRecordExample();
        ex.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("submitted_time desc");
        List<Map<String, Object>> list = new ArrayList<>();
        for (ShortReviewRecord row : shortReviewRecordMapper.selectByExample(ex)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("review_id", row.getReviewId());
            item.put("episode_id", row.getEpisodeId());
            item.put("review_status", row.getReviewStatus());
            item.put("review_comment", row.getReviewComment());
            item.put("reject_item_list", ShortJson.list(row.getRejectItemJson()));
            item.put("submitted_time", row.getSubmittedTime());
            list.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("review_list", list);
        return Responses.ok(result);
    }

    private void patchReview(ShortEpisode ep, String status) {
        ShortEpisode patch = new ShortEpisode();
        patch.setReviewStatus(status);
        patch.setUpdatedTime(new Date());
        ShortEpisodeExample up = new ShortEpisodeExample();
        up.createCriteria().andIdEqualTo(ep.getId());
        shortEpisodeMapper.updateByExampleSelective(patch, up);
    }
}
