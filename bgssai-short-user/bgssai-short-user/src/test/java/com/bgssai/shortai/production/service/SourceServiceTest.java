package com.bgssai.shortai.production.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bgssai.shortai.aimodel.AiModelResolveService;
import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.exception.BizException;
import com.bgssai.shortai.mapper.ShortSourceChapterMapper;
import com.bgssai.shortai.mapper.ShortSourceDocMapper;
import com.bgssai.shortai.model.ShortWork;
import com.bgssai.shortai.production.dto.SourceDtos.SourceImportReq;

class SourceServiceTest {

    private final WorkAccessService workAccessService = mock(WorkAccessService.class);
    private final ShortSourceDocMapper docMapper = mock(ShortSourceDocMapper.class);
    private final ShortSourceChapterMapper chapterMapper = mock(ShortSourceChapterMapper.class);
    private final AiModelResolveService aiModelResolveService = mock(AiModelResolveService.class);
    private final AiTaskQueue aiTaskQueue = mock(AiTaskQueue.class);

    private SourceService service() {
        SourceService service = new SourceService();
        ReflectionTestUtils.setField(service, "workAccessService", workAccessService);
        ReflectionTestUtils.setField(service, "shortSourceDocMapper", docMapper);
        ReflectionTestUtils.setField(service, "shortSourceChapterMapper", chapterMapper);
        ReflectionTestUtils.setField(service, "aiModelResolveService", aiModelResolveService);
        ReflectionTestUtils.setField(service, "aiTaskQueue", aiTaskQueue);

        ShortWork work = new ShortWork();
        work.setId(1L);
        work.setWorkId("w-1");
        work.setUserId("u-1");
        work.setWorkTitle("退婚三年后她成了首富");
        when(workAccessService.requireWork(any())).thenReturn(work);
        when(workAccessService.requireUser()).thenReturn("u-1");
        return service;
    }

    private SourceImportReq req(String content, Integer rights) {
        SourceImportReq req = new SourceImportReq();
        req.setWorkId("w-1");
        req.setSourceType("NOVEL");
        req.setSourceTitle("原著名");
        req.setSourceContent(content);
        req.setRightsConfirmFlag(rights);
        return req;
    }

    @Test
    void importsNovelAndPersistsEachChapter() {
        SourceService service = service();
        String content = "第一章 退婚\n正文一。\n\n第二章 归来\n正文二。\n\n第三章 夺权\n正文三。";
        ApiResponse response = service.importSource(req(content, 1));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        assertEquals(3, result.get("chapter_count"));
        assertEquals(content.length(), result.get("char_count"));
        verify(docMapper).insertSelective(any());
        verify(chapterMapper, times(3)).insertSelective(any());
    }

    @Test
    void rejectsImportWithoutRightsConfirmation() {
        SourceService service = service();
        BizException ex = assertThrows(BizException.class,
                () -> service.importSource(req("第一章 退婚\n正文。", null)));
        assertEquals("50000001", ex.getCode());
        // 权利确认是硬前置：没勾就不该有任何落库
        verify(docMapper, never()).insertSelective(any());
        verify(chapterMapper, never()).insertSelective(any());
    }

    @Test
    void rejectsImportOverCharLimit() {
        SourceService service = service();
        String content = "字".repeat(1000001);
        BizException ex = assertThrows(BizException.class, () -> service.importSource(req(content, 1)));
        assertEquals("50000001", ex.getCode());
        verify(docMapper, never()).insertSelective(any());
    }

    @Test
    void rejectsUnknownSourceType() {
        SourceService service = service();
        SourceImportReq req = req("第一章 退婚\n正文。", 1);
        req.setSourceType("COMIC");
        BizException ex = assertThrows(BizException.class, () -> service.importSource(req));
        assertEquals("50000001", ex.getCode());
    }

    @Test
    void rejectsBlankContent() {
        SourceService service = service();
        BizException ex = assertThrows(BizException.class, () -> service.importSource(req("   ", 1)));
        assertEquals("50000001", ex.getCode());
    }
}
