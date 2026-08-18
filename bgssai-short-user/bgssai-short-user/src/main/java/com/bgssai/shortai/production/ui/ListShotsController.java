package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.ShotService;

@RestController
public class ListShotsController {

    @Autowired
    private ShotService service;

    @NeedAop
    @GetMapping("/bgssai/user/shots/list")
    public PageResponse handle(@RequestParam(value = "episode_id", required = false) String episode_id, @RequestParam(value = "page_num", required = false, defaultValue = "1") int page_num, @RequestParam(value = "page_size", required = false, defaultValue = "50") int page_size, @RequestParam(value = "shot_status", required = false) String shot_status) {
        return service.list(episode_id, page_num, page_size, shot_status);
    }
}
