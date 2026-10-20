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
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortCharacterRelationMapper;
import com.bgssai.shortai.mapper.ShortContinuityIssueMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortStoryMapMapper;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;
import com.bgssai.shortai.model.ShortCharacterRelation;
import com.bgssai.shortai.model.ShortCharacterRelationExample;
import com.bgssai.shortai.model.ShortContinuityIssue;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortStoryMap;
import com.bgssai.shortai.model.ShortStoryMapExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.FeatureDtos.BlockingSaveReq;
import com.bgssai.shortai.production.dto.FeatureDtos.RelationSaveReq;
import com.bgssai.shortai.production.dto.FeatureDtos.StoryMapGenerateReq;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;

@Service
public class StoryMapService {

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private ShortStoryMapMapper shortStoryMapMapper;
    @Autowired
    private ShortCharacterRelationMapper shortCharacterRelationMapper;
    @Autowired
    private ShortCharacterMapper shortCharacterMapper;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;
    @Autowired
    private ShortContinuityIssueMapper shortContinuityIssueMapper;

    @Transactional
    public ApiResponse generate(StoryMapGenerateReq req) {
        String scope = req == null || req.getMapScope() == null ? "TENSION" : req.getMapScope();
        ShortWork work;
        ShortEpisode ep = null;
        int draftFlag = 0;
        if (req.getEpisodeId() != null && !req.getEpisodeId().isBlank()) {
            ep = workAccessService.requireEpisode(req.getEpisodeId());
            work = workAccessService.requireWork(ep.getWorkId());
            draftFlag = "READY".equals(ep.getScriptStatus()) ? 0 : 1;
        } else {
            work = workAccessService.requireWork(req.getWorkId());
        }
        Map<String, Object> mapJson = new LinkedHashMap<>();
        if ("TENSION".equals(scope) || "ALL".equals(scope)) {
            mapJson.put("tension", buildTension(ep == null ? null : ep.getEpisodeId(), work.getWorkId()));
        }
        if ("RELATION".equals(scope) || "ALL".equals(scope)) {
            generateRelations(work);
            mapJson.put("relations", "ok");
        }
        if ("BLOCKING".equals(scope) || "ALL".equals(scope)) {
            if (ep != null) {
                generateBlocking(ep);
            }
            mapJson.put("blocking", "ok");
        }
        ShortStoryMap row = new ShortStoryMap();
        row.setMapId(ShortIds.id("sm-"));
        row.setWorkId(work.getWorkId());
        row.setEpisodeId(ep == null ? null : ep.getEpisodeId());
        row.setUserId(work.getUserId());
        row.setMapScope(scope);
        row.setMapJson(ShortJson.toJson(mapJson));
        row.setDraftFlag(draftFlag);
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        row.setUpdatedTime(new Date());
        shortStoryMapMapper.insertSelective(row);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("map_id", row.getMapId());
        result.put("draft_flag", draftFlag);
        result.put("map_scope", scope);
        return Responses.ok(result);
    }

    public ApiResponse tension(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        Map<String, Object> tension = buildTension(episodeId, ep.getWorkId());
        ShortStoryMapExample ex = new ShortStoryMapExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("id desc");
        ShortStoryMap map = shortStoryMapMapper.selectOneByExample(ex);
        tension.put("draft_flag", map == null || map.getDraftFlag() == null ? 0 : map.getDraftFlag());
        return Responses.ok(tension);
    }

