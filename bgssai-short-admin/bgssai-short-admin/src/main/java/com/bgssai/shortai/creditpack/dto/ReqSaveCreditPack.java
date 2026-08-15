package com.bgssai.shortai.creditpack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReqSaveCreditPack {
    private Long id;
    private String packCode;
    private String name;
    private Integer creditCent;
    private Integer priceCent;
    @JsonProperty("is_active")
    private Integer active;
    private Integer sortOrder;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPackCode() { return packCode; }
    public void setPackCode(String packCode) { this.packCode = packCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCreditCent() { return creditCent; }
    public void setCreditCent(Integer creditCent) { this.creditCent = creditCent; }
    public Integer getPriceCent() { return priceCent; }
    public void setPriceCent(Integer priceCent) { this.priceCent = priceCent; }
    public Integer getActive() { return active; }
    public void setActive(Integer active) { this.active = active; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
