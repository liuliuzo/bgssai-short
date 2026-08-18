package com.bgssai.shortai.production.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.ReviewAdminService;

@RestController
public class GetAdminReviewDetailController {

    @Autowired
    private ReviewAdminService reviewAdminService;

    @NeedAop
    @GetMapping("/bgssai/admin/reviews/detail")
    public ApiResponse handle(@RequestParam("review_id") String reviewId) {
        return reviewAdminService.detail(reviewId);
    }
}
