package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortLookLibrary implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String libraryId;
    private String userId;
    private String libraryName;
    private String description;
    private String status;
    private Integer mountPortrait;
    private Integer itemCount;
    private Long usageBytes;
    private Integer deletedFlag;
    private Date createdTime;
    private Date updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLibraryId() { return libraryId; }
    public void setLibraryId(String libraryId) { this.libraryId = libraryId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getLibraryName() { return libraryName; }
    public void setLibraryName(String libraryName) { this.libraryName = libraryName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getMountPortrait() { return mountPortrait; }
    public void setMountPortrait(Integer mountPortrait) { this.mountPortrait = mountPortrait; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public Long getUsageBytes() { return usageBytes; }
    public void setUsageBytes(Long usageBytes) { this.usageBytes = usageBytes; }
    public Integer getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Integer deletedFlag) { this.deletedFlag = deletedFlag; }
    public Date getCreatedTime() { return createdTime; }
    public void setCreatedTime(Date createdTime) { this.createdTime = createdTime; }
    public Date getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Date updatedTime) { this.updatedTime = updatedTime; }
}
