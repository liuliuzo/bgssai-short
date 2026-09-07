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
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.production.dto.ShotReorderDtos.ShotReorderReq;

/**
 * 分镜画布的场内重排：拖动节点即改镜号。
 *
 * <p>重排只改顺序——不删素材、不改状态、不触发任何重抽。画布上拖一下就烧掉几十块钱，
 * 没有人敢用这个功能。
 *
 * <p>只允许场内重排。跨场意味着这一镜换了场景，那是剧本层的事（回剧本改场次划分再拆镜）；
 * 在画布上硬拖只会让镜头的 scene_no 与它的画面内容、绑定的场景卡对不上，
 * 而这种不一致要等到出图出来才看得见。
 */
@Service
public class ShotReorderService {

    private static final Logger log = LoggerFactory.getLogger(ShotReorderService.class);

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private ShortShotMapper shortShotMapper;

    @Transactional
    public ApiResponse reorder(ShotReorderReq req) {
        if (req == null || req.getShotIdList() == null || req.getShotIdList().isEmpty()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "shot_id_list required");
        }
        ShortEpisode ep = workAccessService.requireEpisode(req.getEpisodeId());
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("shot_no asc");
        List<ShortShot> before = shortShotMapper.selectByExample(ex);
        Map<String, ShortShot> byId = new LinkedHashMap<>();
        for (ShortShot shot : before) {
            byId.put(shot.getShotId(), shot);
        }

        List<ShortShot> ordered = new ArrayList<>();
        for (String shotId : req.getShotIdList()) {
            ShortShot shot = byId.get(shotId);
            if (shot == null) {
                throw new BizException(CommonErrorCode.INVALID_PARAM, "镜头不属于本集：" + shotId);
            }
            if (!sameScene(shot.getSceneNo(), req.getSceneNo())) {
                throw new BizException(CommonErrorCode.INVALID_PARAM,
                        "镜头不属于该场次，跨场请回剧本层改场次划分：" + shotId);
            }
            if ("LOCKED".equals(shot.getShotStatus())) {
                throw new BizException(CommonErrorCode.RULE_REJECT,
                        "镜 " + shot.getShotNo() + " 已锁定，先解锁再调整顺序");
            }
            ordered.add(shot);
        }

        // 该场次原有的镜号就是可用的位置，重排只是把这些位置重新分配给同一批镜头，
        // 因此其余场次的镜号一个都不会动。
        List<Integer> slots = new ArrayList<>();
        for (ShortShot shot : before) {
            if (sameScene(shot.getSceneNo(), req.getSceneNo()) && shot.getShotNo() != null) {
                slots.add(shot.getShotNo());
            }
        }
        if (slots.size() != ordered.size()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM,
                    "本场次共 " + slots.size() + " 镜，提交了 " + ordered.size()
                            + " 镜，请提交该场次的完整顺序");
        }
        slots.sort(Integer::compareTo);

        // 重排前每一镜的前驱，用来判断谁的上游真的换了人
        Map<String, String> prevBefore = new LinkedHashMap<>();
        String cursor = null;
        for (ShortShot shot : before) {
            prevBefore.put(shot.getShotId(), cursor);
            cursor = shot.getShotId();
        }

        Date now = new Date();
        for (int i = 0; i < ordered.size(); i++) {
            ShortShot shot = ordered.get(i);
            int targetNo = slots.get(i);
            if (Integer.valueOf(targetNo).equals(shot.getShotNo())) {
                continue;
            }
            ShortShot patch = new ShortShot();
            patch.setShotNo(targetNo);
            patch.setUpdatedTime(now);
            ShortShotExample up = new ShortShotExample();
            up.createCriteria().andIdEqualTo(shot.getId());
            shortShotMapper.updateByExampleSelective(patch, up);
        }

        // 顺序变了，靠「接上一镜」吃上游尾帧的镜头就可能接不上：标出来，但不替用户花钱重抽
        ShortShotExample afterEx = new ShortShotExample();
        afterEx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        afterEx.setOrderByClause("shot_no asc");
        List<ShortShot> after = shortShotMapper.selectByExample(afterEx);
        List<Integer> staleShotNos = new ArrayList<>();
        String prevId = null;
        for (ShortShot shot : after) {
            String prevWas = prevBefore.get(shot.getShotId());
            boolean prevChanged = prevId == null ? prevWas != null : !prevId.equals(prevWas);
            prevId = shot.getShotId();
            if (!"INHERIT_PREV".equals(shot.getFrameLinkMode()) || !prevChanged) {
                continue;
            }
            ShortShot patch = new ShortShot();
            patch.setStaleFlag(1);
            patch.setUpdatedTime(now);
            ShortShotExample up = new ShortShotExample();
            up.createCriteria().andIdEqualTo(shot.getId());
            shortShotMapper.updateByExampleSelective(patch, up);
            staleShotNos.add(shot.getShotNo());
        }

        log.info("shots reordered episodeId={} sceneNo={} count={} stale={}",
                ep.getEpisodeId(), req.getSceneNo(), ordered.size(), staleShotNos.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("episode_id", ep.getEpisodeId());
        result.put("scene_no", req.getSceneNo());
        result.put("reordered_count", ordered.size());
        result.put("stale_shot_no_list", staleShotNos);
        return Responses.ok(result);
    }

    private boolean sameScene(Integer left, Integer right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }
}
