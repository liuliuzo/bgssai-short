package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.FilmService;

@RestController
public class FilmDownloadUrlController {

    @Autowired
    private FilmService service;

    @NeedAop
    @GetMapping("/bgssai/user/films/download-url")
    public ApiResponse handle(@RequestParam(value = "asset_id", required = false) String asset_id) {
        return service.downloadUrl(asset_id);
    }
}
