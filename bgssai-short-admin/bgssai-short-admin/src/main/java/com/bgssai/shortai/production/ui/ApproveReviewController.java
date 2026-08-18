package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.dto.ReviewActionReq;
import com.bgssai.shortai.production.service.ReviewAdminService;

@RestController
public class ApproveReviewController {

    @Autowired
    private ReviewAdminService reviewAdminService;

    @NeedAop
    @PostMapping("/bgssai/admin/reviews/approve")
    public ApiResponse handle(@RequestBody ReviewActionReq req) {
        return reviewAdminService.approve(req);
    }
}
