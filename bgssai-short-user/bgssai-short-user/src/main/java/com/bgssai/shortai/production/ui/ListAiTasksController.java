package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.AiTaskService;

@RestController
public class ListAiTasksController {

    @Autowired
    private AiTaskService service;

    @NeedAop
    @GetMapping("/bgssai/user/ai-tasks/list")
    public PageResponse handle(@RequestParam(value = "page_num", required = false, defaultValue = "1") int page_num, @RequestParam(value = "page_size", required = false, defaultValue = "20") int page_size, @RequestParam(value = "task_type", required = false) String task_type, @RequestParam(value = "task_status", required = false) String task_status, @RequestParam(value = "batch_id", required = false) String batch_id, @RequestParam(value = "episode_id", required = false) String episode_id, @RequestParam(value = "work_id", required = false) String work_id) {
        return service.list(page_num, page_size, task_type, task_status, batch_id, episode_id, work_id);
    }
}
