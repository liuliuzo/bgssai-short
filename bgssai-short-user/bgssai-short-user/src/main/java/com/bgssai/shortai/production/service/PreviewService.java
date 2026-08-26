package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.mapper.ShortContinuityIssueMapper;
import com.bgssai.shortai.mapper.ShortDubbingMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortShotPrevizMapper;
import com.bgssai.shortai.model.ShortContinuityIssueExample;
import com.bgssai.shortai.model.ShortDubbing;
import com.bgssai.shortai.model.ShortDubbingExample;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortShotPreviz;
import com.bgssai.shortai.model.ShortShotPrevizExample;

@Service
public class PreviewService {

    @Autowired
    private WorkAccessService workAccessService;
    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortShotPrevizMapper shortShotPrevizMapper;
    @Autowired
    private ShortDubbingMapper shortDubbingMapper;
    @Autowired
    private ShortContinuityIssueMapper shortContinuityIssueMapper;

    public ApiResponse summary(String episodeId) {
        ShortEpisode ep = workAccessService.requireEpisode(episodeId);
        ShortShotExample sx = new ShortShotExample();
        sx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        sx.setOrderByClause("shot_no asc");
        List<ShortShot> shots = shortShotMapper.selectByExample(sx);
        ShortShotPrevizExample px = new ShortShotPrevizExample();
        px.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        Map<String, ShortShotPreviz> previz = new LinkedHashMap<>();
        for (ShortShotPreviz p : shortShotPrevizMapper.selectByExample(px)) {
            previz.put(p.getShotId(), p);
        }
        ShortDubbingExample dx = new ShortDubbingExample();
        dx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        int misaligned = 0;
        for (ShortDubbing d : shortDubbingMapper.selectByExample(dx)) {
            if ("MISALIGNED".equals(d.getAlignStatus())) {
                misaligned++;
            }
        }
        ShortContinuityIssueExample cx = new ShortContinuityIssueExample();
        cx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0)
                .andHandleStatusEqualTo("OPEN");
        int openIssues = shortContinuityIssueMapper.selectByExample(cx).size();
        List<Map<String, Object>> frames = new ArrayList<>();
        int totalSec = 0;
        int withFrame = 0;
        for (ShortShot shot : shots) {
            int dur = shot.getShotDurationSec() == null ? 3 : shot.getShotDurationSec();
            totalSec += dur;
            Map<String, Object> frame = new LinkedHashMap<>();
            frame.put("shot_id", shot.getShotId());
            frame.put("shot_no", shot.getShotNo());
            frame.put("duration_sec", dur);
            frame.put("first_frame_asset_id", shot.getFirstFrameAssetId());
            frame.put("previz_level", shot.getPrevizLevel());
            ShortShotPreviz p = previz.get(shot.getShotId());
            frame.put("previz_text", p == null ? shot.getPrevizText() : p.getPrevizText());
            frames.add(frame);
            if (shot.getFirstFrameAssetId() != null && !shot.getFirstFrameAssetId().isBlank()) {
                withFrame++;
            }
        }
        List<String> answers = new ArrayList<>();
        answers.add(withFrame > 0 ? "样片帧已可连看，覆盖 " + withFrame + "/" + shots.size() + " 镜"
                : "尚无足够首帧，建议先做预演或生成");
        answers.add(openIssues == 0 ? "连贯性暂无明显待处理问题" : "连贯性待处理 " + openIssues + " 条");
        answers.add(misaligned == 0 ? "本集配音对齐正常" : "配音未对齐 " + misaligned + " 条，合成前需处理");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("episode_id", ep.getEpisodeId());
        result.put("episode_title", ep.getEpisodeTitle());
        result.put("total_sec", totalSec);
        result.put("shot_count", shots.size());
        result.put("frame_ready_count", withFrame);
        result.put("open_continuity_count", openIssues);
        result.put("misaligned_dubbing_count", misaligned);
        result.put("three_answers", answers);
        result.put("frame_list", frames);
        result.put("compose_status", ep.getComposeStatus());
        result.put("dubbing_status", ep.getDubbingStatus());
        return Responses.ok(result);
    }
}
