package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.FeatureDtos.BlockingSaveReq;
import com.bgssai.shortai.production.dto.FeatureDtos.RelationSaveReq;
import com.bgssai.shortai.production.dto.FeatureDtos.StoryMapGenerateReq;
import com.bgssai.shortai.production.service.StoryMapService;

@RestController
public class StoryMapControllers {

    @Autowired
    private StoryMapService service;

    @NeedAop
    @PostMapping("/bgssai/user/story-map/generate")
    public ApiResponse generate(@RequestBody StoryMapGenerateReq req) {
        return service.generate(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/story-map/relation-save")
    public ApiResponse relationSave(@RequestBody RelationSaveReq req) {
        return service.relationSave(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/story-map/blocking-save")
    public ApiResponse blockingSave(@RequestBody BlockingSaveReq req) {
        return service.blockingSave(req);
    }

    @NeedAop
    @GetMapping("/bgssai/user/story-map/tension")
    public ApiResponse tension(@RequestParam("episode_id") String episodeId) {
        return service.tension(episodeId);
    }

    @NeedAop
    @GetMapping("/bgssai/user/story-map/tension-band")
    public ApiResponse tensionBand(@RequestParam("work_id") String workId) {
        return service.tensionBand(workId);
    }

    @NeedAop
    @GetMapping("/bgssai/user/story-map/relations")
    public ApiResponse relations(@RequestParam("work_id") String workId,
            @RequestParam(value = "as_of_episode_no", required = false) Integer asOfEpisodeNo) {
        return service.relations(workId, asOfEpisodeNo);
    }

    @NeedAop
    @GetMapping("/bgssai/user/story-map/blocking")
    public ApiResponse blocking(@RequestParam("shot_id") String shotId) {
        return service.blocking(shotId);
    }

    @NeedAop
    @GetMapping("/bgssai/user/story-map/scene-band")
    public ApiResponse sceneBand(@RequestParam("episode_id") String episodeId) {
        return service.sceneBand(episodeId);
    }

    @NeedAop
    @GetMapping("/bgssai/user/story-map/appearance-heat")
    public ApiResponse appearanceHeat(@RequestParam("work_id") String workId) {
        return service.appearanceHeat(workId);
    }
}
