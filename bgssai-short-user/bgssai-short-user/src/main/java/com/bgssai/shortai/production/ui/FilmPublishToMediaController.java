package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.ProductionDtos.FilmPublishToMediaReq;
import com.bgssai.shortai.production.service.FilmPublishToMediaService;

@RestController
public class FilmPublishToMediaController {

    @Autowired
    private FilmPublishToMediaService service;

    @NeedAop
    @PostMapping("/bgssai/user/films/publish-to-media")
    public ApiResponse handle(@RequestBody FilmPublishToMediaReq req) {
        return service.publish(req);
    }
}
