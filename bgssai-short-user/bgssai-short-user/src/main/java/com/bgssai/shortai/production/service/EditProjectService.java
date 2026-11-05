package com.bgssai.shortai.production.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortAssetMapper;
import com.bgssai.shortai.mapper.ShortCharacterMapper;
import com.bgssai.shortai.mapper.ShortDubbingMapper;
import com.bgssai.shortai.mapper.ShortEditExportMapper;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortSubtitleMapper;
import com.bgssai.shortai.model.ShortAsset;
import com.bgssai.shortai.model.ShortAssetExample;
import com.bgssai.shortai.model.ShortCharacter;
import com.bgssai.shortai.model.ShortCharacterExample;
import com.bgssai.shortai.model.ShortDubbing;
import com.bgssai.shortai.model.ShortDubbingExample;
import com.bgssai.shortai.model.ShortEditExport;
import com.bgssai.shortai.model.ShortEditExportExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortSubtitle;
import com.bgssai.shortai.model.ShortSubtitleExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.EditProjectDtos.EditProjectExportReq;
import com.bgssai.shortai.production.support.EdlWriter;
import com.bgssai.shortai.production.support.EditBaselineCards;
import com.bgssai.shortai.production.support.EditTimeline;
import com.bgssai.shortai.production.support.FcpxmlWriter;
import com.bgssai.shortai.production.support.JianyingDraftWriter;
import com.bgssai.shortai.production.support.ShortIds;
import com.bgssai.shortai.production.support.ShortJson;
import com.bgssai.shortai.storage.ObjectStorage;
import com.github.pagehelper.PageHelper;

/**
 * 剪辑工程导出：把一集已生成的镜头视频、配音、字幕、BGM 按同一份时间轴，
 * 封装成剪映草稿 / FCPXML / EDL 三种工程格式的 ZIP 包。
 *
 * <p>与成片合成刻意不共用前置条件：合成服务「可以发布」，所以要求全镜定稿、配音对齐、字幕就绪、
 * 合规无阻断；导出服务「拿出去改」，只要求至少一镜有可用视频。照抄合成的前置会让这个功能
 * 只在创作者已经不需要改的时刻才可用，等于废掉。缺片的镜头列名返回，时间轴上直接跳过，
 * 不插黑场占位——在剪辑软件里看到一段黑，创作者会以为是转场问题，还得回平台查一遍。
 *
 * <p>本服务不调模型、不写用量、不扣配额，因此不进 short_ai_task（那套体系强绑定模型与计费），
 * 独立走 short_edit_export 记录状态，异步线程执行。
 */
@Service
public class EditProjectService {

    private static final Logger log = LoggerFactory.getLogger(EditProjectService.class);

    private static final long MEDIA_LIMIT_BYTE = 2L * 1024 * 1024 * 1024;

    private static final int RETAIN_PER_FORMAT = 10;

    private static final int PRESIGN_SECONDS = 1800;

    private static final long US_PER_SEC = 1000000L;

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private AssetService assetService;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortCharacterMapper shortCharacterMapper;
    @Autowired
    private ShortAssetMapper shortAssetMapper;
    @Autowired
    private ShortDubbingMapper shortDubbingMapper;
    @Autowired
    private ShortSubtitleMapper shortSubtitleMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;
    @Autowired
    private ShortEditExportMapper shortEditExportMapper;
    @Autowired
    private ObjectStorage objectStorage;
    @Autowired
    private Environment environment;
    @Autowired
    @Qualifier("editExportExecutor")
    private TaskExecutor editExportExecutor;