    public ApiResponse tensionBand(String workId) {
        ShortWork work = workAccessService.requireWork(workId);
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("episode_no asc");
        List<Map<String, Object>> band = new ArrayList<>();
        for (ShortEpisode ep : shortEpisodeMapper.selectByExample(ex)) {
            Map<String, Object> tension = buildTension(ep.getEpisodeId(), workId);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> points = (List<Map<String, Object>>) tension.get("point_list");
            int avg = 50;
            if (points != null && !points.isEmpty()) {
                avg = points.stream().mapToInt(p -> ShortJson.integer(p.get("tension_score"), 50)).sum()
                        / points.size();
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("episode_id", ep.getEpisodeId());
            item.put("episode_no", ep.getEpisodeNo());
            item.put("avg_tension_score", avg);
            item.put("reversal_flag", avg >= 75 ? 1 : 0);
            band.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("band_list", band);
        return Responses.ok(result);
    }

    public ApiResponse relations(String workId, Integer asOfEpisodeNo) {
        ShortWork work = workAccessService.requireWork(workId);
        ShortCharacterExample cx = new ShortCharacterExample();
        cx.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        List<ShortCharacter> chars = shortCharacterMapper.selectByExample(cx);
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (ShortCharacter c : chars) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("character_id", c.getCharacterId());
            n.put("character_name", c.getCharacterName());
            n.put("character_role", c.getCharacterRole());
            n.put("appear_shot_count", 0);
            nodes.add(n);
        }
        ShortCharacterRelationExample rx = new ShortCharacterRelationExample();
        ShortCharacterRelationExample.Criteria crit = rx.createCriteria().andWorkIdEqualTo(work.getWorkId())
                .andDeletedFlagEqualTo(0);
        if (asOfEpisodeNo != null) {
            crit.andAsOfEpisodeNoLessThan(asOfEpisodeNo + 1);
        }
        Map<String, String> nameById = new LinkedHashMap<>();
        for (ShortCharacter c : chars) {
            nameById.put(c.getCharacterId(), c.getCharacterName());
        }
        List<Map<String, Object>> edges = new ArrayList<>();
        for (ShortCharacterRelation r : shortCharacterRelationMapper.selectByExample(rx)) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("relation_id", r.getRelationId());
            e.put("from_character_id", r.getFromCharacterId());
            e.put("from_character_name", nameById.get(r.getFromCharacterId()));
            e.put("to_character_id", r.getToCharacterId());
            e.put("to_character_name", nameById.get(r.getToCharacterId()));
            e.put("relation_type", r.getRelationType());
            e.put("relation_type_name", relationTypeName(r.getRelationType()));
            e.put("relation_strength", 3);
            e.put("reveal_episode_no", r.getAsOfEpisodeNo());
            e.put("revealed_flag", 1);
            e.put("relation_desc", r.getRelationDesc());
            edges.add(e);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("node_list", nodes);
        result.put("edge_list", edges);
        result.put("offline_list", new ArrayList<>());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse relationSave(RelationSaveReq req) {
        ShortWork work = workAccessService.requireWork(req == null ? null : req.getWorkId());
        if (Integer.valueOf(1).equals(req.getDeletedFlag()) && req.getRelationId() != null) {
            ShortCharacterRelationExample ex = new ShortCharacterRelationExample();
            ex.createCriteria().andRelationIdEqualTo(req.getRelationId()).andDeletedFlagEqualTo(0);
            ShortCharacterRelation patch = new ShortCharacterRelation();
            patch.setDeletedFlag(1);
            patch.setUpdatedTime(new Date());
            shortCharacterRelationMapper.updateByExampleSelective(patch, ex);
            return Responses.ok("ok");
        }
        if (req.getRelationId() != null && !req.getRelationId().isBlank()) {
            ShortCharacterRelationExample ex = new ShortCharacterRelationExample();
            ex.createCriteria().andRelationIdEqualTo(req.getRelationId()).andDeletedFlagEqualTo(0);
            ShortCharacterRelation row = shortCharacterRelationMapper.selectOneByExample(ex);
            if (row == null) {
                throw new ForbiddenException("relation not found");
            }
            ShortCharacterRelation patch = new ShortCharacterRelation();
            if (req.getRelationType() != null) {
                patch.setRelationType(req.getRelationType());
            }
            if (req.getRelationDesc() != null) {
                patch.setRelationDesc(req.getRelationDesc());
            }
            if (req.getAsOfEpisodeNo() != null) {
                patch.setAsOfEpisodeNo(req.getAsOfEpisodeNo());
            }
            patch.setUpdatedTime(new Date());
            ShortCharacterRelationExample up = new ShortCharacterRelationExample();
            up.createCriteria().andIdEqualTo(row.getId());
            shortCharacterRelationMapper.updateByExampleSelective(patch, up);
            return Responses.ok(Map.of("relation_id", row.getRelationId()));
        }
        ShortCharacterRelation row = new ShortCharacterRelation();
        row.setRelationId(ShortIds.id("rl-"));
        row.setWorkId(work.getWorkId());
        row.setUserId(work.getUserId());
        row.setFromCharacterId(req.getFromCharacterId());
        row.setToCharacterId(req.getToCharacterId());
        row.setRelationType(req.getRelationType() == null ? "RELATED" : req.getRelationType());
        row.setAsOfEpisodeNo(req.getAsOfEpisodeNo() == null ? 1 : req.getAsOfEpisodeNo());
        row.setRelationDesc(req.getRelationDesc());
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        row.setUpdatedTime(new Date());
        shortCharacterRelationMapper.insertSelective(row);
        return Responses.ok(Map.of("relation_id", row.getRelationId()));
    }

    public ApiResponse blocking(String shotId) {
        ShortShot shot = requireShot(shotId);
        Map<String, Object> blocking = ShortJson.map(shot.getBlockingJson());
        if (blocking.isEmpty()) {
            blocking = defaultBlocking(shot);
        }
        blocking.put("shot_id", shot.getShotId());
        blocking.put("shot_no", shot.getShotNo());
        blocking.put("axis_flag", shot.getAxisFlag() == null ? 0 : shot.getAxisFlag());
        return Responses.ok(blocking);
    }

    @Transactional
    public ApiResponse blockingSave(BlockingSaveReq req) {
        ShortShot shot = requireShot(req == null ? null : req.getShotId());
        Map<String, Object> blocking = req.getBlocking() == null ? defaultBlocking(shot) : req.getBlocking();
        ShortShot patch = new ShortShot();
        patch.setBlockingJson(ShortJson.toJson(blocking));
        if (req.getAxisFlag() != null) {
            patch.setAxisFlag(req.getAxisFlag());
            if (Integer.valueOf(1).equals(req.getAxisFlag())) {
                ShortContinuityIssue issue = new ShortContinuityIssue();
                issue.setIssueId(ShortIds.id("ci-"));
                issue.setEpisodeId(shot.getEpisodeId());
                issue.setWorkId(shot.getWorkId());
                issue.setIssueType("EYELINE");
                issue.setSeverity("MAJOR");
                issue.setIssueDesc("镜头存在越轴风险");
                issue.setShotId(shot.getShotId());
                issue.setHandleStatus("OPEN");
                issue.setDeletedFlag(0);
                issue.setCreatedTime(new Date());
                issue.setUpdatedTime(new Date());
                shortContinuityIssueMapper.insertSelective(issue);
            }
        }
        patch.setUpdatedTime(new Date());
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId());
        shortShotMapper.updateByExampleSelective(patch, up);
        return Responses.ok(blocking);
    }

    public ApiResponse sceneBand(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        List<ShortShot> shots = listShots(ep.getEpisodeId());
        Map<Integer, Integer> dur = new LinkedHashMap<>();
        int total = 0;
        for (ShortShot s : shots) {
            int d = s.getShotDurationSec() == null ? 3 : s.getShotDurationSec();
            int scene = s.getSceneNo() == null ? 1 : s.getSceneNo();
            dur.merge(scene, d, Integer::sum);
            total += d;
        }
        List<Map<String, Object>> band = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : dur.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("scene_no", e.getKey());
            item.put("duration_sec", e.getValue());
            item.put("ratio", total == 0 ? 0 : (double) e.getValue() / total);
            item.put("split_hint_flag", total > 0 && e.getValue() * 1.0 / total > 0.4 ? 1 : 0);
            band.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scene_band_list", band);
        result.put("total_sec", total);
        return Responses.ok(result);
    }

    public ApiResponse appearanceHeat(String workId) {
        ShortWork work = workAccessService.requireWork(workId);
        ShortCharacterExample cx = new ShortCharacterExample();
        cx.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        List<Map<String, Object>> heat = new ArrayList<>();
        for (ShortCharacter c : shortCharacterMapper.selectByExample(cx)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("character_id", c.getCharacterId());
            item.put("character_name", c.getCharacterName());
            item.put("heat_score", 50);
            heat.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("heat_list", heat);
        return Responses.ok(result);
    }

    private Map<String, Object> buildTension(String episodeId, String workId) {
        List<ShortShot> shots = episodeId == null ? List.of() : listShots(episodeId);
        List<Map<String, Object>> points = new ArrayList<>();
        List<Map<String, Object>> expect = new ArrayList<>();
        List<Map<String, Object>> deviation = new ArrayList<>();
        int t = 0;
        int total = 0;
        for (ShortShot s : shots) {
            total += s.getShotDurationSec() == null ? 3 : s.getShotDurationSec();
        }
        for (ShortShot s : shots) {
            int dur = s.getShotDurationSec() == null ? 3 : s.getShotDurationSec();
            int score = 40;
            if (s.getEmotion() != null && (s.getEmotion().contains("怒") || "ANGRY".equals(s.getEmotion()))) {
                score = 80;
            } else if (s.getDialogueText() != null && s.getDialogueText().length() > 20) {
                score = 65;
            } else if (s.getShotNo() != null && s.getShotNo() <= 2) {
                score = 75;
            }
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("at_sec", t);
            p.put("tension_score", score);
            p.put("shot_no", s.getShotNo());
            p.put("scene_no", s.getSceneNo());
            points.add(p);
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("at_sec", t);
            e.put("tension_score", t < 3 ? 80 : (total - t <= 5 ? 78 : 60));
            expect.add(e);
            t += dur;
        }
        if (points.size() >= 3) {
            int flatStart = -1;
            for (int i = 0; i < points.size(); i++) {
                int score = ShortJson.integer(points.get(i).get("tension_score"), 50);
                if (score < 50) {
                    if (flatStart < 0) {
                        flatStart = i;
                    }
                } else if (flatStart >= 0 && i - flatStart >= 2) {
                    Map<String, Object> d = new LinkedHashMap<>();
                    d.put("deviation_type", "FLAT");
                    d.put("start_sec", points.get(flatStart).get("at_sec"));
                    d.put("end_sec", points.get(i - 1).get("at_sec"));
                    d.put("issue_desc", "张力偏低，观众易划走");
                    d.put("suggestion", "提前埋反转或加速冲突");
                    d.put("fix_action", "REWRITE_SCENE");
                    deviation.add(d);
                    flatStart = -1;
                } else {
                    flatStart = -1;
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("episode_id", episodeId);
        result.put("work_id", workId);
        result.put("duration_sec", total);
        result.put("point_list", points);
        result.put("expect_list", expect);
        result.put("deviation_list", deviation);
        return result;
    }

    private void generateRelations(ShortWork work) {
        ShortCharacterExample cx = new ShortCharacterExample();
        cx.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        List<ShortCharacter> chars = shortCharacterMapper.selectByExample(cx);
        if (chars.size() < 2) {
            return;
        }
        ShortCharacterRelationExample existing = new ShortCharacterRelationExample();
        existing.createCriteria().andWorkIdEqualTo(work.getWorkId()).andDeletedFlagEqualTo(0);
        if (!shortCharacterRelationMapper.selectByExample(existing).isEmpty()) {
            return;
        }
        ShortCharacterRelation row = new ShortCharacterRelation();
        row.setRelationId(ShortIds.id("rl-"));
        row.setWorkId(work.getWorkId());
        row.setUserId(work.getUserId());
        row.setFromCharacterId(chars.get(0).getCharacterId());
        row.setToCharacterId(chars.get(1).getCharacterId());
        row.setRelationType("RIVAL");
        row.setAsOfEpisodeNo(1);
        row.setRelationDesc("自动抽取关系");
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        row.setUpdatedTime(new Date());
        shortCharacterRelationMapper.insertSelective(row);
    }

    private void generateBlocking(ShortEpisode ep) {
        List<ShortShot> shots = listShots(ep.getEpisodeId());
        ShortShot prev = null;
        for (ShortShot shot : shots) {
            Map<String, Object> blocking = defaultBlocking(shot);
            ShortShot patch = new ShortShot();
            patch.setBlockingJson(ShortJson.toJson(blocking));
            int axis = 0;
            if (prev != null && prev.getCameraMove() != null && shot.getCameraMove() != null
                    && !prev.getCameraMove().equals(shot.getCameraMove())
                    && "REVERSE".equals(shot.getCameraMove())) {
                axis = 1;
                ShortContinuityIssue issue = new ShortContinuityIssue();
                issue.setIssueId(ShortIds.id("ci-"));
                issue.setEpisodeId(ep.getEpisodeId());
                issue.setWorkId(ep.getWorkId());
                issue.setIssueType("EYELINE");
                issue.setSeverity("MAJOR");
                issue.setIssueDesc("相邻镜头疑似越轴");
                issue.setShotId(shot.getShotId());
                issue.setRelatedShotId(prev.getShotId());
                issue.setHandleStatus("OPEN");
                issue.setDeletedFlag(0);
                issue.setCreatedTime(new Date());
                issue.setUpdatedTime(new Date());
                shortContinuityIssueMapper.insertSelective(issue);
            }
            patch.setAxisFlag(axis);
            patch.setUpdatedTime(new Date());
            ShortShotExample up = new ShortShotExample();
            up.createCriteria().andIdEqualTo(shot.getId());
            shortShotMapper.updateByExampleSelective(patch, up);
            prev = shot;
        }
    }

    private Map<String, Object> defaultBlocking(ShortShot shot) {
        Map<String, Object> blocking = new LinkedHashMap<>();
        Map<String, Object> stage = new LinkedHashMap<>();
        stage.put("scene_card_id", shot.getSceneCardId());
        stage.put("stage_desc", shot.getShotDesc());
        stage.put("prop_list", new ArrayList<>());
        blocking.put("stage", stage);
        blocking.put("actor_list", new ArrayList<>());
        Map<String, Object> camera = new LinkedHashMap<>();
        camera.put("x", 0.5);
        camera.put("y", 0.86);
        camera.put("angle", 0);
        camera.put("shot_size", shot.getShotSize());
        camera.put("camera_move", shot.getCameraMove());
        blocking.put("camera", camera);
        return blocking;
    }

    private List<ShortShot> listShots(String episodeId) {
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("shot_no asc");
        return shortShotMapper.selectByExample(ex);
    }

    private ShortShot requireShot(String shotId) {
        if (shotId == null || shotId.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "shot_id required");
        }
        String userId = workAccessService.requireUser();
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(shotId).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(ex);
        if (shot == null || !userId.equals(shot.getUserId())) {
            throw new ForbiddenException("shot not found");
        }
        return shot;
    }

    private static String relationTypeName(String code) {
        if (code == null || code.isBlank()) {
            return "-";
        }
        return switch (code) {
            case "MENTOR" -> "师徒";
            case "RIVAL" -> "对立";
            case "RELATED" -> "关联";
            case "ALLY" -> "同盟";
            case "FAMILY" -> "亲属";
            case "LOVE" -> "情感";
            case "ENEMY" -> "敌对";
            default -> code;
        };
    }
}
