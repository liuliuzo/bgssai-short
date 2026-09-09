package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.ProductionDtos.FilmPublishBatchToMediaReq;
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

    @NeedAop
    @PostMapping("/bgssai/user/films/publish-to-media/batch")
    public ApiResponse batch(@RequestBody FilmPublishBatchToMediaReq req) {
        return service.publishBatch(req);
    }

    @NeedAop
    @GetMapping("/bgssai/user/films/publish-to-media/preflight")
    public ApiResponse preflight(
            @RequestParam(value = "episode_id", required = false) String episodeId,
            @RequestParam(value = "film_id", required = false) String filmId,
            @RequestParam(value = "work_id", required = false) String workId) {
        return service.preflight(episodeId, filmId, workId);
    }
}
