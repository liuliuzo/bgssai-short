package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortReferenceTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String templateId;

    private String templateName;

    private String genre;

    private String hookTypeJson;

    private String beatSheetJson;

    private String turningPointJson;

    private String tabooListJson;

    private Integer enabledFlag;

    private Integer deletedFlag;

    private Date createdTime;

    private Date updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId == null ? null : templateId.trim();
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName == null ? null : templateName.trim();
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre == null ? null : genre.trim();
    }

    public String getHookTypeJson() {
        return hookTypeJson;
    }

    public void setHookTypeJson(String hookTypeJson) {
        this.hookTypeJson = hookTypeJson == null ? null : hookTypeJson.trim();
    }

    public String getBeatSheetJson() {
        return beatSheetJson;
    }

    public void setBeatSheetJson(String beatSheetJson) {
        this.beatSheetJson = beatSheetJson == null ? null : beatSheetJson.trim();
    }

    public String getTurningPointJson() {
        return turningPointJson;
    }

    public void setTurningPointJson(String turningPointJson) {
        this.turningPointJson = turningPointJson == null ? null : turningPointJson.trim();
    }

    public String getTabooListJson() {
        return tabooListJson;
    }

    public void setTabooListJson(String tabooListJson) {
        this.tabooListJson = tabooListJson == null ? null : tabooListJson.trim();
    }

    public Integer getEnabledFlag() {
        return enabledFlag;
    }

    public void setEnabledFlag(Integer enabledFlag) {
        this.enabledFlag = enabledFlag;
    }

    public Integer getDeletedFlag() {
        return deletedFlag;
    }

    public void setDeletedFlag(Integer deletedFlag) {
        this.deletedFlag = deletedFlag;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
    }

}
