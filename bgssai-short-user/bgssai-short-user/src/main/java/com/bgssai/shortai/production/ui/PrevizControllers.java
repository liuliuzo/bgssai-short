package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.FeatureDtos.PrevizGenerateReq;
import com.bgssai.shortai.production.dto.FeatureDtos.PrevizPromptReq;
import com.bgssai.shortai.production.dto.FeatureDtos.PrevizSaveReq;
import com.bgssai.shortai.production.service.PrevizService;

@RestController
public class PrevizControllers {

    @Autowired
    private PrevizService service;

    @NeedAop
    @GetMapping("/bgssai/user/previz/list")
    public ApiResponse list(@RequestParam("episode_id") String episodeId) {
        return service.list(episodeId);
    }

    @NeedAop
    @PostMapping("/bgssai/user/previz/generate-text")
    public ApiResponse generateText(@RequestBody PrevizGenerateReq req) {
        return service.generateText(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/previz/generate-draft")
    public ApiResponse generateDraft(@RequestBody PrevizGenerateReq req) {
        return service.generateDraft(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/previz/prompt-check")
    public ApiResponse promptCheck(@RequestBody PrevizPromptReq req) {
        return service.promptCheck(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/previz/prompt-diff")
    public ApiResponse promptDiff(@RequestBody PrevizPromptReq req) {
        return service.promptDiff(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/previz/save")
    public ApiResponse save(@RequestBody PrevizSaveReq req) {
        return service.save(req);
    }
}
