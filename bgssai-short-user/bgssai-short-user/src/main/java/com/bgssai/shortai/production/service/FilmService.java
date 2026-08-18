package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortFilmMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortAssetExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortFilm;
import com.bgssai.shortai.model.ShortFilmExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.production.dto.ProductionDtos.FilmReq;
import com.bgssai.shortai.production.support.ComplianceLexicon;

@Service
public class FilmService {

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private AiTaskQueue aiTaskQueue;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortFilmMapper shortFilmMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;
    @Autowired
    private ShortAssetMapper shortAssetMapper;

    public ApiResponse precheck(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        List<ShortShot> shots = shortShotMapper.selectByExample(ex);
        List<Map<String, Object>> unlocked = new ArrayList<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        for (ShortShot shot : shots) {
            if (!"LOCKED".equals(shot.getShotStatus()) && !"VIDEO_READY".equals(shot.getShotStatus())
                    && !"IMAGE_READY".equals(shot.getShotStatus())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("shot_id", shot.getShotId());
                item.put("shot_no", shot.getShotNo());
                item.put("shot_status", shot.getShotStatus());
                unlocked.add(item);
            }
            parts.add(ComplianceLexicon.scan(shot.getDialogueText(), shot.getSceneNo(), null, shot.getShotNo()));
        }
        Map<String, Object> scan = ComplianceLexicon.merge(parts);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ready", unlocked.isEmpty() && !"BLOCK".equals(scan.get("risk_level")));
        result.put("unlocked_shots", unlocked);
        result.put("risk_level", scan.get("risk_level"));
        result.put("hit_list", scan.get("hit_list"));
        return Responses.ok(result);
    }

    public ApiResponse timeline(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("shot_no asc");
        List<Map<String, Object>> clips = new ArrayList<>();
        int t = 0;
        for (ShortShot shot : shortShotMapper.selectByExample(ex)) {
            int dur = shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec();
            Map<String, Object> clip = new LinkedHashMap<>();
            clip.put("shot_id", shot.getShotId());
            clip.put("shot_no", shot.getShotNo());
            clip.put("start_sec", t);
            clip.put("duration_sec", dur);
            clip.put("shot_desc", shot.getShotDesc());
            clips.add(clip);
            t += dur;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clip_list", clips);
        result.put("total_sec", t);
        return Responses.ok(result);
    }

    public ApiResponse compose(FilmReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        Map<String, Object> check = precheckMap(ep.getEpisodeId());
        if (!Boolean.TRUE.equals(check.get("ready"))) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "precheck not ready");
        }
        patchCompose(ep, "COMPOSING");
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("episode_id", ep.getEpisodeId());
        ShortAiTask task = aiTaskQueue.enqueue(ep.getUserId(), ep.getWorkId(), ep.getEpisodeId(), "COMPOSE", "COMPOSE",
                ep.getEpisodeId(), request, null, 0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task_id", task.getTaskId());
        return Responses.ok(result);
    }

    public ApiResponse detail(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        ShortFilmExample ex = new ShortFilmExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        ShortFilm film = shortFilmMapper.selectOneByExample(ex);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("episode_id", ep.getEpisodeId());
        result.put("compose_status", ep.getComposeStatus());
        if (film != null) {
            result.put("film_id", film.getFilmId());
            result.put("film_asset_id", film.getFilmAssetId());
            result.put("film_duration_sec", film.getFilmDurationSec());
            result.put("download_url", film.getFilmAssetId() == null ? null
                    : "/bgssai/user/assets/" + film.getFilmAssetId());
        }
        return Responses.ok(result);
    }

    public ApiResponse downloadUrl(String assetId) {
        String userId = workAccessService.requireUser();
        ShortAssetExample ex = new ShortAssetExample();
        ex.createCriteria().andAssetIdEqualTo(assetId).andDeletedFlagEqualTo(0);
        ShortAsset asset = shortAssetMapper.selectOneByExample(ex);
        if (asset == null || !userId.equals(asset.getUserId())) {
            throw new ForbiddenException("asset not found");
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 30);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("download_url", "/bgssai/user/assets/" + assetId);
        result.put("expires_at", cal.getTime());
        return Responses.ok(result);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> precheckMap(String episodeId) {
        return (Map<String, Object>) precheck(episodeId).getResult();
    }

    private void patchCompose(ShortEpisode ep, String status) {
        ShortEpisode patch = new ShortEpisode();
        patch.setComposeStatus(status);
        patch.setUpdatedTime(new Date());
        ShortEpisodeExample up = new ShortEpisodeExample();
        up.createCriteria().andIdEqualTo(ep.getId());
        shortEpisodeMapper.updateByExampleSelective(patch, up);
    }
}
