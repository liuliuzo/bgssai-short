package com.bgssai.shortai.production.dto;

import java.util.List;
import java.util.Map;

public class ReviewActionReq {

    private String reviewId;
    private String reviewComment;
    private List<Map<String, Object>> rejectItemList;

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public List<Map<String, Object>> getRejectItemList() {
        return rejectItemList;
    }

    public void setRejectItemList(List<Map<String, Object>> rejectItemList) {
        this.rejectItemList = rejectItemList;
    }
}
