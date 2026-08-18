package com.bgssai.shortai.production.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bgssai.shortai.common.aop.UserContext;
import com.bgssai.shortai.common.exception.ForbiddenException;
import com.bgssai.shortai.common.exception.UnauthorizedException;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortEpisodeExample;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.model.ShortWorkExample;

@Service
public class WorkAccessService {

    @Autowired
    private ShortWorkMapper shortWorkMapper;
    @Autowired
    private ShortEpisodeMapper shortEpisodeMapper;

    public String requireUser() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isEmpty()) {
            throw new UnauthorizedException("no user in context");
        }
        return userId;
    }

    public ShortWork requireWork(String workId) {
        String userId = requireUser();
        if (workId == null || workId.isBlank()) {
            throw new ForbiddenException("work not found");
        }
        ShortWorkExample ex = new ShortWorkExample();
        ex.createCriteria().andWorkIdEqualTo(workId.trim()).andDeletedFlagEqualTo(0);
        ShortWork work = shortWorkMapper.selectOneByExample(ex);
        if (work == null || !userId.equals(work.getUserId())) {
            throw new ForbiddenException("work not found");
        }
        return work;
    }

    public ShortEpisode requireEpisode(String episodeId) {
        String userId = requireUser();
        if (episodeId == null || episodeId.isBlank()) {
            throw new ForbiddenException("episode not found");
        }
        ShortEpisodeExample ex = new ShortEpisodeExample();
        ex.createCriteria().andEpisodeIdEqualTo(episodeId.trim()).andDeletedFlagEqualTo(0);
        ShortEpisode episode = shortEpisodeMapper.selectOneByExample(ex);
        if (episode == null || !userId.equals(episode.getUserId())) {
            throw new ForbiddenException("episode not found");
        }
        return episode;
    }
}
