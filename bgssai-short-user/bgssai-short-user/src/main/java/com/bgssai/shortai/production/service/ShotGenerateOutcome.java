package com.bgssai.shortai.production.service;

import java.util.Date;
import java.util.List;

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
        if (task.getTargetId() == null || task.getTargetId().isBlank()) {
            return;
        }
        if (activeSibling(task)) {
            return;
        }
        ShortShotExample ex = new ShortShotExample();
        ex.createCriteria().andShotIdEqualTo(task.getTargetId()).andDeletedFlagEqualTo(0);
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
        ShortShotExample up = new ShortShotExample();
        up.createCriteria().andIdEqualTo(shot.getId()).andShotStatusEqualTo(shot.getShotStatus());
        shortShotMapper.updateByExampleSelective(patch, up);
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
