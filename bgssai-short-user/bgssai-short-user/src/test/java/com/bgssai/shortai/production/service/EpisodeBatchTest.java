package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.common.exception.CommonErrorCode;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.FeatureDtos.EpisodeBatchReq;

/**
 * 剧集批量操作：必须显式给出动作；归档 / 删除与单集删除同一规则（已合成的集不可删）。
 */
class EpisodeBatchTest {

    private WorkService service;
    private ShortEpisodeMapper episodeMapper;
    private WorkAccessService access;

    @BeforeEach
    void setUp() {
        service = new WorkService();
        episodeMapper = mock(ShortEpisodeMapper.class);
        access = mock(WorkAccessService.class);
        ReflectionTestUtils.setField(service, "shortEpisodeMapper", episodeMapper);
        ReflectionTestUtils.setField(service, "workAccessService", access);
        ReflectionTestUtils.setField(service, "workStatusService", mock(WorkStatusService.class));
        ShortWork work = new ShortWork();
        work.setWorkId("w-1");
        when(access.requireWork("w-1")).thenReturn(work);
    }

    private EpisodeBatchReq req(String action) {
        EpisodeBatchReq req = new EpisodeBatchReq();
        req.setWorkId("w-1");
        req.setEpisodeIdList(List.of("e-1"));
        req.setAction(action);
        return req;
    }

    private void episode(String composeStatus) {
        ShortEpisode ep = new ShortEpisode();
        ep.setId(1L);
        ep.setEpisodeId("e-1");
        ep.setWorkId("w-1");
        ep.setComposeStatus(composeStatus);
        when(access.requireEpisode("e-1")).thenReturn(ep);
    }

    @Test
    void missing_action_is_rejected_instead_of_archiving() {
        episode("NONE");
        BizException ex = assertThrows(BizException.class, () -> service.batchEpisodes(req(null)));
        assertEquals(CommonErrorCode.INVALID_PARAM.code(), ex.getCode());
        verify(episodeMapper, never()).updateByExampleSelective(any(), any());
    }

    @Test
    void composed_episode_cannot_be_archived_in_batch() {
        episode("READY");
        BizException ex = assertThrows(BizException.class, () -> service.batchEpisodes(req("ARCHIVE")));
        assertEquals(CommonErrorCode.RULE_REJECT.code(), ex.getCode());
        verify(episodeMapper, never()).updateByExampleSelective(any(), any());
    }

    @Test
    void uncomposed_episode_is_archived() {
        episode("NONE");
        service.batchEpisodes(req("ARCHIVE"));
        verify(episodeMapper).updateByExampleSelective(any(), any());
    }
}
