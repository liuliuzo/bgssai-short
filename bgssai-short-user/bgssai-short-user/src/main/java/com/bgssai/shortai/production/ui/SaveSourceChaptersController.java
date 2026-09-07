package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.SourceDtos.SourceChaptersSaveReq;
import com.bgssai.shortai.production.service.SourceService;

@RestController
public class SaveSourceChaptersController {

    @Autowired
    private SourceService service;

    @NeedAop
    @PostMapping("/bgssai/user/sources/chapters-save")
    public ApiResponse handle(@RequestBody SourceChaptersSaveReq req) {
        return service.chaptersSave(req);
    }
}
