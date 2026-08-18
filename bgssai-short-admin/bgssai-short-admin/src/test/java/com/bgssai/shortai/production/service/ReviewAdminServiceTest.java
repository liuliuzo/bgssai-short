package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.common.AdminAuthGuard;
import com.bgssai.shortai.mapper.ShortEpisodeMapper;
import com.bgssai.shortai.mapper.ShortReviewRecordMapper;
import com.bgssai.shortai.mapper.ShortWorkMapper;
import com.bgssai.shortai.model.ShortEpisode;
import com.bgssai.shortai.model.ShortReviewRecord;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.ReviewActionReq;

class ReviewAdminServiceTest {

    @Test
    void approveCompletesWorkWhenAllEpisodesApproved() {
        ReviewAdminService service = new ReviewAdminService();
        ShortReviewRecordMapper reviewMapper = org.mockito.Mockito.mock(ShortReviewRecordMapper.class);
        ShortEpisodeMapper episodeMapper = org.mockito.Mockito.mock(ShortEpisodeMapper.class);
        ShortWorkMapper workMapper = org.mockito.Mockito.mock(ShortWorkMapper.class);
        ReflectionTestUtils.setField(service, "shortReviewRecordMapper", reviewMapper);
        ReflectionTestUtils.setField(service, "shortEpisodeMapper", episodeMapper);
        ReflectionTestUtils.setField(service, "shortWorkMapper", workMapper);

        ShortReviewRecord row = new ShortReviewRecord();
        row.setId(1L);
        row.setReviewId("r-1");
        row.setWorkId("w-1");
        row.setEpisodeId("e-1");
        row.setReviewStatus("PENDING");
        when(reviewMapper.selectOneByExample(any())).thenReturn(row);

        ShortEpisode pending = new ShortEpisode();
        pending.setId(2L);
        pending.setEpisodeId("e-1");
        pending.setReviewStatus("PENDING");
        ShortEpisode approvedAfter = new ShortEpisode();
        approvedAfter.setEpisodeId("e-1");
        approvedAfter.setReviewStatus("APPROVED");
        when(episodeMapper.selectOneByExample(any())).thenReturn(pending);
        List<ShortEpisode> all = new ArrayList<>();
        all.add(approvedAfter);
        when(episodeMapper.selectByExample(any())).thenReturn(all);

        ShortWork work = new ShortWork();
        work.setId(3L);
        work.setWorkId("w-1");
        work.setWorkStatus("REVIEWING");
        when(workMapper.selectOneByExample(any())).thenReturn(work);

        ReviewActionReq req = new ReviewActionReq();
        req.setReviewId("r-1");
        try (MockedStatic<AdminAuthGuard> mocked = mockStatic(AdminAuthGuard.class)) {
            mocked.when(AdminAuthGuard::isAdmin).thenReturn(true);
            service.approve(req);
        }
        ArgumentCaptor<ShortWork> captor = ArgumentCaptor.forClass(ShortWork.class);
        org.mockito.Mockito.verify(workMapper).updateByExampleSelective(captor.capture(), any());
        assertEquals("COMPLETED", captor.getValue().getWorkStatus());
    }
}