    public ApiResponse precheck(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        ShortShotExample sx = new ShortShotExample();
        sx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        sx.setOrderByClause("shot_no asc");
        List<ShortShot> shots = shortShotMapper.selectByExample(sx);
        List<Map<String, Object>> missing = new ArrayList<>();
        int ready = 0;
        long mediaByte = 0L;
        for (ShortShot shot : shots) {
            ShortAsset video = assetOf(shot.getVideoAssetId());
            if (video == null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("shot_no", shot.getShotNo());
                item.put("shot_desc", shot.getShotDesc());
                missing.add(item);
                continue;
            }
            ready++;
            mediaByte += video.getFileSizeByte() == null ? 0L : video.getFileSizeByte();
            ShortDubbingExample dx = new ShortDubbingExample();
            dx.createCriteria().andShotIdEqualTo(shot.getShotId()).andDeletedFlagEqualTo(0);
            for (ShortDubbing dubbing : shortDubbingMapper.selectByExample(dx)) {
                ShortAsset audio = assetOf(dubbing.getAudioAssetId());
                if (audio != null) {
                    mediaByte += audio.getFileSizeByte() == null ? 0L : audio.getFileSizeByte();
                }
            }
        }
        ShortWork work = workAccessService.requireWork(ep.getWorkId());
        ShortAsset bgm = assetOf(ShortJson.text(ShortJson.map(work.getComposeSettingJson()).get("bgm_asset_id")));
        if (bgm != null) {
            mediaByte += bgm.getFileSizeByte() == null ? 0L : bgm.getFileSizeByte();
        }
        ShortEditExportExample lastEx = new ShortEditExportExample();
        lastEx.createCriteria().andUserIdEqualTo(ep.getUserId()).andDeletedFlagEqualTo(0);
        lastEx.setOrderByClause("created_time desc, id desc");
        ShortEditExport last = shortEditExportMapper.selectOneByExample(lastEx);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("episode_id", ep.getEpisodeId());
        result.put("exportable_flag", ready > 0 ? 1 : 0);
        result.put("ready_shot_count", ready);
        result.put("total_shot_count", shots.size());
        result.put("missing_shot_list", missing);
        result.put("estimated_media_byte", mediaByte);
        result.put("media_limit_byte", MEDIA_LIMIT_BYTE);
        result.put("last_draft_dir", last == null ? null : last.getDraftDir());
        List<ShortCharacter> characters = loadCharacters(ep.getWorkId());
        result.put("character_version_list", EditBaselineCards.lockedCharacterCards(characters));
        result.put("voided_character_list", EditBaselineCards.voidedCharacterCards(characters, shots));
        result.put("timeline_version", EditBaselineCards.timelineCard(shots, characters));
        return Responses.ok(result);
    }

    private List<ShortCharacter> loadCharacters(String workId) {
        if (shortCharacterMapper == null || workId == null || workId.isBlank()) {
            return List.of();
        }
        ShortCharacterExample ex = new ShortCharacterExample();
        ex.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        List<ShortCharacter> rows = shortCharacterMapper.selectByExample(ex);
        return rows == null ? List.of() : rows;
    }

