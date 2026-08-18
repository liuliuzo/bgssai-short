package com.bgssai.shortai.production.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortFilmMapper;
import com.bgssai.shortai.mapper.ShortSceneCardMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortFilm;
import com.bgssai.shortai.model.ShortFilmExample;
import com.bgssai.shortai.model.ShortSceneCard;
import com.bgssai.shortai.model.ShortSceneCardExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;
import com.bgssai.shortai.production.support.MockAiProvider;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.quota.service.QuotaConsumeService;

@Component
public class ShortAiTaskWorker {

    private static final Logger log = LoggerFactory.getLogger(ShortAiTaskWorker.class);

    private static final String[] COLORS = { "#2563eb", "#7c3aed", "#db2777", "#0f766e", "#b45309" };

    @Autowired
    private ShortAiTaskMapper shortAiTaskMapper;
    @Autowired
    private ShortWorkMapper shortWorkMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortCharacterMapper shortCharacterMapper;
    @Autowired
    private ShortSceneCardMapper shortSceneCardMapper;
    @Autowired
    private ShortAssetMapper shortAssetMapper;
    @Autowired
    private ShortFilmMapper shortFilmMapper;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private WorkStatusService workStatusService;
    @Autowired
    private QuotaConsumeService quotaConsumeService;
    @Autowired
    private AiTaskQueue aiTaskQueue;

    @Scheduled(fixedDelay = 800)
    public void tick() {
        ShortAiTaskExample ex = new ShortAiTaskExample();
        ex.createCriteria().andTaskStatusEqualTo("QUEUED");
        ex.setOrderByClause("id asc");
        ShortAiTask task = shortAiTaskMapper.selectOneByExample(ex);
        if (task == null) {
            return;
        }
        ShortAiTask running = new ShortAiTask();
        running.setTaskStatus("RUNNING");
        running.setStartedTime(new Date());
        running.setProgressPercent(30);
        running.setUpdatedTime(new Date());
        ShortAiTaskExample cas = new ShortAiTaskExample();
        cas.createCriteria().andIdEqualTo(task.getId()).andTaskStatusEqualTo("QUEUED");
        int n = shortAiTaskMapper.updateByExampleSelective(running, cas);
        if (n != 1) {
            return;
        }
        try {
            complete(task);
            quotaConsumeService.consume(task.getUserId(), task.getCostCent() == null ? 0 : task.getCostCent());
            ShortAiTask ok = new ShortAiTask();
            ok.setTaskStatus("SUCCEEDED");
            ok.setProgressPercent(100);
            ok.setFinishedTime(new Date());
            ok.setUpdatedTime(new Date());
            ShortAiTaskExample up = new ShortAiTaskExample();
            up.createCriteria().andIdEqualTo(task.getId());
            shortAiTaskMapper.updateByExampleSelective(ok, up);
            addSpent(task);
        } catch (BizException exBiz) {
            fail(task, exBiz.getCode(), exBiz.getMessage());
        } catch (Exception exAll) {
            log.warn("mock task failed taskId={}", task.getTaskId(), exAll);
            fail(task, "MODEL_TIMEOUT", exAll.getMessage());
        }
    }

    private void complete(ShortAiTask task) {
        Map<String, Object> req = ShortJson.map(task.getRequestJson());
        if ("OUTLINE".equals(task.getTargetType())) {
            finishOutline(task);
            return;
        }
        if ("SCRIPT".equals(task.getTargetType())) {
            finishScript(task, req);
            return;
        }
        if ("REWRITE".equals(task.getTargetType())) {
            finishRewrite(task, req);
            return;
        }
        if ("PORTRAIT".equals(task.getTargetType())) {
            finishPortrait(task, req);
            return;
        }
        if ("SCENE_PORTRAIT".equals(task.getTargetType())) {
            finishScenePortrait(task, req);
            return;
        }
        if ("SHOT".equals(task.getTargetType())) {
            finishShot(task);
            return;
        }
        if ("COMPOSE".equals(task.getTargetType())) {
            finishCompose(task);
        }
    }

    private void finishOutline(ShortAiTask task) {
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(task.getWorkId()).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        if (work == null) {
            return;
        }
        List<Map<String, Object>> outline = MockAiProvider.outline(work.getWorkTitle(), work.getOneLinePitch(),
                work.getEpisodeCount() == null ? 24 : work.getEpisodeCount(), work.getGenre());
        ShortWork patch = new ShortWork();
        patch.setOutlineJson(ShortJson.toJson(outline));
        patch.setUpdatedTime(new Date());
        ShortWorkExample up = new ShortWorkExample();
        up.createCriteria().andIdEqualTo(work.getId());
        shortWorkMapper.updateByExampleSelective(patch, up);
        scriptService.applyOutline(work.getWorkId(), outline);
        workStatusService.refresh(work.getWorkId());
    }

