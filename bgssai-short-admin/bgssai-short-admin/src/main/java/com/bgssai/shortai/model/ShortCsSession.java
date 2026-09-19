package com.bgssai.shortai.model;

import java.util.Date;

/** short_cs_session — 在线客服会话。手工维护。 */
public class ShortCsSession {

    private Long id;
    private String sessionId;
    private String guestToken;
    private String userId;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String status;
    private Integer unreadUser;
    private Integer unreadAdmin;
    private Date lastMessageAt;
    private Boolean delFlag;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getGuestToken() { return guestToken; }
    public void setGuestToken(String guestToken) { this.guestToken = guestToken; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getUnreadUser() { return unreadUser; }
    public void setUnreadUser(Integer unreadUser) { this.unreadUser = unreadUser; }
    public Integer getUnreadAdmin() { return unreadAdmin; }
    public void setUnreadAdmin(Integer unreadAdmin) { this.unreadAdmin = unreadAdmin; }
    public Date getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Date lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public Boolean getDelFlag() { return delFlag; }
    public void setDelFlag(Boolean delFlag) { this.delFlag = delFlag; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
