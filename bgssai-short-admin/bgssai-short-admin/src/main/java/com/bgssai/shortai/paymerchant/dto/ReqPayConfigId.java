package com.bgssai.shortai.paymerchant.dto;

/**
 * Single-id request body shared by the "set collection active" and "delete" endpoints of both
 * channels (POST business parameters travel in the JSON body, CLAUDE.md / Standards §3).
 */
public class ReqPayConfigId {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
