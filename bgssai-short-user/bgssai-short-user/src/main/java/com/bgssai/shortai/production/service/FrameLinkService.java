package com.bgssai.shortai.production.service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.AiModelConfig;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.production.dto.FrameLinkDtos.FrameLinkBatchReq;
import com.bgssai.shortai.production.dto.FrameLinkDtos.FrameLinkSaveReq;
import com.bgssai.shortai.production.dto.FrameLinkDtos.LastFrameGenerateReq;
import com.bgssai.shortai.production.support.ShortJson;

/**
 * 首尾帧衔接：让相邻镜头接得上，同时明确「哪里不接」。
 *
 * <p>唯一需要反复守住的判断是 {@link #previousLinkableShot}：衔接只在**同一场次**内成立。
 * 跨场次硬接会把两个不同场景的画面糊在一起——上一场雨夜车内、下一场白天办公室，
 * 拿雨夜的尾帧当办公室的开头，比不接更糟。换场本来就该跳，那是剪辑语言，不是需要修的缺陷。
 */
@Service
public class FrameLinkService {

    private static final Logger log = LoggerFactory.getLogger(FrameLinkService.class);

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private AiTaskQueue aiTaskQueue;
    @Autowired
    private AiModelResolveService aiModelResolveService;
    @Autowired
    private ShortShotMapper shortShotMapper;

    public ApiResponse save(FrameLinkSaveReq req) {
        String mode = normalizeMode(req == null ? null : req.getFrameLinkMode());
        ShortShot shot = requireShot(req == null ? null : req.getShotId());
        if ("BOOKEND".equals(mode)) {
            requireLastFrameCapableModel(req.getModelCode());
        }
        ShortShot patch = new ShortShot();
        patch.setFrameLinkMode(mode);
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);

