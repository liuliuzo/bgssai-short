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
import com.bgssai.shortai.mapper.ShortDubbingMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortSubtitleMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;
import com.bgssai.shortai.model.ShortDubbing;
import com.bgssai.shortai.model.ShortDubbingExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortSubtitle;
import com.bgssai.shortai.model.ShortSubtitleExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;
import com.bgssai.shortai.production.dto.FeatureDtos.ApplyVoiceReq;
import com.bgssai.shortai.production.dto.FeatureDtos.DubbingAlignBatchReq;
import com.bgssai.shortai.production.dto.FeatureDtos.DubbingAlignReq;
import com.bgssai.shortai.production.dto.FeatureDtos.DubbingRegenReq;
import com.bgssai.shortai.production.dto.FeatureDtos.DubbingSubmitReq;
import com.bgssai.shortai.production.dto.FeatureDtos.SubtitleRegenReq;
import com.bgssai.shortai.production.dto.FeatureDtos.SubtitleStyleSaveReq;
import com.bgssai.shortai.production.dto.FeatureDtos.SubtitleTranscribeReq;
import com.bgssai.shortai.production.dto.FeatureDtos.SubtitleUpdateReq;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;

@Service
public class DubbingService {

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private AiTaskQueue aiTaskQueue;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortDubbingMapper shortDubbingMapper;
    @Autowired
    private ShortSubtitleMapper shortSubtitleMapper;
    @Autowired
    private ShortCharacterMapper shortCharacterMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;
    @Autowired
    private ShortWorkMapper shortWorkMapper;