    public ApiResponse export(EditProjectExportReq req) {
        if (req == null || req.getEpisodeId() == null || req.getEpisodeId().isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "episode_id required");
        }
        String format = req.getProjectFormat() == null ? "" : req.getProjectFormat().trim().toUpperCase();
        if (!"JIANYING".equals(format) && !"FCPXML".equals(format) && !"EDL".equals(format)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM,
                    "project_format must be JIANYING, FCPXML or EDL");
        }
        String jianyingVersion = req.getJianyingVersion() == null || req.getJianyingVersion().isBlank()
                ? "V6" : req.getJianyingVersion().trim().toUpperCase();
        if ("JIANYING".equals(format) && !"V5".equals(jianyingVersion) && !"V6".equals(jianyingVersion)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "jianying_version must be V5 or V6");
        }
        String mediaMode = req.getMediaMode() == null || req.getMediaMode().isBlank()
                ? "WITH_MEDIA" : req.getMediaMode().trim().toUpperCase();
        if (!"WITH_MEDIA".equals(mediaMode) && !"LINK_ONLY".equals(mediaMode)) {
            throw new BizException(CommonErrorCode.INVALID_PARAM,
                    "media_mode must be WITH_MEDIA or LINK_ONLY");
        }
        String draftDir = req.getDraftDir() == null ? "" : req.getDraftDir().trim();
        if ("JIANYING".equals(format) && draftDir.isEmpty()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM,
                    "剪映草稿需要填写本机剪映草稿目录（draft_dir）");
        }
        ShortEpisode ep = workAccessService.requireEpisode(req.getEpisodeId());
        ShortWork work = workAccessService.requireWork(ep.getWorkId());

        ShortShotExample sx = new ShortShotExample();
        sx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        sx.setOrderByClause("shot_no asc");
        List<ShortShot> shots = shortShotMapper.selectByExample(sx);
        int ready = 0;
        long mediaByte = 0L;
        for (ShortShot shot : shots) {
            ShortAsset video = assetOf(shot.getVideoAssetId());
            if (video == null) {
                continue;
            }
            ready++;
            mediaByte += video.getFileSizeByte() == null ? 0L : video.getFileSizeByte();
            ShortDubbingExample dx = new ShortDubbingExample();
            dx.createCriteria().andShotIdEqualTo(shot.getShotId()).andDeletedFlagEqualTo(0);
            for (ShortDubbing dubbing : shortDubbingMapper.selectByExample(dx)) {
                ShortAsset audio = assetOf(dubbing.getAudioAssetId());
                if (audio != null) {
                    mediaByte += audio.getFileSizeByte() == null ? 0L : audio.getFileSizeByte();
                }
            }
        }
        if (ready == 0) {
            throw new BizException(CommonErrorCode.RULE_REJECT, "本集尚无已生成的镜头视频");
        }
        if ("WITH_MEDIA".equals(mediaMode) && mediaByte > MEDIA_LIMIT_BYTE) {
            throw new BizException(CommonErrorCode.MEDIA_TOO_LARGE,
                    "素材总量超过 2 GB，请改用仅导出工程文件（LINK_ONLY）");
        }

        ShortEditExport row = new ShortEditExport();
        row.setExportId(ShortIds.id("x-"));
        row.setEpisodeId(ep.getEpisodeId());
        row.setWorkId(ep.getWorkId());
        row.setUserId(ep.getUserId());
        row.setProjectFormat(format);
        row.setJianyingVersion("JIANYING".equals(format) ? jianyingVersion : null);
        row.setMediaMode(mediaMode);
        row.setDraftDir(draftDir.isEmpty() ? null : draftDir);
        row.setExportStatus("QUEUED");
        row.setClipCount(ready);
        row.setPackageName(packageName(work, ep, format));
        row.setDeletedFlag(0);
        row.setCreatedTime(new Date());
        shortEditExportMapper.insertSelective(row);

        String exportId = row.getExportId();
        editExportExecutor.execute(() -> runExport(exportId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("export_id", exportId);
        result.put("export_status", "QUEUED");
        return Responses.ok(result);
    }

    /**
     * 打包本身跑在独立线程池上：一集素材几百 MB，从对象存储逐个拉回来再压缩，同步做会把
     * HTTP 线程占住几十秒。用户拿到 export_id 后轮询 detail 即可。
     * <p>
     * 刻意用注入的线程池直接提交，而不是给本方法挂 {@code @Async}：同类内部调用不走 Spring 代理，
     * 注解会静默失效、打包退化成同步执行，这类错误在功能上看不出来，只有等大集导出把请求线程占死才暴露。
     */
    public void runExport(String exportId) {
        ShortEditExportExample ex = new ShortEditExportExample();
        ex.createCriteria().andExportIdEqualTo(exportId).andDeletedFlagEqualTo(0);
        ShortEditExport row = shortEditExportMapper.selectOneByExample(ex);
        if (row == null) {
            return;
        }
        ShortEditExport running = new ShortEditExport();
        running.setExportStatus("RUNNING");
        running.setUpdatedTime(new Date());
        ShortEditExportExample up = new ShortEditExportExample();
        up.createCriteria().andIdEqualTo(row.getId()).andExportStatusEqualTo("QUEUED");
        if (shortEditExportMapper.updateByExampleSelective(running, up) != 1) {
            return;
        }
        try {
            ShortEpisodeExample epx = new ShortEpisodeExample();
            epx.createCriteria().andEpisodeIdEqualTo(row.getEpisodeId()).andDeletedFlagEqualTo(0);
            ShortEpisode ep = shortEpisodeMapper.selectOneByExample(epx);
            if (ep == null) {
                throw new BizException(CommonErrorCode.RULE_REJECT, "episode not found");
            }
            EditTimeline timeline = buildTimeline(ep, row);
            byte[] zip = pack(timeline, row);
            String env = environment.getActiveProfiles().length > 0
                    ? environment.getActiveProfiles()[0] : "local";
            ShortAsset asset = new ShortAsset();
            asset.setAssetId(ShortIds.id("a-"));
            asset.setUserId(row.getUserId());
            asset.setWorkId(row.getWorkId());
            asset.setEpisodeId(row.getEpisodeId());
            asset.setAssetType("EDIT_PROJECT");
            asset.setMimeType("application/zip");
            String objectKey = "short/" + env + "/" + row.getUserId() + "/" + row.getWorkId()
                    + "/" + asset.getAssetId() + ".zip";
            objectStorage.put(objectKey, zip, "application/zip");
            asset.setStorageKey(objectKey);
            asset.setFileUrl("/bgssai/user/assets/" + asset.getAssetId());
            asset.setFileSizeByte(zip.length);
            asset.setSelectedFlag(0);
            asset.setDraftFlag(0);
            asset.setDeletedFlag(0);
            asset.setCreatedTime(new Date());
            shortAssetMapper.insertSelective(asset);

            List<Map<String, Object>> missing = new ArrayList<>();
            for (EditTimeline.Missing item : timeline.getMissingList()) {
                Map<String, Object> one = new LinkedHashMap<>();
                one.put("shot_no", item.getShotNo());
                one.put("shot_desc", item.getShotDesc());
                missing.add(one);
            }
            ShortEditExport done = new ShortEditExport();
            done.setExportStatus("READY");
            done.setAssetId(asset.getAssetId());
            done.setClipCount(timeline.getClipList().size());
            done.setPackageByte((long) zip.length);
            done.setMissingJson(ShortJson.toJson(missing));
            done.setFinishedTime(new Date());
            done.setUpdatedTime(new Date());
            ShortEditExportExample doneEx = new ShortEditExportExample();
            doneEx.createCriteria().andIdEqualTo(row.getId());
            shortEditExportMapper.updateByExampleSelective(done, doneEx);
            retain(row);
        } catch (Exception exAll) {
            log.warn("edit project export failed exportId={}", exportId, exAll);
            ShortEditExport failed = new ShortEditExport();
            failed.setExportStatus("FAILED");
            String reason = exAll.getMessage() == null ? "导出失败" : exAll.getMessage();
            failed.setFailReason(reason.length() > 900 ? reason.substring(0, 900) : reason);
            failed.setFinishedTime(new Date());
            failed.setUpdatedTime(new Date());
            ShortEditExportExample failEx = new ShortEditExportExample();
            failEx.createCriteria().andIdEqualTo(row.getId());
            shortEditExportMapper.updateByExampleSelective(failed, failEx);
        }
    }

    public ApiResponse detail(String exportId) {
        String userId = workAccessService.requireUser();
        if (exportId == null || exportId.isBlank()) {
            throw new BizException(CommonErrorCode.INVALID_PARAM, "export_id required");
        }
        ShortEditExportExample ex = new ShortEditExportExample();
        ex.createCriteria().andExportIdEqualTo(exportId.trim()).andUserIdEqualTo(userId)
                .andDeletedFlagEqualTo(0);
        ShortEditExport row = shortEditExportMapper.selectOneByExample(ex);
        if (row == null) {
            throw new BizException(CommonErrorCode.RESOURCE_NOT_FOUND, "export not found");
        }
        Map<String, Object> result = view(row);
        if ("READY".equals(row.getExportStatus()) && row.getAssetId() != null) {
            ShortAsset asset = assetOf(row.getAssetId());
            if (asset != null && asset.getStorageKey() != null && !asset.getStorageKey().isBlank()) {
                ObjectStorage.PresignResult signed =
                        objectStorage.presignGet(asset.getStorageKey(), PRESIGN_SECONDS);
                result.put("download_url", signed.downloadUrl());
                result.put("expire_at", DateTimeFormatter.ISO_INSTANT.format(signed.expireAt()));
            }
        }
        return Responses.ok(result);
    }

    public PageResponse list(String episodeId, Integer pageNum, Integer pageSize) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 || pageSize > 100 ? 20 : pageSize;
        ShortEditExportExample ex = new ShortEditExportExample();
        ex.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andUserIdEqualTo(ep.getUserId())
                .andDeletedFlagEqualTo(0);
        ex.setOrderByClause("created_time desc, id desc");
        PageHelper.startPage(safePageNum, safePageSize);
        List<ShortEditExport> rows = shortEditExportMapper.selectByExample(ex);
        long total = ((com.github.pagehelper.Page<ShortEditExport>) rows).getTotal();
        List<Map<String, Object>> items = new ArrayList<>();
        for (ShortEditExport row : rows) {
            items.add(view(row));
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

    /**
     * 组装时间轴。口径与 FilmService.timeline 一致：镜头按 shot_no 升序、起点为前序镜头时长累加，
     * 配音落在所属镜头区间内，BGM 从 0 铺到全集时长。缺片的镜头只登记不占时间轴，后续镜头前移。
     */
    private EditTimeline buildTimeline(ShortEpisode ep, ShortEditExport row) {
        ShortWork work = null;
        try {
            work = workAccessService.requireWork(ep.getWorkId());
        } catch (RuntimeException exAll) {
            log.warn("work not accessible in async export, fall back to episode fields workId={}",
                    ep.getWorkId());
        }
        EditTimeline timeline = new EditTimeline();
        timeline.setProjectTitle(work == null ? "BGSSAI" : safeName(work.getWorkTitle()));
        timeline.setEpisodeNo(ep.getEpisodeNo() == null ? 1 : ep.getEpisodeNo());
        timeline.setWidth(1080);
        timeline.setHeight(1920);
        timeline.setFps(30);

        ShortShotExample sx = new ShortShotExample();
        sx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        sx.setOrderByClause("shot_no asc");
        List<ShortShot> shots = shortShotMapper.selectByExample(sx);
        long cursorUs = 0L;
        for (ShortShot shot : shots) {
            ShortAsset video = assetOf(shot.getVideoAssetId());
            if (video == null) {
                EditTimeline.Missing missing = new EditTimeline.Missing();
                missing.setShotNo(shot.getShotNo() == null ? 0 : shot.getShotNo());
                missing.setShotDesc(shot.getShotDesc());
                timeline.getMissingList().add(missing);
                continue;
            }
            int durationSec = video.getDurationSec() != null && video.getDurationSec() > 0
                    ? video.getDurationSec()
                    : (shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec());
            long durationUs = durationSec * US_PER_SEC;
            EditTimeline.Clip clip = new EditTimeline.Clip();
            clip.setShotNo(shot.getShotNo() == null ? 0 : shot.getShotNo());
            clip.setShotDesc(shot.getShotDesc());
            clip.setFileName("shot_" + clip.getShotNo() + extOf(video.getMimeType(), "mp4"));
            clip.setStartUs(cursorUs);
            clip.setDurationUs(durationUs);
            clip.setWidth(video.getWidth() == null ? 1080 : video.getWidth());
            clip.setHeight(video.getHeight() == null ? 1920 : video.getHeight());
            timeline.getClipList().add(clip);

            ShortDubbingExample dx = new ShortDubbingExample();
            dx.createCriteria().andShotIdEqualTo(shot.getShotId()).andDeletedFlagEqualTo(0);
            dx.setOrderByClause("line_no asc");
            long voiceCursorUs = cursorUs;
            for (ShortDubbing dubbing : shortDubbingMapper.selectByExample(dx)) {
                ShortAsset audio = assetOf(dubbing.getAudioAssetId());
                if (audio == null) {
                    continue;
                }
                int voiceSec = dubbing.getAudioDurationSec() == null || dubbing.getAudioDurationSec() <= 0
                        ? durationSec : dubbing.getAudioDurationSec();
                EditTimeline.Voice voice = new EditTimeline.Voice();
                voice.setShotNo(clip.getShotNo());
                voice.setFileName("voice_" + clip.getShotNo() + "_" + dubbing.getLineNo()
                        + extOf(audio.getMimeType(), "mp3"));
                voice.setStartUs(voiceCursorUs);
                voice.setDurationUs(voiceSec * US_PER_SEC);
                timeline.getVoiceList().add(voice);
                voiceCursorUs += voiceSec * US_PER_SEC;
            }
            cursorUs += durationUs;
        }
        timeline.setTotalUs(cursorUs);

        ShortSubtitleExample subEx = new ShortSubtitleExample();
        subEx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        subEx.setOrderByClause("subtitle_no asc");
        List<ShortSubtitle> subtitles = shortSubtitleMapper.selectByExample(subEx);
        if (!subtitles.isEmpty()) {
            for (ShortSubtitle subtitle : subtitles) {
                long startUs = subtitle.getStartMs() * 1000L;
                long endUs = subtitle.getEndMs() * 1000L;
                if (endUs <= startUs) {
                    continue;
                }
                EditTimeline.Line line = new EditTimeline.Line();
                line.setLineText(subtitle.getSubtitleText());
                line.setStartUs(startUs);
                line.setDurationUs(endUs - startUs);
                timeline.getLineList().add(line);
            }
        } else {
            // 字幕未生成时不空着字幕轨：配音台词本身就是台词文本，按配音区间铺上去，
            // 创作者进剪辑软件后直接改字，比让他从头敲一遍强。
            for (EditTimeline.Voice voice : timeline.getVoiceList()) {
                ShortShotExample lookup = new ShortShotExample();
                lookup.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId())
                        .andShotNoEqualTo(voice.getShotNo()).andDeletedFlagEqualTo(0);
                ShortShot shot = shortShotMapper.selectOneByExample(lookup);
                if (shot == null || shot.getDialogueText() == null || shot.getDialogueText().isBlank()) {
                    continue;
                }
                EditTimeline.Line line = new EditTimeline.Line();
                line.setLineText(shot.getDialogueText());
                line.setStartUs(voice.getStartUs());
                line.setDurationUs(voice.getDurationUs());
                timeline.getLineList().add(line);
            }
        }

        if (work != null) {
            String bgmAssetId = ShortJson.text(ShortJson.map(work.getComposeSettingJson()).get("bgm_asset_id"));
            ShortAsset bgm = assetOf(bgmAssetId);
            if (bgm != null) {
                timeline.setBgmFileName("bgm" + extOf(bgm.getMimeType(), "mp3"));
                timeline.setBgmDurationUs(bgm.getDurationSec() == null ? timeline.getTotalUs()
                        : bgm.getDurationSec() * US_PER_SEC);
            }
        }
        log.info("edit project timeline built exportId={} clips={} voices={} lines={} missing={}",
                row.getExportId(), timeline.getClipList().size(), timeline.getVoiceList().size(),
                timeline.getLineList().size(), timeline.getMissingList().size());
        return timeline;
    }

    /** 把工程文件与（可选的）素材压成一个 ZIP。 */
    private byte[] pack(EditTimeline timeline, ShortEditExport row) throws Exception {
        String format = row.getProjectFormat();
        String draftName = timeline.getProjectTitle() + "_第" + timeline.getEpisodeNo() + "集";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8);
        String root = "JIANYING".equals(format) ? draftName
                : timeline.getProjectTitle() + "_E" + timeline.getEpisodeNo();

        if ("JIANYING".equals(format)) {
            String draftDir = row.getDraftDir() == null ? "" : row.getDraftDir().trim();
            String normalizedDir = draftDir.replace('\\', '/');
            while (normalizedDir.endsWith("/")) {
                normalizedDir = normalizedDir.substring(0, normalizedDir.length() - 1);
            }
            String foldPath = normalizedDir + "/" + draftName;
            String assetsAbsPath = foldPath + "/assets";
            String mainFile = "V5".equals(row.getJianyingVersion()) ? "draft_content.json" : "draft_info.json";
            writeEntry(zip, root + "/" + mainFile,
                    ShortJson.toJson(JianyingDraftWriter.draftContent(timeline, assetsAbsPath))
                            .getBytes(StandardCharsets.UTF_8));
            writeEntry(zip, root + "/draft_meta_info.json",
                    ShortJson.toJson(JianyingDraftWriter.draftMetaInfo(timeline, draftName,
                            normalizedDir, foldPath)).getBytes(StandardCharsets.UTF_8));
        } else if ("FCPXML".equals(format)) {
            writeEntry(zip, root + "/" + root + ".fcpxml",
                    FcpxmlWriter.write(timeline).getBytes(StandardCharsets.UTF_8));
        } else {
            writeEntry(zip, root + "/" + root + ".edl",
                    EdlWriter.writeEdl(timeline).getBytes(StandardCharsets.UTF_8));
            writeEntry(zip, root + "/subtitle.srt",
                    EdlWriter.writeSrt(timeline).getBytes(StandardCharsets.UTF_8));
        }

        List<String[]> manifest = new ArrayList<>();
        ShortShotExample sx = new ShortShotExample();
        sx.createCriteria().andEpisodeIdEqualTo(row.getEpisodeId()).andDeletedFlagEqualTo(0);
        sx.setOrderByClause("shot_no asc");
        Map<Integer, ShortShot> shotByNo = new LinkedHashMap<>();
        for (ShortShot shot : shortShotMapper.selectByExample(sx)) {
            shotByNo.put(shot.getShotNo() == null ? 0 : shot.getShotNo(), shot);
        }
        boolean withMedia = "WITH_MEDIA".equals(row.getMediaMode());
        for (EditTimeline.Clip clip : timeline.getClipList()) {
            ShortShot shot = shotByNo.get(clip.getShotNo());
            ShortAsset video = shot == null ? null : assetOf(shot.getVideoAssetId());
            if (video == null) {
                continue;
            }
            if (withMedia) {
                writeEntry(zip, root + "/assets/" + clip.getFileName(),
                        objectStorage.get(video.getStorageKey()));
            }
            manifest.add(new String[] { clip.getFileName(), video.getAssetId(), "VIDEO",
                    String.valueOf(clip.getShotNo()), String.valueOf(clip.getDurationUs() / US_PER_SEC),
                    withMedia ? "" : assetService.resolveUrl(video.getAssetId()) });
        }
        for (EditTimeline.Voice voice : timeline.getVoiceList()) {
            ShortShot shot = shotByNo.get(voice.getShotNo());
            if (shot == null) {
                continue;
            }
            ShortDubbingExample dx = new ShortDubbingExample();
            dx.createCriteria().andShotIdEqualTo(shot.getShotId()).andDeletedFlagEqualTo(0);
            dx.setOrderByClause("line_no asc");
            for (ShortDubbing dubbing : shortDubbingMapper.selectByExample(dx)) {
                ShortAsset audio = assetOf(dubbing.getAudioAssetId());
                if (audio == null) {
                    continue;
                }
                String fileName = "voice_" + voice.getShotNo() + "_" + dubbing.getLineNo()
                        + extOf(audio.getMimeType(), "mp3");
                if (!fileName.equals(voice.getFileName())) {
                    continue;
                }
                if (withMedia) {
                    writeEntry(zip, root + "/assets/" + fileName, objectStorage.get(audio.getStorageKey()));
                }
                manifest.add(new String[] { fileName, audio.getAssetId(), "AUDIO",
                        String.valueOf(voice.getShotNo()),
                        String.valueOf(voice.getDurationUs() / US_PER_SEC),
                        withMedia ? "" : assetService.resolveUrl(audio.getAssetId()) });
            }
        }
        if (timeline.getBgmFileName() != null) {
            ShortWork work = null;
            try {
                work = workAccessService.requireWork(row.getWorkId());
            } catch (RuntimeException exAll) {
                log.warn("bgm skipped, work not accessible workId={}", row.getWorkId());
            }
            if (work != null) {
                ShortAsset bgm = assetOf(
                        ShortJson.text(ShortJson.map(work.getComposeSettingJson()).get("bgm_asset_id")));
                if (bgm != null) {
                    if (withMedia) {
                        writeEntry(zip, root + "/assets/" + timeline.getBgmFileName(),
                                objectStorage.get(bgm.getStorageKey()));
                    }
                    manifest.add(new String[] { timeline.getBgmFileName(), bgm.getAssetId(), "BGM", "",
                            String.valueOf(timeline.getBgmDurationUs() / US_PER_SEC),
                            withMedia ? "" : assetService.resolveUrl(bgm.getAssetId()) });
                }
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append('﻿');
        csv.append("file_name,asset_id,asset_type,shot_no,duration_sec,download_url\r\n");
        for (String[] cells : manifest) {
            for (int i = 0; i < cells.length; i++) {
                if (i > 0) {
                    csv.append(',');
                }
                String cell = cells[i] == null ? "" : cells[i];
                csv.append('"').append(cell.replace("\"", "\"\"")).append('"');
            }
            csv.append("\r\n");
        }
        writeEntry(zip, root + "/manifest.csv", csv.toString().getBytes(StandardCharsets.UTF_8));
        writeEntry(zip, root + "/README.txt", readme(timeline, row, draftName)
                .getBytes(StandardCharsets.UTF_8));
        zip.finish();
        zip.close();
        return buffer.toByteArray();
    }

    private String readme(EditTimeline timeline, ShortEditExport row, String draftName) {
        StringBuilder text = new StringBuilder();
        text.append("BGSSAI AI 短剧制作平台 - 剪辑工程导出\r\n\r\n");
        text.append("作品：").append(timeline.getProjectTitle()).append("\r\n");
        text.append("剧集：第 ").append(timeline.getEpisodeNo()).append(" 集\r\n");
        text.append("格式：").append(row.getProjectFormat());
        if ("JIANYING".equals(row.getProjectFormat())) {
            text.append("（").append("V5".equals(row.getJianyingVersion()) ? "剪映 5.x" : "剪映 6.0 及以上")
                    .append("）");
        }
        text.append("\r\n");
        text.append("素材：").append("WITH_MEDIA".equals(row.getMediaMode())
                ? "已随包打进 assets 目录" : "未随包，见 manifest.csv 的下载链接（30 分钟有效）").append("\r\n");
        text.append("镜头：已导出 ").append(timeline.getClipList().size()).append(" 段");
        if (!timeline.getMissingList().isEmpty()) {
            text.append("，以下镜头尚无可用视频、时间轴上已跳过（不插黑场）：");
            for (int i = 0; i < timeline.getMissingList().size(); i++) {
                if (i > 0) {
                    text.append("、");
                }
                text.append("镜 ").append(timeline.getMissingList().get(i).getShotNo());
            }
        }
        text.append("\r\n\r\n");
        if ("JIANYING".equals(row.getProjectFormat())) {
            text.append("导入剪映：\r\n");
            text.append("1. 把本包内的 ").append(draftName).append(" 文件夹整个解压到剪映草稿目录下；\r\n");
            text.append("2. 草稿目录须与导出时填写的一致：").append(row.getDraftDir()).append("\r\n");
            text.append("3. 重启剪映，草稿列表中会出现该集，双击打开即可。\r\n");
            text.append("路径不一致时草稿仍能打开，但素材会显示离线。\r\n");
            text.append("本包面向中国大陆版剪映桌面端，与 CapCut 的兼容性尚未验证。\r\n");
        } else if ("FCPXML".equals(row.getProjectFormat())) {
            text.append("导入达芬奇 / Final Cut：\r\n");
            text.append("1. 先把整包解压到本地任意目录，保持 assets 与 fcpxml 的相对位置不变；\r\n");
            text.append("2. 达芬奇：文件 - 导入 - 时间线 - 导入 AAF / EDL / XML，选择包内的 .fcpxml；\r\n");
            text.append("3. Final Cut：文件 - 导入 - XML。\r\n");
        } else {
            text.append("导入 Premiere / 达芬奇：\r\n");
            text.append("1. 先把整包解压到本地任意目录；\r\n");
            text.append("2. 导入包内的 .edl，按提示指定 assets 目录为素材路径；\r\n");
            text.append("3. EDL 不携带字幕轨，字幕见包内 subtitle.srt，单独导入即可。\r\n");
        }
        return text.toString();
    }

    private void writeEntry(ZipOutputStream zip, String name, byte[] content) throws Exception {
        if (content == null) {
            return;
        }
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    /**
     * 每集每格式只留最近 10 条，超出的连同产物素材一起软删——导出包动辄几百 MB，
     * 不设上限的话对象存储会被反复导出的历史包堆满。
     */
    private void retain(ShortEditExport row) {
        ShortEditExportExample ex = new ShortEditExportExample();
        ex.createCriteria().andEpisodeIdEqualTo(row.getEpisodeId())
                .andProjectFormatEqualTo(row.getProjectFormat())
                .andExportStatusEqualTo("READY").andDeletedFlagEqualTo(0);
        ex.setOrderByClause("created_time desc, id desc");
        List<ShortEditExport> rows = shortEditExportMapper.selectByExample(ex);
        for (int i = RETAIN_PER_FORMAT; i < rows.size(); i++) {
            ShortEditExport old = rows.get(i);
            ShortEditExport patch = new ShortEditExport();
            patch.setDeletedFlag(1);
            patch.setUpdatedTime(new Date());
            ShortEditExportExample up = new ShortEditExportExample();
            up.createCriteria().andIdEqualTo(old.getId());
            shortEditExportMapper.updateByExampleSelective(patch, up);
            if (old.getAssetId() == null) {
                continue;
            }
            ShortAsset asset = assetOf(old.getAssetId());
            if (asset == null) {
                continue;
            }
            ShortAsset assetPatch = new ShortAsset();
            assetPatch.setId(asset.getId());
            assetPatch.setDeletedFlag(1);
            assetPatch.setUpdatedTime(new Date());
            shortAssetMapper.updateByPrimaryKeySelective(assetPatch);
        }
    }

    private Map<String, Object> view(ShortEditExport row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("export_id", row.getExportId());
        item.put("episode_id", row.getEpisodeId());
        item.put("work_id", row.getWorkId());
        item.put("project_format", row.getProjectFormat());
        item.put("jianying_version", row.getJianyingVersion());
        item.put("media_mode", row.getMediaMode());
        item.put("draft_dir", row.getDraftDir());
        item.put("export_status", row.getExportStatus());
        item.put("clip_count", row.getClipCount());
        item.put("package_byte", row.getPackageByte());
        item.put("asset_id", row.getAssetId());
        item.put("package_name", row.getPackageName());
        item.put("missing_shot_list", ShortJson.list(row.getMissingJson()));
        String failReason = row.getFailReason();
        item.put("fail_reason", failReason == null || failReason.isBlank()
                ? failReason
                : com.bgssai.shortai.aiprovider.TokenHubClientSupport.userFacingMessage(failReason));
        item.put("created_time", row.getCreatedTime());
        item.put("finished_time", row.getFinishedTime());
        return item;
    }

    private ShortAsset assetOf(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return null;
        }
        ShortAssetExample ex = new ShortAssetExample();
        ex.createCriteria().andAssetIdEqualTo(assetId).andDeletedFlagEqualTo(0);
        return shortAssetMapper.selectOneByExample(ex);
    }

    private String packageName(ShortWork work, ShortEpisode ep, String format) {
        return safeName(work.getWorkTitle()) + "_E" + (ep.getEpisodeNo() == null ? 1 : ep.getEpisodeNo())
                + "_" + format + ".zip";
    }

    private String safeName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "BGSSAI";
        }
        String name = raw.replace("/", "_").replace("\\", "_").replace("..", "_")
                .replace(":", "_").replace("*", "_").replace("?", "_").replace("\"", "_")
                .replace("<", "_").replace(">", "_").replace("|", "_").trim();
        return name.isEmpty() ? "BGSSAI" : name;
    }

    private String extOf(String mimeType, String fallback) {
        if (mimeType == null || mimeType.isBlank()) {
            return "." + fallback;
        }
        String mime = mimeType.toLowerCase();
        if (mime.contains("mp4")) {
            return ".mp4";
        }
        if (mime.contains("webm")) {
            return ".webm";
        }
        if (mime.contains("quicktime")) {
            return ".mov";
        }
        if (mime.contains("mpeg") || mime.contains("mp3")) {
            return ".mp3";
        }
        if (mime.contains("wav")) {
            return ".wav";
        }
        if (mime.contains("aac")) {
            return ".aac";
        }
        return "." + fallback;
    }
}
