package com.bgssai.shortai.usage.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.usage.service.UsageUserService;

@RestController
public class UsageRecordsController {

    @Autowired
    private UsageUserService service;

    @NeedAop
    @GetMapping("/bgssai/user/usage/records")
    public ApiResponse handle(
            @RequestParam(value = "page_num", defaultValue = "1") int pageNum,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(value = "work_id", required = false) String workId,
            @RequestParam(value = "episode_id", required = false) String episodeId,
            @RequestParam(value = "ability_type", required = false) String abilityType,
            @RequestParam(value = "start_date", required = false) String startDate,
            @RequestParam(value = "end_date", required = false) String endDate) {
        return service.records(pageNum, pageSize, workId, episodeId, abilityType, startDate, endDate);
    }
}
