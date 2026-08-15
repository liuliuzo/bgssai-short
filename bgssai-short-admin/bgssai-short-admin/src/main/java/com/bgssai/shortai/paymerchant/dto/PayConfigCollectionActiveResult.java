package com.bgssai.shortai.paymerchant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result payload of the "set collection active" endpoint.
 */
public class PayConfigCollectionActiveResult {

    private Long id;

    @JsonProperty("is_collection_active")
    private Boolean collectionActive;

    public PayConfigCollectionActiveResult(Long id, Boolean collectionActive) {
        this.id = id;
        this.collectionActive = collectionActive;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getCollectionActive() {
        return collectionActive;
    }

    public void setCollectionActive(Boolean collectionActive) {
        this.collectionActive = collectionActive;
    }
}
