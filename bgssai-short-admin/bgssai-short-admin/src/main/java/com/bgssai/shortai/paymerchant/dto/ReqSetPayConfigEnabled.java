package com.bgssai.shortai.paymerchant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enable / disable request body shared by both channels.
 */
public class ReqSetPayConfigEnabled {

    private Long id;

    @JsonProperty("is_enabled")
    private Boolean enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