    private void finishScript(ShortAiTask task, Map<String, Object> req) {
        ShortEpisode ep = episode(task.getEpisodeId());
        if (ep == null) {
            return;
        }
        scriptService.persistGeneratedScript(ep, MockAiProvider.script(ep.getEpisodeTitle(), ep.getEpisodeSynopsis(),
                ShortJson.text(req.get("extra_prompt"))));
    }

    private void finishRewrite(ShortAiTask task, Map<String, Object> req) {
        ShortEpisode ep = episode(task.getEpisodeId());
        if (ep == null) {
            return;
        }
        scriptService.persistRewrite(ep, ShortJson.integer(req.get("scene_no"), 0),
                req.get("line_no") == null ? null : ShortJson.integer(req.get("line_no"), 0),
                ShortJson.text(req.get("instruction")));
    }

    private void finishPortrait(ShortAiTask task, Map<String, Object> req) {
        ShortCharacterExample ex = new ShortCharacterExample();
        ex.createCriteria().andCharacterIdEqualTo(task.getTargetId()).andDeletedFlagEqualTo(0);
        ShortCharacter row = shortCharacterMapper.selectOneByExample(ex);
        if (row == null) {
            return;
        }
        int count = ShortJson.integer(req.get("candidate_count"), 4);
        for (int i = 0; i < count; i++) {
            saveSvg(row.getUserId(), row.getWorkId(), null, row.getCharacterId(), "IMAGE",
                    MockAiProvider.svg(row.getCharacterName(), "候选 " + (i + 1), COLORS[i % COLORS.length]),
                    task.getTaskId(), i == 0);
        }
        ShortCharacter patch = new ShortCharacter();
        patch.setCardStatus("GENERATING");
        patch.setUpdatedTime(new Date());
        ShortCharacterExample up = new ShortCharacterExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortCharacterMapper.updateByExampleSelective(patch, up);
    }

    private void finishScenePortrait(ShortAiTask task, Map<String, Object> req) {
        ShortSceneCardExample ex = new ShortSceneCardExample();
        ex.createCriteria().andSceneCardIdEqualTo(task.getTargetId()).andDeletedFlagEqualTo(0);
        ShortSceneCard row = shortSceneCardMapper.selectOneByExample(ex);
        if (row == null) {
            return;
        }
        int count = ShortJson.integer(req.get("candidate_count"), 4);
        for (int i = 0; i < count; i++) {
            saveSvg(row.getUserId(), row.getWorkId(), null, row.getSceneCardId(), "IMAGE",
                    MockAiProvider.svg(row.getCardName(), "场景候选 " + (i + 1), COLORS[i % COLORS.length]),
                    task.getTaskId(), i == 0);
        }
    }

