package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.ScriptService;

@RestController
public class ListScriptVersionsController {

    @Autowired
    private ScriptService service;

    @NeedAop
    @GetMapping("/bgssai/user/scripts/versions")
    public ApiResponse handle(@RequestParam(value = "episode_id", required = false) String episode_id) {
        return service.versions(episode_id);
    }
}
