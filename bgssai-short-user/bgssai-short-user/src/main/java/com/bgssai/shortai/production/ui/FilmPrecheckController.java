package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.FilmService;

@RestController
public class FilmPrecheckController {

    @Autowired
    private FilmService service;

    @NeedAop
    @GetMapping("/bgssai/user/films/precheck")
    public ApiResponse handle(@RequestParam(value = "episode_id", required = false) String episode_id) {
        return service.precheck(episode_id);
    }
}