        shot.setFrameLinkMode(mode);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shot_id", shot.getShotId());
        result.put("frame_link_mode", mode);
        result.putAll(effectiveState(shot));
        return Responses.ok(result);
    }

    /**
     * 整集一键衔接：同场次相邻镜头全部置 INHERIT_PREV，跨场处保持 NONE。
     * 一集四五十镜，逐个点开关不现实，但也不能把跨场也一并接上。
     */
    public ApiResponse batch(FrameLinkBatchReq req) {
        String mode = normalizeMode(req == null ? null : req.getFrameLinkMode());
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("shot_no asc");
        List<ShortShot> shots = shortShotMapper.selectByExample(ex);
        Date now = new Date();
        int linked = 0;
        int crossScene = 0;
        for (int i = 0; i < shots.size(); i++) {
            ShortShot shot = shots.get(i);
            boolean sameScene = i > 0 && sameScene(shots.get(i - 1), shot);
            String target = sameScene ? mode : "NONE";
            if (sameScene) {
                linked++;
            } else if (i > 0) {
                crossScene++;
            }
            ShortShot patch = new ShortShot();
            patch.setFrameLinkMode(target);
            patch.setUpdatedTime(now);
            ShortShotExample up = new ShortShotExample();
            up.createCriteria().andIdEqualTo(shot.getId());
            shortShotMapper.updateByExampleSelective(patch, up);
        }
        log.info("frame link batch episodeId={} mode={} linked={} crossScene={}",
                ep.getEpisodeId(), mode, linked, crossScene);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("episode_id", ep.getEpisodeId());
        result.put("frame_link_mode", mode);
        result.put("linked_count", linked);
        result.put("cross_scene_count", crossScene);
        return Responses.ok(result);
    }

    /**
     * 单独出一张尾帧图。适用于本镜还没出视频、但想先定「往哪收」的情况；
     * 已出视频的镜头默认走 ffmpeg 抽帧，不必花这笔钱。
     */
    public ApiResponse generateLastFrame(LastFrameGenerateReq req) {
        ShortShot shot = requireShot(req == null ? null : req.getShotId());
        String prompt = req.getLastFramePrompt() == null ? "" : req.getLastFramePrompt().trim();
        if (prompt.isEmpty()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "last_frame_prompt required");
        }
        ShortShot patch = new ShortShot();
        patch.setLastFramePrompt(prompt);
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("shot_id", shot.getShotId());
        request.put("last_frame_prompt", prompt);
        request.put("model_code", req.getModelCode());
        ShortAiTask task = aiTaskQueue.enqueue(shot.getUserId(), shot.getWorkId(), shot.getEpisodeId(),
                "IMAGE", "LAST_FRAME", shot.getShotId(), request, null, 0, req.getModelCode());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_id", task.getTaskId());
        return Responses.ok(result);
    }

    /**
     * 本镜的衔接是否真的生效，以及不生效的原因。列表接口与生成链路共用这一份判定，
     * 免得界面说「已衔接」而实际生成没带参考帧。
     */
    public Map<String, Object> effectiveState(ShortShot shot) {
        Map<String, Object> state = new LinkedHashMap<>();
        String mode = shot.getFrameLinkMode() == null ? "NONE" : shot.getFrameLinkMode();
        if ("NONE".equals(mode)) {
            state.put("link_effective_flag", 0);
            state.put("downgrade_reason", null);
            state.put("link_hint", null);
            return state;
        }
        if ("BOOKEND".equals(mode)) {
            boolean ready = shot.getLastFrameAssetId() != null && !shot.getLastFrameAssetId().isBlank();
            state.put("link_effective_flag", ready ? 1 : 0);
            state.put("downgrade_reason", ready ? null : "NO_LAST_FRAME");
            state.put("link_hint", ready ? null : "本镜尚无尾帧，先抽帧或单独出一张尾帧图");
            return state;
        }
        ShortShot prev = previousLinkableShot(shot);
        if (prev == null) {
            state.put("link_effective_flag", 0);
            state.put("downgrade_reason", "CROSS_SCENE");
            state.put("link_hint", "跨场不衔接（换场本来就该跳）");
            return state;
        }
        if (prev.getLastFrameAssetId() == null || prev.getLastFrameAssetId().isBlank()) {
            state.put("link_effective_flag", 0);
            state.put("downgrade_reason", "PREV_NO_LAST_FRAME");
            state.put("link_hint", "上一镜尚无尾帧，本次未衔接");
            return state;
        }
        state.put("link_effective_flag", 1);
        state.put("downgrade_reason", null);
        state.put("link_hint", "接镜 " + prev.getShotNo() + " 尾帧");
        return state;
    }

    /**
     * 找同一场次内紧邻的上一镜。跨场返回 null——这不是「找不到」，是「按设计不接」。
     */
    public ShortShot previousLinkableShot(ShortShot shot) {
        if (shot.getShotNo() == null) {
            return null;
        }
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(shot.getEpisodeId()).andDeletedFlagEqualTo(0)
                .andShotNoLessThan(shot.getShotNo());
        ex.setOrderByClause("shot_no desc");
        List<ShortShot> before = shortShotMapper.selectByExample(ex);
        if (before.isEmpty()) {
            return null;
        }
        ShortShot prev = before.get(0);
        return sameScene(prev, shot) ? prev : null;
    }

    /**
     * 上游尾帧变了，把下一镜标成可能接不上。只标记不重抽：重抽要花钱，花不花由创作者定，
     * 与预算闸门、批量准入是同一条产品原则。
     */
    public void markNextStale(ShortShot shot) {
        if (shot == null || shot.getShotNo() == null) {
            return;
        }
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(shot.getEpisodeId()).andDeletedFlagEqualTo(0)
                .andShotNoGreaterThan(shot.getShotNo());
        ex.setOrderByClause("shot_no asc");
        List<ShortShot> after = shortShotMapper.selectByExample(ex);
        if (after.isEmpty()) {
            return;
        }
        ShortShot next = after.get(0);
        if (!"INHERIT_PREV".equals(next.getFrameLinkMode()) || !sameScene(shot, next)) {
            return;
        }
        ShortShot patch = new ShortShot();
        patch.setStaleFlag(1);
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(next.getId());
        shortShotMapper.updateByExampleSelective(patch, up);
        log.info("next shot marked stale by last-frame change shotId={} nextShotId={}",
                shot.getShotId(), next.getShotId());
    }

    /** 模型是否支持首尾帧，取 config_json.support_last_frame。 */
    public boolean supportsLastFrame(AiModelConfig model) {
        if (model == null) {
            return false;
        }
        Map<String, Object> config = ShortJson.map(model.getConfigJson());
        return ShortJson.integer(config.get("support_last_frame"), 0) == 1;
    }

    private void requireLastFrameCapableModel(String modelCode) {
        AiModelConfig model = aiModelResolveService.resolve("VIDEO", modelCode);
        if (!supportsLastFrame(model)) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "所选视频模型不支持首尾帧（" + model.getModelCode() + "），请换模型或改用「接上一镜」");
        }
    }

    private boolean sameScene(ShortShot a, ShortShot b) {
        Integer left = a.getSceneNo();
        Integer right = b.getSceneNo();
        if (left == null || right == null) {
            // 场次未标注时按同场处理：拆镜产出的镜头本就连续，此时不接反而更违和
            return true;
        }
        return left.equals(right);
    }

    private String normalizeMode(String raw) {
        String mode = raw == null ? "" : raw.trim().toUpperCase();
        if (!"NONE".equals(mode) && !"INHERIT_PREV".equals(mode) && !"BOOKEND".equals(mode)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM,
                    "frame_link_mode must be NONE, INHERIT_PREV or BOOKEND");
        }
        return mode;
    }

    private ShortShot requireShot(String shotId) {
        String userId = workAccessService.requireUser();
        if (shotId == null || shotId.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "shot_id required");
        }
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(shotId.trim()).andUserIdEqualTo(userId)
                .andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(ex);
        if (shot == null) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "镜头不存在");
        }
        return shot;
    }
}
