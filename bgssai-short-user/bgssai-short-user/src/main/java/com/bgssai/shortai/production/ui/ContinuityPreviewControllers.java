package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.FeatureDtos.ContinuityRecheckReq;
import com.bgssai.shortai.production.dto.FeatureDtos.ForeshadowSaveReq;
import com.bgssai.shortai.production.service.ContinuityService;
import com.bgssai.shortai.production.service.PreviewService;

@RestController
public class ContinuityPreviewControllers {

    @Autowired
    private ContinuityService continuityService;
    @Autowired
    private PreviewService previewService;

    @NeedAop
    @GetMapping("/bgssai/user/preview/summary")
    public ApiResponse previewSummary(@RequestParam("episode_id") String episodeId) {
        return previewService.summary(episodeId);
    }

    @NeedAop
    @GetMapping("/bgssai/user/continuity/overview")
    public ApiResponse overview(@RequestParam("work_id") String workId) {
        return continuityService.overview(workId);
    }

    @NeedAop
    @GetMapping("/bgssai/user/continuity/foreshadows")
    public ApiResponse foreshadows(@RequestParam("work_id") String workId) {
        return continuityService.foreshadows(workId);
    }

    @NeedAop
    @PostMapping("/bgssai/user/continuity/foreshadow-save")
    public ApiResponse foreshadowSave(@RequestBody ForeshadowSaveReq req) {
        return continuityService.foreshadowSave(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/continuity/recheck")
    public ApiResponse recheck(@RequestBody ContinuityRecheckReq req) {
        return continuityService.recheck(req);
    }
}