    private void finishShot(ShortAiTask task) {
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(task.getTargetId()).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(ex);
        if (shot == null) {
            return;
        }
        String svg = MockAiProvider.svg("镜 " + shot.getShotNo(), shot.getShotDesc(), COLORS[Math.abs(shot.getShotNo()) % COLORS.length]);
        ShortAsset asset = saveSvg(shot.getUserId(), shot.getWorkId(), shot.getEpisodeId(), shot.getShotId(),
                "IMAGE".equals(task.getTaskType()) ? "IMAGE" : "VIDEO", svg, task.getTaskId(), true);
        ShortShot patch = new ShortShot();
        if ("VIDEO".equals(task.getTaskType())) {
            patch.setVideoAssetId(asset.getAssetId());
            patch.setShotStatus("VIDEO_READY");
            asset.setDurationSec(shot.getShotDurationSec());
            shortAssetMapper.updateByPrimaryKeySelective(asset);
        } else {
            patch.setFirstFrameAssetId(asset.getAssetId());
            patch.setShotStatus("IMAGE_READY");
            if (Integer.valueOf(1).equals(task.getAutoNextFlag())) {
                aiTaskQueue.enqueue(shot.getUserId(), shot.getWorkId(), shot.getEpisodeId(), "VIDEO", "SHOT",
                        shot.getShotId(), java.util.Map.of("shot_id", shot.getShotId()), task.getBatchId(), 0);
            }
        }
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);
    }

    private void finishCompose(ShortAiTask task) {
        ShortEpisode ep = episode(task.getEpisodeId());
        if (ep == null) {
            return;
        }
        ShortShotExample sx = new ShortShotExample();
        sx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        sx.setOrderByClause("shot_no asc");
        List<ShortShot> shots = shortShotMapper.selectByExample(sx);
        StringBuilder timeline = new StringBuilder();
        timeline.append("{\"episode_id\":\"").append(ep.getEpisodeId()).append("\",\"shots\":[");
        int duration = 0;
        for (int i = 0; i < shots.size(); i++) {
            ShortShot shot = shots.get(i);
            duration += shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec();
            if (i > 0) {
                timeline.append(',');
            }
            timeline.append("{\"shot_no\":").append(shot.getShotNo()).append(",\"desc\":\"")
                    .append(shot.getShotDesc() == null ? "" : shot.getShotDesc().replace("\"", "'"))
                    .append("\"}");
        }
        timeline.append("]}");
        ShortAsset asset = saveSvg(ep.getUserId(), ep.getWorkId(), ep.getEpisodeId(), ep.getEpisodeId(), "FILM",
                MockAiProvider.svg(ep.getEpisodeTitle(), "成片 " + duration + "s", "#111827"), task.getTaskId(), true);
        asset.setDurationSec(duration);
        asset.setStorageKey(timeline.toString());
        shortAssetMapper.updateByPrimaryKeySelective(asset);
        ShortFilmExample fx = new ShortFilmExample();
        fx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        ShortFilm film = shortFilmMapper.selectOneByExample(fx);
        if (film == null) {
            film = new ShortFilm();
            film.setFilmId(ShortIds.id("f-"));
            film.setEpisodeId(ep.getEpisodeId());
            film.setWorkId(ep.getWorkId());
            film.setUserId(ep.getUserId());
            film.setDeletedFlag(0);
            film.setCreatedTime(new Date());
            shortFilmMapper.insertSelective(film);
            film = shortFilmMapper.selectOneByExample(fx);
        }
        ShortFilm patch = new ShortFilm();
        patch.setFilmAssetId(asset.getAssetId());
        patch.setFilmDurationSec(duration);
        patch.setComposeStatus("READY");
        patch.setComposeTaskId(task.getTaskId());
        patch.setComposedTime(new Date());
        patch.setExportJson(timeline.toString());
        patch.setUpdatedTime(new Date());
        ShortFilmExample up = new ShortFilmExample();
        up.createCriteria().andIdEqualTo(film.getId());
        shortFilmMapper.updateByExampleSelective(patch, up);
        ShortEpisode epPatch = new ShortEpisode();
        epPatch.setComposeStatus("READY");
        epPatch.setFilmAssetId(asset.getAssetId());
        epPatch.setFilmDurationSec(duration);
        epPatch.setUpdatedTime(new Date());
        ShortEpisodeExample eup = new ShortEpisodeExample();
        eup.createCriteria().andIdEqualTo(ep.getId());
        shortEpisodeMapper.updateByExampleSelective(epPatch, eup);
        workStatusService.refresh(ep.getWorkId());
    }

    private ShortAsset saveSvg(String userId, String workId, String episodeId, String shotId, String type,
            String svg, String taskId, boolean selected) {
        ShortAsset asset = new ShortAsset();
        asset.setAssetId(ShortIds.id("a-"));
        asset.setUserId(userId);
        asset.setWorkId(workId);
        asset.setEpisodeId(episodeId);
        asset.setShotId(shotId);
        asset.setAssetType(type);
        asset.setStorageKey(svg);
        asset.setFileUrl("/bgssai/user/assets/" + asset.getAssetId());
        asset.setWidth(1080);
        asset.setHeight(1920);
        asset.setSourceTaskId(taskId);
        asset.setSelectedFlag(selected ? 1 : 0);
        asset.setDeletedFlag(0);
        asset.setCreatedTime(new Date());
        shortAssetMapper.insertSelective(asset);
        return asset;
    }

    private ShortEpisode episode(String episodeId) {
        if (episodeId == null) {
            return null;
        }
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        return shortEpisodeMapper.selectOneByExample(ex);
    }

    private void addSpent(ShortAiTask task) {
        if (task.getWorkId() == null || task.getCostCent() == null) {
            return;
        }
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(task.getWorkId()).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        if (work == null) {
            return;
        }
        ShortWork patch = new ShortWork();
        patch.setCostSpentCent((work.getCostSpentCent() == null ? 0 : work.getCostSpentCent()) + task.getCostCent());
        patch.setUpdatedTime(new Date());
        ShortWorkExample up = new ShortWorkExample();
        up.createCriteria().andIdEqualTo(work.getId());
        shortWorkMapper.updateByExampleSelective(patch, up);
    }

    private void fail(ShortAiTask task, String code, String message) {
        ShortAiTask patch = new ShortAiTask();
        patch.setTaskStatus("FAILED");
        patch.setFailReasonCode(code);
        patch.setFailReason(message);
        patch.setFinishedTime(new Date());
        patch.setUpdatedTime(new Date());
        ShortAiTaskExample up = new ShortAiTaskExample();
        up.createCriteria().andIdEqualTo(task.getId());
        shortAiTaskMapper.updateByExampleSelective(patch, up);
    }
}
