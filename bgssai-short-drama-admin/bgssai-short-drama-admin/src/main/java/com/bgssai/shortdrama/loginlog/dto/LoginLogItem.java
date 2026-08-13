package com.bgssai.shortdrama.loginlog.dto;

import java.util.Date;

/**
 * One login-log row for the list response.
 * Java properties stay camelCase; the global Jackson snake_case strategy (BgssConfig)
 * serialises them as user_id / ip_address / create_time on the wire (Standards §1).
 */
public class LoginLogItem {

    private Integer id;

    private String userId;

    private String action;

    private String ipAddress;

    private String userAgent;

    private Date createTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
