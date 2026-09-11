package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.CharacterService;

@RestController
public class ListConsistencyPanelController {

    @Autowired
    private CharacterService service;

    @NeedAop
    @GetMapping("/bgssai/user/assets/consistency-panel")
    public ApiResponse handle(@RequestParam(value = "work_id", required = false) String work_id) {
        return service.consistencyPanel(work_id);
    }
}
