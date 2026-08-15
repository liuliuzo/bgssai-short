package com.bgssai.shortai.paymerchant.dto;

/**
 * Result payload carrying only the affected row id (save / delete).
 */
public class PayConfigIdResult {

    private Long id;

    public PayConfigIdResult(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
