package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortContinuityIssueMapper;
import com.bgssai.shortai.mapper.ShortEpisodeGapMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortForeshadowMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.ShortContinuityIssue;
import com.bgssai.shortai.model.ShortContinuityIssueExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortEpisodeGap;
import com.bgssai.shortai.model.ShortEpisodeGapExample;
import com.bgssai.shortai.model.ShortForeshadow;
import com.bgssai.shortai.model.ShortForeshadowExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.FeatureDtos.ContinuityRecheckReq;
import com.bgssai.shortai.production.dto.FeatureDtos.ForeshadowSaveReq;
import com.bgssai.shortai.production.support.ShortIds;

@Service
public class ContinuityService {

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private ShortContinuityIssueMapper shortContinuityIssueMapper;
    @Autowired
    private ShortForeshadowMapper shortForeshadowMapper;
    @Autowired
    private ShortEpisodeGapMapper shortEpisodeGapMapper;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;

    public ApiResponse overview(String workId) {
        ShortWork work = workAccessService.requireWork(workId);
        ShortShotExample shotEx = new ShortShotExample();
        shotEx.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        Map<String, ShortShot> liveShots = new HashMap<>();
        for (ShortShot shot : shortShotMapper.selectByExample(shotEx)) {
            if (shot.getShotId() != null) {
                liveShots.put(shot.getShotId(), shot);
            }
        }
        ShortContinuityIssueExample ex = new ShortContinuityIssueExample();
        ex.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        List<Map<String, Object>> issues = new ArrayList<>();
        int open = 0;
        for (ShortContinuityIssue i : shortContinuityIssueMapper.selectByExample(ex)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("issue_id", i.getIssueId());
            item.put("episode_id", i.getEpisodeId());
            item.put("issue_type", i.getIssueType());
            item.put("severity", i.getSeverity());
            item.put("issue_desc", i.getIssueDesc());
            item.put("shot_id", i.getShotId());
            ShortShot shot = liveShots.get(i.getShotId());
            if (shot == null) {
                item.put("shot_no", null);
                item.put("shot_in_episode", 0);
            } else {
                item.put("shot_no", shot.getShotNo());
                item.put("shot_in_episode", 1);
            }
            item.put("related_shot_id", i.getRelatedShotId());
            item.put("handle_status", i.getHandleStatus());
            issues.add(item);
            if ("OPEN".equals(i.getHandleStatus())) {
                open++;
            }
        }
        ShortEpisodeGapExample gx = new ShortEpisodeGapExample();
        gx.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        List<Map<String, Object>> gaps = new ArrayList<>();
        for (ShortEpisodeGap g : shortEpisodeGapMapper.selectByExample(gx)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("gap_id", g.getGapId());
            item.put("from_episode_id", g.getFromEpisodeId());
            item.put("to_episode_id", g.getToEpisodeId());
            item.put("gap_desc", g.getGapDesc());
            item.put("severity", g.getSeverity());
            item.put("handle_status", g.getHandleStatus());
            gaps.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("open_issue_count", open);
        result.put("issue_list", issues);
        result.put("gap_list", gaps);
        return Responses.ok(result);
    }

    public ApiResponse foreshadows(String workId) {
        ShortWork work = workAccessService.requireWork(workId);
        ShortForeshadowExample ex = new ShortForeshadowExample();
        ex.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("plant_episode_no asc");
        List<Map<String, Object>> list = new ArrayList<>();
        for (ShortForeshadow f : shortForeshadowMapper.selectByExample(ex)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("foreshadow_id", f.getForeshadowId());
            item.put("plant_episode_no", f.getPlantEpisodeNo());
            item.put("payoff_episode_no", f.getPayoffEpisodeNo());
            item.put("foreshadow_desc", f.getForeshadowDesc());
            item.put("status", f.getStatus());
            list.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("foreshadow_list", list);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse foreshadowSave(ForeshadowSaveReq req) {
        ShortWork work = workAccessService.requireWork(req == null ? null : req.getWorkId());
        if (Integer.valueOf(1).equals(req.getDeletedFlag()) && req.getForeshadowId() != null) {
            ShortForeshadowExample ex = new ShortForeshadowExample();
            ex.createCriteria().andForeshadowIdEqualTo(req.getForeshadowId()).andDeletedFlagEqualTo(0);
            ShortForeshadow patch = new ShortForeshadow();
            patch.setDeletedFlag(1);
            patch.setUpdatedTime(new Date());
            shortForeshadowMapper.updateByExampleSelective(patch, ex);
            return Responses.ok("ok");
        }
        if (req.getForeshadowId() != null && !req.getForeshadowId().isBlank()) {
            ShortForeshadowExample ex = new ShortForeshadowExample();
            ex.createCriteria().andForeshadowIdEqualTo(req.getForeshadowId()).andDeletedFlagEqualTo(0);
            ShortForeshadow row = shortForeshadowMapper.selectOneByExample(ex);
            if (row == null || !work.getUserId().equals(row.getUserId())) {
                throw new ForbiddenException("foreshadow not found");
            }
            ShortForeshadow patch = new ShortForeshadow();
            if (req.getPlantEpisodeNo() != null) {
                patch.setPlantEpisodeNo(req.getPlantEpisodeNo());
            }
            if (req.getPayoffEpisodeNo() != null) {
                patch.setPayoffEpisodeNo(req.getPayoffEpisodeNo());
            }
            if (req.getForeshadowDesc() != null) {
                patch.setForeshadowDesc(req.getForeshadowDesc());
            }
            if (req.getStatus() != null) {
                patch.setStatus(req.getStatus());
            }
            patch.setUpdatedTime(new Date());
            ShortForeshadowExample up = new ShortForeshadowExample();
            up.createCriteria().andIdEqualTo(row.getId());
            shortForeshadowMapper.updateByExampleSelective(patch, up);
            return Responses.ok(Map.of("foreshadow_id", row.getForeshadowId()));
        }
        ShortForeshadow row = new ShortForeshadow();
        row.setForeshadowId(ShortIds.id("fs-"));
        row.setWorkId(work.getWorkId());
        row.setUserId(work.getUserId());
        row.setPlantEpisodeNo(req.getPlantEpisodeNo() == null ? 1 : req.getPlantEpisodeNo());
        row.setPayoffEpisodeNo(req.getPayoffEpisodeNo());
        row.setForeshadowDesc(req.getForeshadowDesc());
        row.setStatus(req.getStatus() == null ? "OPEN" : req.getStatus());
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        row.setUpdatedTime(new Date());
        shortForeshadowMapper.insertSelective(row);
        return Responses.ok(Map.of("foreshadow_id", row.getForeshadowId()));
    }

    @Transactional
    public ApiResponse recheck(ContinuityRecheckReq req) {
        if (req == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "work_id or episode_id required");
        }
        ShortWork work;
        List<ShortEpisode> episodes = new ArrayList<>();
        if (req.getEpisodeId() != null && !req.getEpisodeId().isBlank()) {
            ShortEpisode ep = workAccessService.requireEpisode(req.getEpisodeId());
            work = workAccessService.requireWork(ep.getWorkId());
            episodes.add(ep);
        } else {
            work = workAccessService.requireWork(req.getWorkId());
            ShortEpisodeExample ex = new ShortEpisodeExample();
            ex.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
            episodes.addAll(shortEpisodeMapper.selectByExample(ex));
        }
        int created = 0;
        for (ShortEpisode ep : episodes) {
            ShortShotExample sx = new ShortShotExample();
            sx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
            sx.setOrderByClause("shot_no asc");
            List<ShortShot> shots = shortShotMapper.selectByExample(sx);
            ShortShot prev = null;
            for (ShortShot shot : shots) {
                if (prev != null) {
                    String a = prev.getShotDesc() == null ? "" : prev.getShotDesc();
                    String b = shot.getShotDesc() == null ? "" : shot.getShotDesc();
                    if ((a.contains("室内") && b.contains("室外")) || (a.contains("室外") && b.contains("室内"))) {
                        ShortContinuityIssue issue = new ShortContinuityIssue();
                        issue.setIssueId(ShortIds.id("ci-"));
                        issue.setEpisodeId(ep.getEpisodeId());
                        issue.setWorkId(work.getWorkId());
                        issue.setIssueType("SPACE");
                        issue.setSeverity("MINOR");
                        issue.setIssueDesc("相邻镜头空间可能跳变");
                        issue.setShotId(shot.getShotId());
                        issue.setRelatedShotId(prev.getShotId());
                        issue.setHandleStatus("OPEN");
                        issue.setDeletedFlag(0);
                        issue.setCreatedTime(new Date());
                        issue.setUpdatedTime(new Date());
                        shortContinuityIssueMapper.insertSelective(issue);
                        created++;
                    }
                    if (Integer.valueOf(1).equals(shot.getAxisFlag())) {
                        ShortContinuityIssue issue = new ShortContinuityIssue();
                        issue.setIssueId(ShortIds.id("ci-"));
                        issue.setEpisodeId(ep.getEpisodeId());
                        issue.setWorkId(work.getWorkId());
                        issue.setIssueType("EYELINE");
                        issue.setSeverity("MAJOR");
                        issue.setIssueDesc("镜头标记越轴");
                        issue.setShotId(shot.getShotId());
                        issue.setRelatedShotId(prev.getShotId());
                        issue.setHandleStatus("OPEN");
                        issue.setDeletedFlag(0);
                        issue.setCreatedTime(new Date());
                        issue.setUpdatedTime(new Date());
                        shortContinuityIssueMapper.insertSelective(issue);
                        created++;
                    }
                }
                prev = shot;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created_count", created);
        return Responses.ok(result);
    }
}
