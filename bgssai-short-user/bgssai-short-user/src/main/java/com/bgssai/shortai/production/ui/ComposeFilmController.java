package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.ProductionDtos;
import com.bgssai.shortai.production.service.FilmService;

@RestController
public class ComposeFilmController {

    @Autowired
    private FilmService service;

    @NeedAop
    @PostMapping("/bgssai/user/films/compose-estimate")
    public ApiResponse estimate(@RequestBody ProductionDtos.FilmReq req) {
        return service.estimateCompose(req);
    }

    @NeedAop
    @PostMapping("/bgssai/user/films/compose")
    public ApiResponse handle(@RequestBody ProductionDtos.FilmReq req) {
        return service.compose(req);
    }
}