    @Transactional
    public ApiResponse submit(DubbingSubmitReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        List<ShortShot> shots = listShots(ep.getEpisodeId());
        if (req.getShotIdList() != null && !req.getShotIdList().isEmpty()) {
            shots = shots.stream().filter(s -> req.getShotIdList().contains(s.getShotId())).toList();
        }
        ShortWork work = workAccessService.requireWork(ep.getWorkId());
        Map<String, ShortCharacter> chars = charactersOf(ep.getWorkId());
        String batchId = ShortIds.id("b-");
        List<String> taskIds = new ArrayList<>();
        int lineSeed = 1;
        for (ShortShot shot : shots) {
            String dialogue = shot.getDialogueText();
            if (dialogue == null || dialogue.isBlank()) {
                continue;
            }
            softDeleteDubbing(shot.getShotId());
            ShortCharacter lead = resolveCharacter(shot, chars);
            String voiceId = lead != null && lead.getVoiceId() != null && !lead.getVoiceId().isBlank()
                    ? lead.getVoiceId()
                    : (work.getNarratorVoiceId() == null ? "v-narrator-01" : work.getNarratorVoiceId());
            ShortDubbing row = new ShortDubbing();
            row.setDubbingId(ShortIds.id("db-"));
            row.setShotId(shot.getShotId());
            row.setEpisodeId(ep.getEpisodeId());
            row.setWorkId(ep.getWorkId());
            row.setUserId(ep.getUserId());
            row.setLineNo(lineSeed++);
            row.setCharacterId(lead == null ? null : lead.getCharacterId());
            row.setVoiceId(voiceId);
            row.setVoiceSpeed("1.0");
            row.setEmotion(shot.getEmotion() == null ? "NEUTRAL" : shot.getEmotion());
            row.setLineText(dialogue.trim());
            row.setAlignStatus("PENDING");
            row.setAutoFixedFlag(0);
            row.setDeletedFlag(0);
            row.setCreatedTime(new Date());
            row.setUpdatedTime(new Date());
            shortDubbingMapper.insertSelective(row);
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("dubbing_id", row.getDubbingId());
            request.put("shot_id", shot.getShotId());
            request.put("line_text", row.getLineText());
            request.put("voice_id", voiceId);
            request.put("voice_speed", row.getVoiceSpeed());
            request.put("emotion", row.getEmotion());
            request.put("model_code", req.getModelCode());
            ShortAiTask task = aiTaskQueue.enqueue(ep.getUserId(), ep.getWorkId(), ep.getEpisodeId(), "VOICE",
                    "DUBBING", row.getDubbingId(), request, batchId, 0, req.getModelCode());
            taskIds.add(task.getTaskId());
        }
        if (taskIds.isEmpty()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "no dialogue shots to dub");
        }
        patchEpisodeDubbing(ep, "GENERATING");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch_id", batchId);
        result.put("task_id_list", taskIds);
        result.put("task_count", taskIds.size());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse regenerate(DubbingRegenReq req) {
        if (req == null || req.getShotId() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "shot_id required");
        }
        ShortShot shot = requireShot(req.getShotId());
        ShortDubbingExample ex = new ShortDubbingExample();
        ShortDubbingExample.Criteria c = ex.createCriteria().andShotIdEqualTo(shot.getShotId())
                .andDeletedFlagEqualTo(0);
        if (req.getLineNo() != null) {
            c.andLineNoEqualTo(req.getLineNo());
        }
        List<ShortDubbing> rows = shortDubbingMapper.selectByExample(ex);
        if (rows.isEmpty()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "dubbing not found");
        }
        String batchId = ShortIds.id("b-");
        List<String> taskIds = new ArrayList<>();
        for (ShortDubbing row : rows) {
            ShortDubbing patch = new ShortDubbing();
            if (req.getVoiceId() != null) {
                patch.setVoiceId(req.getVoiceId());
            }
            if (req.getVoiceSpeed() != null) {
                patch.setVoiceSpeed(req.getVoiceSpeed());
            }
            if (req.getEmotion() != null) {
                patch.setEmotion(req.getEmotion());
            }
            patch.setAlignStatus("PENDING");
            patch.setAudioAssetId(null);
            patch.setUpdatedTime(new Date());
            ShortDubbingExample up = new ShortDubbingExample();
            up.createCriteria().andIdEqualTo(row.getId());
            shortDubbingMapper.updateByExampleSelective(patch, up);
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("dubbing_id", row.getDubbingId());
            request.put("shot_id", row.getShotId());
            request.put("line_text", row.getLineText());
            request.put("voice_id", req.getVoiceId() == null ? row.getVoiceId() : req.getVoiceId());
            request.put("voice_speed", req.getVoiceSpeed() == null ? row.getVoiceSpeed() : req.getVoiceSpeed());
            request.put("emotion", req.getEmotion() == null ? row.getEmotion() : req.getEmotion());
            request.put("model_code", req.getModelCode());
            ShortAiTask task = aiTaskQueue.enqueue(shot.getUserId(), shot.getWorkId(), shot.getEpisodeId(), "VOICE",
                    "DUBBING", row.getDubbingId(), request, batchId, 0, req.getModelCode());
            taskIds.add(task.getTaskId());
        }
        ShortEpisode ep = workAccessService.requireEpisode(shot.getEpisodeId());
        patchEpisodeDubbing(ep, "GENERATING");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch_id", batchId);
        result.put("task_id_list", taskIds);
        return Responses.ok(result);
    }

    public ApiResponse list(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        List<ShortShot> shots = listShots(ep.getEpisodeId());
        ShortDubbingExample dex = new ShortDubbingExample();
        dex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        dex.setOrderByClause("line_no asc");
        List<ShortDubbing> dubbings = shortDubbingMapper.selectByExample(dex);
        Map<String, List<ShortDubbing>> byShot = new LinkedHashMap<>();
        for (ShortDubbing d : dubbings) {
            byShot.computeIfAbsent(d.getShotId(), k -> new ArrayList<>()).add(d);
        }
        Map<String, ShortCharacter> chars = charactersOf(ep.getWorkId());
        List<Map<String, Object>> dubbingList = new ArrayList<>();
        int misaligned = 0;
        for (ShortShot shot : shots) {
            List<ShortDubbing> lines = byShot.getOrDefault(shot.getShotId(), List.of());
            if (lines.isEmpty() && (shot.getDialogueText() == null || shot.getDialogueText().isBlank())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("shot_id", shot.getShotId());
            item.put("shot_no", shot.getShotNo());
            item.put("shot_duration_sec", shot.getShotDurationSec());
            List<Map<String, Object>> lineList = new ArrayList<>();
            int audioSum = 0;
            String alignStatus = "PENDING";
            String suggest = null;
            int autoFixed = 0;
            double gap = 0;
            for (ShortDubbing d : lines) {
                Map<String, Object> line = new LinkedHashMap<>();
                line.put("dubbing_id", d.getDubbingId());
                line.put("line_no", d.getLineNo());
                line.put("character_id", d.getCharacterId());
                ShortCharacter ch = d.getCharacterId() == null ? null : chars.get(d.getCharacterId());
                line.put("character_name", ch == null ? "旁白" : ch.getCharacterName());
                line.put("voice_id", d.getVoiceId());
                line.put("voice_speed", d.getVoiceSpeed());
                line.put("emotion", d.getEmotion());
                line.put("line_text", d.getLineText());
                line.put("audio_asset_id", d.getAudioAssetId());
                line.put("audio_duration_sec", d.getAudioDurationSec());
                lineList.add(line);
                audioSum += d.getAudioDurationSec() == null ? 0 : d.getAudioDurationSec();
                alignStatus = d.getAlignStatus();
                autoFixed = d.getAutoFixedFlag() == null ? 0 : d.getAutoFixedFlag();
                gap = parseGap(d.getAlignGapSec());
                suggest = suggestAction(shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec(),
                        d.getAudioDurationSec() == null ? 0 : d.getAudioDurationSec(), alignStatus);
            }
            item.put("line_list", lineList);
            item.put("audio_duration_sec", audioSum);
            item.put("align_status", alignStatus);
            item.put("align_status_name", alignName(alignStatus));
            item.put("align_gap_sec", gap);
            item.put("suggest_action", suggest);
            item.put("auto_fixed_flag", autoFixed);
            if ("MISALIGNED".equals(alignStatus)) {
                misaligned++;
            }
            dubbingList.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dubbing_list", dubbingList);
        result.put("misaligned_count", misaligned);
        result.put("dubbing_status", ep.getDubbingStatus());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse align(DubbingAlignReq req) {
        if (req == null || req.getShotId() == null || req.getAlignAction() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "shot_id and align_action required");
        }
        ShortShot shot = requireShot(req.getShotId());
        ShortDubbingExample ex = new ShortDubbingExample();
        ex.createCriteria().andShotIdEqualTo(shot.getShotId()).andDeletedFlagEqualTo(0);
        List<ShortDubbing> rows = shortDubbingMapper.selectByExample(ex);
        if (rows.isEmpty()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "dubbing not found");
        }
        String action = req.getAlignAction().trim();
        List<Map<String, Object>> fixed = new ArrayList<>();
        for (ShortDubbing row : rows) {
            Map<String, Object> one = applyAlign(shot, row, action, true);
            fixed.add(one);
        }
        refreshEpisodeStatus(shot.getEpisodeId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fixed_list", fixed);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse alignBatch(DubbingAlignBatchReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        ShortDubbingExample ex = new ShortDubbingExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0)
                .andAlignStatusEqualTo("MISALIGNED");
        List<ShortDubbing> rows = shortDubbingMapper.selectByExample(ex);
        List<Map<String, Object>> fixed = new ArrayList<>();
        List<Map<String, Object>> pending = new ArrayList<>();
        for (ShortDubbing row : rows) {
            ShortShot shot = requireShot(row.getShotId());
            int shotDur = shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec();
            int audioDur = row.getAudioDurationSec() == null ? 0 : row.getAudioDurationSec();
            String suggest = suggestAction(shotDur, audioDur, "MISALIGNED");
            if ("PAD_SILENCE".equals(suggest) || "ADJUST_SPEED".equals(suggest)) {
                fixed.add(applyAlign(shot, row, suggest, true));
            } else {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("shot_id", shot.getShotId());
                item.put("shot_no", shot.getShotNo());
                item.put("suggest_action", "EXTEND_SHOT");
                item.put("align_gap_sec", audioDur - shotDur);
                item.put("estimated_cost_cent", 150);
                pending.add(item);
            }
        }
        refreshEpisodeStatus(ep.getEpisodeId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fixed_list", fixed);
        result.put("pending_extend_list", pending);
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse applyVoiceToCharacter(ApplyVoiceReq req) {
        if (req == null || req.getCharacterId() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "character_id required");
        }
        String userId = workAccessService.requireUser();
        ShortCharacterExample ex = new ShortCharacterExample();
        ex.createCriteria().andCharacterIdEqualTo(req.getCharacterId()).andDeletedFlagEqualTo(0);
        ShortCharacter row = shortCharacterMapper.selectOneByExample(ex);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new ForbiddenException("character not found");
        }
        ShortCharacter patch = new ShortCharacter();
        if (req.getVoiceId() != null) {
            patch.setVoiceId(req.getVoiceId());
        }
        patch.setUpdatedTime(new Date());
        ShortCharacterExample up = new ShortCharacterExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortCharacterMapper.updateByExampleSelective(patch, up);
        return Responses.ok("ok");
    }

    public ApiResponse voices() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(voice("v-female-cold-01", "冷淡女声", "FEMALE", "20-30", "COLD"));
        list.add(voice("v-female-warm-01", "温暖女声", "FEMALE", "20-30", "WARM"));
        list.add(voice("v-male-deep-01", "低沉男声", "MALE", "25-40", "DEEP"));
        list.add(voice("v-male-bright-01", "明亮男声", "MALE", "20-30", "BRIGHT"));
        list.add(voice("v-narrator-01", "旁白", "NEUTRAL", "30-50", "NARRATOR"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("voice_list", list);
        return Responses.ok(result);
    }

    public ApiResponse subtitleDetail(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        ShortWork work = workAccessService.requireWork(ep.getWorkId());
        ShortSubtitleExample ex = new ShortSubtitleExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("subtitle_no asc");
        List<Map<String, Object>> items = new ArrayList<>();
        for (ShortSubtitle s : shortSubtitleMapper.selectByExample(ex)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("subtitle_id", s.getSubtitleId());
            item.put("subtitle_no", s.getSubtitleNo());
            item.put("start_ms", s.getStartMs());
            item.put("end_ms", s.getEndMs());
            item.put("subtitle_text", s.getSubtitleText());
            item.put("shot_id", s.getShotId());
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("episode_id", ep.getEpisodeId());
        result.put("subtitle_list", items);
        result.put("subtitle_style", ShortJson.map(work.getSubtitleStyleJson()));
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse subtitleRegenerate(SubtitleRegenReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        regenerateSubtitles(ep);
        refreshEpisodeStatus(ep.getEpisodeId());
        return subtitleDetail(ep.getEpisodeId());
    }

    @Transactional
    public ApiResponse transcribe(SubtitleTranscribeReq req) {
        ShortEpisode ep = workAccessService.requireEpisode(req == null ? null : req.getEpisodeId());
        if (hasManualSubtitleEdits(ep.getEpisodeId())
                && !Integer.valueOf(1).equals(req.getConfirmOverwriteFlag())) {
            throw new BizException(CommonErrorCode.RULE_REJECT,
                    "subtitles contain manual edits; set confirm_overwrite_flag=1 to continue");
        }
        ShortDubbingExample ex = new ShortDubbingExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("line_no asc");
        List<ShortDubbing> rows = shortDubbingMapper.selectByExample(ex).stream()
                .filter(row -> row.getAudioAssetId() != null && !row.getAudioAssetId().isBlank())
                .toList();
        if (rows.isEmpty()) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "no dubbing audio to transcribe");
        }
        softDeleteSubtitles(ep.getEpisodeId());
        String batchId = ShortIds.id("b-");
        List<String> taskIds = new ArrayList<>();
        for (ShortDubbing row : rows) {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("dubbing_id", row.getDubbingId());
            request.put("audio_asset_id", row.getAudioAssetId());
            request.put("duration_sec", row.getAudioDurationSec());
            request.put("confirm_overwrite_flag", req.getConfirmOverwriteFlag());
            request.put("model_code", req.getModelCode());
            ShortAiTask task = aiTaskQueue.enqueue(ep.getUserId(), ep.getWorkId(), ep.getEpisodeId(), "ASR",
                    "SUBTITLE_TRANSCRIBE", row.getDubbingId(), request, batchId, 0, req.getModelCode());
            taskIds.add(task.getTaskId());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch_id", batchId);
        result.put("task_id_list", taskIds);
        result.put("task_count", taskIds.size());
        return Responses.ok(result);
    }

    @Transactional
    public ApiResponse subtitleUpdate(SubtitleUpdateReq req) {
        if (req == null || req.getSubtitleId() == null) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "subtitle_id required");
        }
        String userId = workAccessService.requireUser();
        ShortSubtitleExample ex = new ShortSubtitleExample();
        ex.createCriteria().andSubtitleIdEqualTo(req.getSubtitleId()).andDeletedFlagEqualTo(0);
        ShortSubtitle row = shortSubtitleMapper.selectOneByExample(ex);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new ForbiddenException("subtitle not found");
        }
        ShortSubtitle patch = new ShortSubtitle();
        if (req.getSubtitleText() != null) {
            patch.setSubtitleText(req.getSubtitleText());
        }
        if (req.getStartMs() != null) {
            patch.setStartMs(req.getStartMs());
        }
        if (req.getEndMs() != null) {
            patch.setEndMs(req.getEndMs());
        }
        patch.setUpdatedTime(new Date());
        ShortSubtitleExample up = new ShortSubtitleExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortSubtitleMapper.updateByExampleSelective(patch, up);
        return Responses.ok("ok");
    }

    @Transactional
    public ApiResponse styleSave(SubtitleStyleSaveReq req) {
        ShortWork work = workAccessService.requireWork(req == null ? null : req.getWorkId());
        Map<String, Object> style = new LinkedHashMap<>();
        style.put("font_size", req.getFontSize() == null ? 36 : req.getFontSize());
        style.put("font_color", req.getFontColor() == null ? "#FFFFFF" : req.getFontColor());
        style.put("stroke_color", req.getStrokeColor() == null ? "#000000" : req.getStrokeColor());
        style.put("position", req.getPosition() == null ? "BOTTOM" : req.getPosition());
        ShortWork patch = new ShortWork();
        patch.setSubtitleStyleJson(ShortJson.toJson(style));
        patch.setUpdatedTime(new Date());
        ShortWorkExample up = new ShortWorkExample();
        up.createCriteria().andIdEqualTo(work.getId());
        shortWorkMapper.updateByExampleSelective(patch, up);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subtitle_style", style);
        return Responses.ok(result);
    }

    public void finishDubbingAlign(ShortDubbing row, ShortShot shot, String audioAssetId, int audioDurationSec) {
        int shotDur = shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec();
        double gap = audioDurationSec - shotDur;
        ShortDubbing patch = new ShortDubbing();
        patch.setAudioAssetId(audioAssetId);
        patch.setAudioDurationSec(audioDurationSec);
        patch.setAlignGapSec(String.valueOf(gap));
        patch.setUpdatedTime(new Date());
        if (Math.abs(gap) <= 0.5) {
            patch.setAlignStatus("ALIGNED");
            patch.setAutoFixedFlag(0);
        } else if (gap < 0) {
            patch.setAlignStatus("ALIGNED");
            patch.setAutoFixedFlag(1);
            patch.setAlignGapSec(String.valueOf(gap));
        } else if (audioDurationSec <= shotDur * 1.25) {
            patch.setAlignStatus("ALIGNED");
            patch.setAutoFixedFlag(1);
            patch.setVoiceSpeed(String.format("%.2f", Math.min(1.25, (double) audioDurationSec / shotDur)));
        } else {
            patch.setAlignStatus("MISALIGNED");
            patch.setAutoFixedFlag(0);
        }
        ShortDubbingExample up = new ShortDubbingExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortDubbingMapper.updateByExampleSelective(patch, up);
        refreshEpisodeStatus(shot.getEpisodeId());
    }

    @Transactional
    public void finishTranscription(ShortDubbing dubbing, Map<String, Object> output) {
        if (dubbing == null) {
            return;
        }
        ShortShotExample shotExample = new ShortShotExample();
        shotExample.createCriteria().andShotIdEqualTo(dubbing.getShotId()).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(shotExample);
        if (shot == null || !dubbing.getUserId().equals(shot.getUserId())) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "shot not found for transcription");
        }
        int episodeOffsetMs = 0;
        for (ShortShot candidate : listShots(dubbing.getEpisodeId())) {
            if (candidate.getShotId().equals(shot.getShotId())) {
                break;
            }
            episodeOffsetMs += (candidate.getShotDurationSec() == null ? 3 : candidate.getShotDurationSec()) * 1000;
        }
        String subtitlePrefix = "asr-" + dubbing.getDubbingId() + "-";
        ShortSubtitleExample delete = new ShortSubtitleExample();
        delete.createCriteria().andSubtitleIdLike(subtitlePrefix + "%").andDeletedFlagEqualTo(0);
        ShortSubtitle deleted = new ShortSubtitle();
        deleted.setDeletedFlag(1);
        deleted.setUpdatedTime(new Date());
        shortSubtitleMapper.updateByExampleSelective(deleted, delete);

        List<Map<String, Object>> sentences = maps(output == null ? null : output.get("sentences"));
        if (sentences.isEmpty()) {
            String text = ShortJson.text(output == null ? null : output.get("text"));
            if (!text.isBlank()) {
                int end = (dubbing.getAudioDurationSec() == null ? 1 : dubbing.getAudioDurationSec()) * 1000;
                sentences = List.of(Map.of("begin_ms", 0, "end_ms", end, "text", text));
            }
        }
        int sequence = 1;
        int shotDurationMs = (shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec()) * 1000;
        int dubbingOffsetMs = precedingDubbingDurationMs(dubbing);
        for (Map<String, Object> sentence : sentences) {
            String text = ShortJson.text(sentence.get("text")).trim();
            if (text.isBlank()) {
                continue;
            }
            int relativeBegin = ShortJson.integer(sentence.get("begin_ms"),
                    ShortJson.integer(sentence.get("start_ms"), 0));
            int begin = Math.max(0, dubbingOffsetMs + relativeBegin);
            begin = Math.min(begin, Math.max(0, shotDurationMs - 100));
            int relativeEnd = ShortJson.integer(sentence.get("end_ms"),
                    (dubbing.getAudioDurationSec() == null ? 1 : dubbing.getAudioDurationSec()) * 1000);
            int end = Math.max(begin + 100, dubbingOffsetMs + relativeEnd);
            end = Math.min(Math.max(begin + 100, end), shotDurationMs);
            List<String> chunks = splitSubtitle(text, 15);
            int part = Math.max(100, (end - begin) / Math.max(1, chunks.size()));
            for (int i = 0; i < chunks.size(); i++) {
                ShortSubtitle sub = new ShortSubtitle();
                sub.setSubtitleId(subtitlePrefix + sequence);
                sub.setEpisodeId(dubbing.getEpisodeId());
                sub.setWorkId(dubbing.getWorkId());
                sub.setUserId(dubbing.getUserId());
                int shotNo = shot.getShotNo() == null ? 0 : shot.getShotNo();
                int lineNo = dubbing.getLineNo() == null ? shotNo * 100 : dubbing.getLineNo();
                sub.setSubtitleNo(lineNo * 100 + sequence++);
                sub.setStartMs(episodeOffsetMs + begin + i * part);
                sub.setEndMs(episodeOffsetMs + Math.min(end, begin + (i + 1) * part));
                sub.setSubtitleText(chunks.get(i));
                sub.setShotId(shot.getShotId());
                sub.setDeletedFlag(0);
                sub.setCreatedTime(new Date());
                sub.setUpdatedTime(new Date());
                upsertTranscribedSubtitle(sub);
            }
        }
    }

    private int precedingDubbingDurationMs(ShortDubbing current) {
        ShortDubbingExample ex = new ShortDubbingExample();
        ex.createCriteria().andShotIdEqualTo(current.getShotId()).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("line_no asc");
        int total = 0;
        for (ShortDubbing row : shortDubbingMapper.selectByExample(ex)) {
            if (row.getDubbingId().equals(current.getDubbingId())) {
                break;
            }
            total += (row.getAudioDurationSec() == null ? 0 : row.getAudioDurationSec()) * 1000;
        }
        return total;
    }

    private void upsertTranscribedSubtitle(ShortSubtitle subtitle) {
        ShortSubtitleExample ex = new ShortSubtitleExample();
        ex.createCriteria().andSubtitleIdEqualTo(subtitle.getSubtitleId());
        ShortSubtitle existing = shortSubtitleMapper.selectOneByExample(ex);
        if (existing == null) {
            shortSubtitleMapper.insertSelective(subtitle);
            return;
        }
        subtitle.setId(null);
        subtitle.setCreatedTime(null);
        ShortSubtitleExample up = new ShortSubtitleExample();
        up.createCriteria().andIdEqualTo(existing.getId());
        shortSubtitleMapper.updateByExampleSelective(subtitle, up);
    }

    public void regenerateSubtitles(ShortEpisode ep) {
        softDeleteSubtitles(ep.getEpisodeId());
        ShortDubbingExample dex = new ShortDubbingExample();
        dex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        dex.setOrderByClause("line_no asc");
        List<ShortDubbing> dubbings = shortDubbingMapper.selectByExample(dex);
        Map<String, ShortShot> shots = new LinkedHashMap<>();
        for (ShortShot s : listShots(ep.getEpisodeId())) {
            shots.put(s.getShotId(), s);
        }
        int cursorMs = 0;
        int no = 1;
        for (ShortShot shot : listShots(ep.getEpisodeId())) {
            int shotDurMs = (shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec()) * 1000;
            List<ShortDubbing> lines = dubbings.stream().filter(d -> shot.getShotId().equals(d.getShotId())).toList();
            if (lines.isEmpty()) {
                cursorMs += shotDurMs;
                continue;
            }
            int per = Math.max(500, shotDurMs / Math.max(1, lines.size()));
            int local = 0;
            for (ShortDubbing d : lines) {
                for (String chunk : splitSubtitle(d.getLineText(), 15)) {
                    ShortSubtitle sub = new ShortSubtitle();
                    sub.setSubtitleId(ShortIds.id("sb-"));
                    sub.setEpisodeId(ep.getEpisodeId());
                    sub.setWorkId(ep.getWorkId());
                    sub.setUserId(ep.getUserId());
                    sub.setSubtitleNo(no++);
                    sub.setStartMs(cursorMs + local);
                    sub.setEndMs(cursorMs + local + per);
                    sub.setSubtitleText(chunk);
                    sub.setShotId(shot.getShotId());
                    sub.setDeletedFlag(0);
                    sub.setCreatedTime(new Date());
                    sub.setUpdatedTime(new Date());
                    shortSubtitleMapper.insertSelective(sub);
                    local += per;
                }
            }
            cursorMs += shotDurMs;
        }
    }

    private Map<String, Object> applyAlign(ShortShot shot, ShortDubbing row, String action, boolean freeOnly) {
        int shotDur = shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec();
        int audioDur = row.getAudioDurationSec() == null ? 0 : row.getAudioDurationSec();
        ShortDubbing patch = new ShortDubbing();
        patch.setUpdatedTime(new Date());
        if ("PAD_SILENCE".equals(action)) {
            patch.setAlignStatus("ALIGNED");
            patch.setAutoFixedFlag(1);
            patch.setAlignGapSec(String.valueOf(audioDur - shotDur));
        } else if ("ADJUST_SPEED".equals(action)) {
            if (audioDur > shotDur * 1.25 && freeOnly) {
                throw new BizException(CommonErrorCode.RULE_REJECT, "ADJUST_SPEED exceeds 1.25x; use EXTEND_SHOT");
            }
            double speed = shotDur <= 0 ? 1.0 : Math.min(1.25, (double) audioDur / Math.max(1, shotDur));
            patch.setVoiceSpeed(String.format("%.2f", speed));
            patch.setAlignStatus("ALIGNED");
            patch.setAutoFixedFlag(1);
            patch.setAlignGapSec(String.valueOf(audioDur - shotDur));
        } else if ("EXTEND_SHOT".equals(action)) {
            ShortShot shotPatch = new ShortShot();
            shotPatch.setShotDurationSec(Math.max(shotDur, audioDur));
            shotPatch.setUpdatedTime(new Date());
            ShortShotExample upShot = new ShortShotExample();
            upShot.createCriteria().andIdEqualTo(shot.getId());
            shortShotMapper.updateByExampleSelective(shotPatch, upShot);
            patch.setAlignStatus("ALIGNED");
            patch.setAutoFixedFlag(0);
            patch.setAlignGapSec("0");
        } else {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "unknown align_action");
        }
        ShortDubbingExample up = new ShortDubbingExample();
        up.createCriteria().andIdEqualTo(row.getId());
        shortDubbingMapper.updateByExampleSelective(patch, up);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("shot_id", shot.getShotId());
        item.put("dubbing_id", row.getDubbingId());
        item.put("align_action", action);
        item.put("align_status", "ALIGNED");
        return item;
    }

    public void refreshEpisodeStatus(String episodeId) {
        ShortEpisodeExample eex = new ShortEpisodeExample();
        eex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        ShortEpisode ep = shortEpisodeMapper.selectOneByExample(eex);
        if (ep == null) {
            return;
        }
        ShortDubbingExample ex = new ShortDubbingExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        List<ShortDubbing> rows = shortDubbingMapper.selectByExample(ex);
        if (rows.isEmpty()) {
            patchEpisodeDubbing(ep, "NONE");
            return;
        }
        boolean pending = rows.stream().anyMatch(r -> "PENDING".equals(r.getAlignStatus())
                || r.getAudioAssetId() == null || r.getAudioAssetId().isBlank());
        if (pending) {
            patchEpisodeDubbing(ep, "GENERATING");
            return;
        }
        boolean mis = rows.stream().anyMatch(r -> "MISALIGNED".equals(r.getAlignStatus()));
        if (mis) {
            patchEpisodeDubbing(ep, "DRAFT");
            return;
        }
        ShortSubtitleExample sx = new ShortSubtitleExample();
        sx.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        if (shortSubtitleMapper.selectByExample(sx).isEmpty()) {
            regenerateSubtitles(ep);
        }
        patchEpisodeDubbing(ep, "READY");
    }

    private void patchEpisodeDubbing(ShortEpisode ep, String status) {
        ShortEpisode patch = new ShortEpisode();
        patch.setDubbingStatus(status);
        patch.setUpdatedTime(new Date());
        ShortEpisodeExample up = new ShortEpisodeExample();
        up.createCriteria().andIdEqualTo(ep.getId());
        shortEpisodeMapper.updateByExampleSelective(patch, up);
    }

    private void softDeleteDubbing(String shotId) {
        ShortDubbingExample ex = new ShortDubbingExample();
        ex.createCriteria().andShotIdEqualTo(shotId).andDeletedFlagEqualTo(0);
        ShortDubbing patch = new ShortDubbing();
        patch.setDeletedFlag(1);
        patch.setUpdatedTime(new Date());
        shortDubbingMapper.updateByExampleSelective(patch, ex);
    }

    private void softDeleteSubtitles(String episodeId) {
        ShortSubtitleExample ex = new ShortSubtitleExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        ShortSubtitle patch = new ShortSubtitle();
        patch.setDeletedFlag(1);
        patch.setUpdatedTime(new Date());
        shortSubtitleMapper.updateByExampleSelective(patch, ex);
    }

    private boolean hasManualSubtitleEdits(String episodeId) {
        ShortSubtitleExample ex = new ShortSubtitleExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        for (ShortSubtitle row : shortSubtitleMapper.selectByExample(ex)) {
            if (row.getCreatedTime() != null && row.getUpdatedTime() != null
                    && row.getUpdatedTime().getTime() - row.getCreatedTime().getTime() > 1000L) {
                return true;
            }
        }
        return false;
    }

    private static List<Map<String, Object>> maps(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!(value instanceof List<?> list)) {
            return result;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> raw) {
                Map<String, Object> mapped = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : raw.entrySet()) {
                    if (entry.getKey() != null) {
                        mapped.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                result.add(mapped);
            }
        }
        return result;
    }

    private List<ShortShot> listShots(String episodeId) {
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId).andDeletedFlagEqualTo(0);
        ex.setOrderByClause("shot_no asc");
        return shortShotMapper.selectByExample(ex);
    }

    private ShortShot requireShot(String shotId) {
        String userId = workAccessService.requireUser();
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(shotId).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(ex);
        if (shot == null || !userId.equals(shot.getUserId())) {
            throw new ForbiddenException("shot not found");
        }
        return shot;
    }

    private Map<String, ShortCharacter> charactersOf(String workId) {
        ShortCharacterExample ex = new ShortCharacterExample();
        ex.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        Map<String, ShortCharacter> map = new LinkedHashMap<>();
        for (ShortCharacter c : shortCharacterMapper.selectByExample(ex)) {
            map.put(c.getCharacterId(), c);
        }
        return map;
    }

    private ShortCharacter resolveCharacter(ShortShot shot, Map<String, ShortCharacter> chars) {
        List<Map<String, Object>> refs = ShortJson.list(shot.getCharacterRefJson());
        if (!refs.isEmpty()) {
            String id = ShortJson.text(refs.get(0).get("character_id"));
            if (!id.isBlank() && chars.containsKey(id)) {
                return chars.get(id);
            }
        }
        return chars.values().stream().findFirst().orElse(null);
    }

    private static List<String> splitSubtitle(String text, int max) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        String t = text.trim();
        while (t.length() > max) {
            out.add(t.substring(0, max));
            t = t.substring(max);
        }
        if (!t.isEmpty()) {
            out.add(t);
        }
        return out;
    }

    private static String suggestAction(int shotDur, int audioDur, String alignStatus) {
        if ("ALIGNED".equals(alignStatus) || "PENDING".equals(alignStatus)) {
            return null;
        }
        double gap = audioDur - shotDur;
        if (gap <= 0) {
            return "PAD_SILENCE";
        }
        if (audioDur <= shotDur * 1.25) {
            return "ADJUST_SPEED";
        }
        return "EXTEND_SHOT";
    }

    private static String alignName(String status) {
        if ("ALIGNED".equals(status)) {
            return "已对齐";
        }
        if ("MISALIGNED".equals(status)) {
            return "未对齐";
        }
        return "生成中";
    }

    private static double parseGap(String gap) {
        if (gap == null || gap.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(gap);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static Map<String, Object> voice(String id, String name, String gender, String age, String style) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("voice_id", id);
        m.put("voice_name", name);
        m.put("gender", gender);
        m.put("age_range", age);
        m.put("style", style);
        m.put("preview_url", null);
        return m;
    }
}
