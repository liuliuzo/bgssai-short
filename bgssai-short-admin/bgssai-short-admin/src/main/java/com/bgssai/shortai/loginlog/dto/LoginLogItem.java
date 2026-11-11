package com.bgssai.shortai.loginlog.dto;

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

    private String ipLocation;

    private String userAgent;

    private String createTime;

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

    public String getIpLocation() {
        return ipLocation;
    }

    public void setIpLocation(String ipLocation) {
        this.ipLocation = ipLocation;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
