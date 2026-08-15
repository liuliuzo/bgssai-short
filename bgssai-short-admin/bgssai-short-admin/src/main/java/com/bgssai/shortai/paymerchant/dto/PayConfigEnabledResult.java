package com.bgssai.shortai.paymerchant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result payload of the enable / disable endpoint. Carries both state bits so the console can
 * render the row without a reload and see that disabling also cleared the collection flag (PMC-04).
 */
public class PayConfigEnabledResult {

    private Long id;

    @JsonProperty("is_enabled")
    private Boolean enabled;

    @JsonProperty("is_collection_active")
    private Boolean collectionActive;

    public PayConfigEnabledResult(Long id, Boolean enabled, Boolean collectionActive) {
        this.id = id;
        this.enabled = enabled;
        this.collectionActive = collectionActive;
    }

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

    public Boolean getCollectionActive() {
        return collectionActive;
    }

    public void setCollectionActive(Boolean collectionActive) {
        this.collectionActive = collectionActive;
    }
}
