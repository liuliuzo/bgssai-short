package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.ProductionDtos;
import com.bgssai.shortai.production.service.CharacterService;

@RestController
public class PortraitSceneCardController {

    @Autowired
    private CharacterService service;

    @NeedAop
    @PostMapping("/bgssai/user/scene-cards/portrait-generate")
    public ApiResponse handle(@RequestBody ProductionDtos.CharacterReq req) {
        return service.portraitScene(req);
    }
}
