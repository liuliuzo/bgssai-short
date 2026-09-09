package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.FeatureDtos.ApplyVoiceReq;
import com.bgssai.shortai.production.dto.FeatureDtos.DubbingAlignBatchReq;
import com.bgssai.shortai.production.dto.FeatureDtos.DubbingAlignReq;
import com.bgssai.shortai.production.dto.FeatureDtos.DubbingRegenReq;
import com.bgssai.shortai.production.dto.FeatureDtos.DubbingSubmitReq;
import com.bgssai.shortai.production.dto.FeatureDtos.SubtitleRegenReq;
import com.bgssai.shortai.production.dto.FeatureDtos.SubtitleStyleSaveReq;
import com.bgssai.shortai.production.dto.FeatureDtos.SubtitleTranscribeReq;
import com.bgssai.shortai.production.dto.FeatureDtos.SubtitleUpdateReq;
import com.bgssai.shortai.production.service.DubbingService;

@RestController
public class DubbingControllers {

    @Autowired
    private DubbingService service;

    @NeedAop
    @PostMapping("/bgssai/user/dubbings/estimate")
    public ApiResponse estimate(@RequestBody DubbingSubmitReq req) {
        return service.estimate(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/dubbings/submit")
    public ApiResponse submit(@RequestBody DubbingSubmitReq req) {
        return service.submit(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/dubbings/regenerate")
    public ApiResponse regenerate(@RequestBody DubbingRegenReq req) {
        return service.regenerate(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/dubbings/align")
    public ApiResponse align(@RequestBody DubbingAlignReq req) {
        return service.align(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/dubbings/align-batch")
    public ApiResponse alignBatch(@RequestBody DubbingAlignBatchReq req) {
        return service.alignBatch(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/dubbings/apply-voice-to-character")
    public ApiResponse applyVoice(@RequestBody ApplyVoiceReq req) {
        return service.applyVoiceToCharacter(req);
    }

    @NeedAop
    @GetMapping("/bgssai/user/dubbings/list")
    public ApiResponse list(@RequestParam("episode_id") String episodeId) {
        return service.list(episodeId);
    }

    @NeedAop
    @GetMapping("/bgssai/user/voices/list")
    public ApiResponse voices() {
        return service.voices();
    }

    @NeedAop
    @GetMapping("/bgssai/user/subtitles/detail")
    public ApiResponse subtitleDetail(@RequestParam("episode_id") String episodeId) {
        return service.subtitleDetail(episodeId);
    }

    @NeedAop
    @PostMapping("/bgssai/user/subtitles/regenerate")
    public ApiResponse subtitleRegen(@RequestBody SubtitleRegenReq req) {
        return service.subtitleRegenerate(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/subtitles/update")
    public ApiResponse subtitleUpdate(@RequestBody SubtitleUpdateReq req) {
        return service.subtitleUpdate(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/subtitles/style-save")
    public ApiResponse styleSave(@RequestBody SubtitleStyleSaveReq req) {
        return service.styleSave(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/subtitles/transcribe")
    public ApiResponse transcribe(@RequestBody SubtitleTranscribeReq req) {
        return service.transcribe(req);
    }
}
