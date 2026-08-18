package com.bgssai.shortai.production.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortShotMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortShotExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;

@Service
public class WorkStatusService {

    @Autowired
    private ShortWorkMapper shortWorkMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;
    @Autowired
    private ShortShotMapper shortShotMapper;

    public void refresh(String workId) {
        ShortWorkExample wx = new ShortWorkExample();
        wx.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(wx);
        if (work == null) {
            return;
        }
        if (Integer.valueOf(1).equals(work.getArchivedFlag())) {
            patchStatus(work, "ARCHIVED");
            return;
        }
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andWorkIdEqualTo(workId).andDeletedFlagEqualTo(0);
        List<ShortEpisode> episodes = shortEpisodeMapper.selectByExample(ex);
        String status = "DRAFT";
        boolean allApproved = !episodes.isEmpty();
        for (ShortEpisode ep : episodes) {
            if (!"APPROVED".equals(ep.getReviewStatus())) {
                allApproved = false;
            }
            String candidate = ofEpisode(ep);
            if (rank(candidate) > rank(status)) {
                status = candidate;
            }
        }
        if (allApproved) {
            status = "COMPLETED";
        }
        patchStatus(work, status);
    }

    private String ofEpisode(ShortEpisode ep) {
        if ("PENDING".equals(ep.getReviewStatus()) || "APPROVED".equals(ep.getReviewStatus())
                || "REJECTED".equals(ep.getReviewStatus())) {
            if ("PENDING".equals(ep.getReviewStatus())) {
                return "REVIEWING";
            }
        }
        if ("COMPOSING".equals(ep.getComposeStatus()) || "READY".equals(ep.getComposeStatus())) {
            return "COMPOSING";
        }
        ShortShotExample sx = new ShortShotExample();
        sx.createCriteria().andEpisodeIdEqualTo(ep.getEpisodeId()).andDeletedFlagEqualTo(0);
        if (shortShotMapper.countByExample(sx) > 0) {
            return "PRODUCING";
        }
        if (ep.getScriptStatus() != null && !"NONE".equals(ep.getScriptStatus())) {
            return "SCRIPTING";
        }
        return "DRAFT";
    }

    private int rank(String status) {
        List<String> order = new ArrayList<>();
        order.add("DRAFT");
        order.add("SCRIPTING");
        order.add("PRODUCING");
        order.add("COMPOSING");
        order.add("REVIEWING");
        order.add("COMPLETED");
        return order.indexOf(status);
    }

    private void patchStatus(ShortWork work, String status) {
        if (status.equals(work.getWorkStatus())) {
            return;
        }
        ShortWork patch = new ShortWork();
        patch.setWorkStatus(status);
        patch.setUpdatedTime(new Date());
        ShortWorkExample up = new ShortWorkExample();
        up.createCriteria().andIdEqualTo(work.getId());
        shortWorkMapper.updateByExampleSelective(patch, up);
    }
}
