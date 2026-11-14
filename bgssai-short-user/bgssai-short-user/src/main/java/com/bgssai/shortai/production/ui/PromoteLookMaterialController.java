package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.LookDtos;
import com.bgssai.shortai.production.service.LookKnowledgeService;

@RestController
public class PromoteLookMaterialController {

    @Autowired
    private LookKnowledgeService service;

    @NeedAop
    @PostMapping("/bgssai/user/look-materials/promote")
    public ApiResponse handle(@RequestBody LookDtos.LookMaterialReq req) {
        return service.promoteMaterial(req);
    }
}
