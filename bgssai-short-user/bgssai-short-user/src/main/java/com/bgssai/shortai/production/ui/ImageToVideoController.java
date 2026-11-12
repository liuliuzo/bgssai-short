package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.ImageToVideoDtos.SubmitReq;
import com.bgssai.shortai.production.service.ImageToVideoService;

@RestController
public class ImageToVideoController {

    @Autowired
    private ImageToVideoService service;

    @NeedAop
    @GetMapping("/bgssai/user/video/image-to-video/capability")
    public ApiResponse capability() {
        return service.capability();
    }

    @NeedAop
    @PostMapping("/bgssai/user/assets/images")
    public ApiResponse upload(@RequestParam("file") MultipartFile file) {
        return service.upload(file);
    }

    @NeedAop
    @PostMapping("/bgssai/user/video/image-to-video")
    public ApiResponse submit(@RequestBody SubmitReq req) {
        return service.submit(req);
    }

    @NeedAop
    @GetMapping("/bgssai/user/video/image-to-video/{task_id}")
    public ApiResponse status(@PathVariable("task_id") String taskId) {
        return service.status(taskId);
    }
}
