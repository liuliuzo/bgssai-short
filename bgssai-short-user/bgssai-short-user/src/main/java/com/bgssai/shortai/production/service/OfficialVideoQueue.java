package com.bgssai.shortai.production.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.mapper.ShortAiTaskMapper;
import com.bgssai.shortai.model.ShortAiTask;
import com.bgssai.shortai.model.ShortAiTaskExample;
import com.bgssai.shortai.production.support.OfficialVideoLane;

/**
 * Queue position is counted when the task list is read. It is not stored.
 * The count includes every company video ahead of this one, because the GPU lane is shared.
 */
@Service
public class OfficialVideoQueue {

    @Autowired
    private ShortAiTaskMapper shortAiTaskMapper;

    public int ahead(ShortAiTask row) {
        if (!OfficialVideoLane.isOfficial(row) || row.getId() == null || !"QUEUED".equals(row.getTaskStatus())) {
            return 0;
        }
        ShortAiTaskExample running = new ShortAiTaskExample();
        running.createCriteria().andTaskStatusEqualTo("RUNNING").andTaskTypeEqualTo("VIDEO")
                .andModelCodeEqualTo(OfficialVideoLane.MODEL);
        long busy = shortAiTaskMapper.countByExample(running);
        ShortAiTaskExample queued = new ShortAiTaskExample();
        queued.createCriteria().andTaskStatusEqualTo("QUEUED").andTaskTypeEqualTo("VIDEO")
                .andModelCodeEqualTo(OfficialVideoLane.MODEL).andIdLessThan(row.getId());
        long earlier = shortAiTaskMapper.countByExample(queued);
        long total = busy + earlier;
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    public void enrich(Map<String, Object> item, ShortAiTask row) {
        OfficialVideoLane.enrich(item, row, ahead(row), System.currentTimeMillis());
    }
}
