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
public class UpdateSceneCardController {

    @Autowired
    private CharacterService service;

    @NeedAop
    @PostMapping("/bgssai/user/scene-cards/update")
    public ApiResponse handle(@RequestBody ProductionDtos.CharacterReq req) {
        return service.updateScene(req);
    }
}
