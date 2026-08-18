package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.ReviewAdminService;

@RestController
public class ListAdminReviewsController {

    @Autowired
    private ReviewAdminService reviewAdminService;

    @NeedAop
    @GetMapping("/bgssai/admin/reviews/list")
    public PageResponse handle(@RequestParam(name = "page_num", defaultValue = "1") int pageNum,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "review_status", required = false) String reviewStatus,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return reviewAdminService.list(pageNum, pageSize, reviewStatus, keyword);
    }
}
