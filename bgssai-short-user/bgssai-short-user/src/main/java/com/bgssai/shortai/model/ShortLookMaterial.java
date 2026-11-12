package com.bgssai.shortai.model;

import java.io.Serializable;
import java.util.Date;

public class ShortLookMaterial implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String materialId;
    private String libraryId;
    private String userId;
    private String materialName;
    private String caption;
    private String materialKind;
    private String viewAngle;
    private String cardId;
    private String assetId;
    private String mimeType;
    private String storageKey;
    private Integer width;
    private Integer height;
    private Integer fileSizeByte;
    private Integer deletedFlag;
    private Date createdTime;
    private Date updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getLibraryId() { return libraryId; }
    public void setLibraryId(String libraryId) { this.libraryId = libraryId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getMaterialKind() { return materialKind; }
    public void setMaterialKind(String materialKind) { this.materialKind = materialKind; }
    public String getViewAngle() { return viewAngle; }
    public void setViewAngle(String viewAngle) { this.viewAngle = viewAngle; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Integer getFileSizeByte() { return fileSizeByte; }
    public void setFileSizeByte(Integer fileSizeByte) { this.fileSizeByte = fileSizeByte; }
    public Integer getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Integer deletedFlag) { this.deletedFlag = deletedFlag; }
    public Date getCreatedTime() { return createdTime; }
    public void setCreatedTime(Date createdTime) { this.createdTime = createdTime; }
    public Date getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Date updatedTime) { this.updatedTime = updatedTime; }
}
