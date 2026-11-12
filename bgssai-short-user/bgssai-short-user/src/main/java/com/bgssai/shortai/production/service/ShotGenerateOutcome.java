package com.bgssai.shortai.production.service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.model.ShortShot;
import com.bgssai.shortai.model.ShortShotExample;

/**
 * A failed or canceled shot generate must not hide a clip or still that is already bound.
 * While another job of the same type is still queued or running, the generating status stays.
 */
@Service
public class ShotGenerateOutcome {

    @Autowired
    private ShortShotMapper shortShotMapper;
    @Autowired
    private ShortAiTaskMapper shortAiTaskMapper;

    public void restoreBoundMedia(ShortAiTask task) {
        if (task == null || !"SHOT".equals(task.getTargetType())) {
            return;
        }
        String type = task.getTaskType();
        if (!"VIDEO".equals(type) && !"IMAGE".equals(type)) {
            return;
        }
        if (task.getId() == null || task.getTargetId() == null || task.getTargetId().isBlank()) {
            return;
        }
        ShortAiTask terminal = shortAiTaskMapper.selectByPrimaryKey(task.getId());
        if (terminal == null || !("FAILED".equals(terminal.getTaskStatus()) || "CANCELED".equals(terminal.getTaskStatus()))
                || !Objects.equals(task.getTaskId(), terminal.getTaskId())
                || !Objects.equals(task.getRetryCount(), terminal.getRetryCount())
                || !Objects.equals(task.getUserId(), terminal.getUserId())
                || !Objects.equals(task.getWorkId(), terminal.getWorkId())
                || !Objects.equals(task.getEpisodeId(), terminal.getEpisodeId())
                || !Objects.equals(task.getTargetId(), terminal.getTargetId())
                || !Objects.equals(task.getTargetType(), terminal.getTargetType())
                || !Objects.equals(task.getTaskType(), terminal.getTaskType())) return;
        if (activeSibling(task)) {
            return;
        }
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(task.getTargetId()).andUserIdEqualTo(task.getUserId())
                .andWorkIdEqualTo(task.getWorkId()).andEpisodeIdEqualTo(task.getEpisodeId()).andDeletedFlagEqualTo(0);
        ShortShot shot = shortShotMapper.selectOneByExample(ex);
        if (shot == null) {
            return;
        }
        String next = nextStatus(type, shot);
        if (next == null || next.equals(shot.getShotStatus())) {
            return;
        }
        ShortShot patch = new ShortShot();
        patch.setShotStatus(next);
        patch.setUpdatedTime(new Date());
        shortShotMapper.restoreBoundMediaAfterTerminalTask(shot, terminal, patch);
    }

    /**
     * Only move a generating shot back when the asset that status was advertising is still bound.
     * The patch never carries asset ids, so a failure cannot unbind or overwrite them.
     */
    static String nextStatus(String taskType, ShortShot shot) {
        if (shot == null) {
            return null;
        }
        if ("VIDEO".equals(taskType)) {
            if (!"VIDEO_GENERATING".equals(shot.getShotStatus())) {
                return null;
            }
            if (present(shot.getVideoAssetId())) {
                return "VIDEO_READY";
            }
            // The first frame was already accepted. Do not leave the shot looking like it is still rendering.
            return present(shot.getFirstFrameAssetId()) ? "IMAGE_READY" : null;
        }
        if ("IMAGE".equals(taskType)) {
            if (!"IMAGE_GENERATING".equals(shot.getShotStatus()) || !present(shot.getFirstFrameAssetId())) {
                return null;
            }
            return present(shot.getVideoAssetId()) ? "VIDEO_READY" : "IMAGE_READY";
        }
        return null;
    }

    private boolean activeSibling(ShortAiTask task) {
        ShortAiTaskExample active = new ShortAiTaskExample();
        ShortAiTaskExample.Criteria criteria = active.createCriteria()
                .andUserIdEqualTo(task.getUserId()).andWorkIdEqualTo(task.getWorkId()).andEpisodeIdEqualTo(task.getEpisodeId())
                .andTargetIdEqualTo(task.getTargetId())
                .andTargetTypeEqualTo("SHOT")
                .andTaskTypeEqualTo(task.getTaskType())
                .andTaskStatusIn(List.of("QUEUED", "RUNNING", "PAUSED"));
        if (task.getTaskId() != null && !task.getTaskId().isBlank()) {
            criteria.andTaskIdNotEqualTo(task.getTaskId());
        }
        return shortAiTaskMapper.countByExample(active) > 0;
    }

    private static boolean present(String id) {
        return id != null && !id.isBlank();
    }
}
