package com.bgssai.shortai.loginlog.dto;

/**
 * One login-log row for the list response.
 * Java properties stay camelCase; Jackson snake_case serialises them on the wire.
 */
public class LoginLogItem {
    private Integer id;
    private String userId;
    private String action;
    private String loginResult;
    private String resultReason;
    private String loginMethod;
    private String identifierType;
    private String identifierMasked;
    private String clientChannel;
    private String browserName;
    private String osName;
    private String deviceType;
    private String deviceLabel;
    private String ipAddress;
    private String ipLocation;
    private String ipCountry;
    private String ipProvince;
    private String ipCity;
    private String ipIsp;
    private String ipGeoSource;
    private String userAgent;
    private String requestScheme;
    private String tlsProtocol;
    private String refererHost;
    private String entryPath;
    private String requestId;
    private String sessionId;
    private String sessionIssuedAt;
    private String sessionExpiresAt;
    private String sessionLastSeenAt;
    private String logoutTime;
    private String acceptLanguage;
    private String clientTimezone;
    private String clientHints;
    private Boolean riskNewDevice;
    private Boolean riskNewCity;
    private String riskSummary;
    private String createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getLoginResult() { return loginResult; }
    public void setLoginResult(String loginResult) { this.loginResult = loginResult; }
    public String getResultReason() { return resultReason; }
    public void setResultReason(String resultReason) { this.resultReason = resultReason; }
    public String getLoginMethod() { return loginMethod; }
    public void setLoginMethod(String loginMethod) { this.loginMethod = loginMethod; }
    public String getIdentifierType() { return identifierType; }
    public void setIdentifierType(String identifierType) { this.identifierType = identifierType; }
    public String getIdentifierMasked() { return identifierMasked; }
    public void setIdentifierMasked(String identifierMasked) { this.identifierMasked = identifierMasked; }
    public String getClientChannel() { return clientChannel; }
    public void setClientChannel(String clientChannel) { this.clientChannel = clientChannel; }
    public String getBrowserName() { return browserName; }
    public void setBrowserName(String browserName) { this.browserName = browserName; }
    public String getOsName() { return osName; }
    public void setOsName(String osName) { this.osName = osName; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getDeviceLabel() { return deviceLabel; }
    public void setDeviceLabel(String deviceLabel) { this.deviceLabel = deviceLabel; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getIpLocation() { return ipLocation; }
    public void setIpLocation(String ipLocation) { this.ipLocation = ipLocation; }
    public String getIpCountry() { return ipCountry; }
    public void setIpCountry(String ipCountry) { this.ipCountry = ipCountry; }
    public String getIpProvince() { return ipProvince; }
    public void setIpProvince(String ipProvince) { this.ipProvince = ipProvince; }
    public String getIpCity() { return ipCity; }
    public void setIpCity(String ipCity) { this.ipCity = ipCity; }
    public String getIpIsp() { return ipIsp; }
    public void setIpIsp(String ipIsp) { this.ipIsp = ipIsp; }
    public String getIpGeoSource() { return ipGeoSource; }
    public void setIpGeoSource(String ipGeoSource) { this.ipGeoSource = ipGeoSource; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getRequestScheme() { return requestScheme; }
    public void setRequestScheme(String requestScheme) { this.requestScheme = requestScheme; }
    public String getTlsProtocol() { return tlsProtocol; }
    public void setTlsProtocol(String tlsProtocol) { this.tlsProtocol = tlsProtocol; }
    public String getRefererHost() { return refererHost; }
    public void setRefererHost(String refererHost) { this.refererHost = refererHost; }
    public String getEntryPath() { return entryPath; }
    public void setEntryPath(String entryPath) { this.entryPath = entryPath; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getSessionIssuedAt() { return sessionIssuedAt; }
    public void setSessionIssuedAt(String sessionIssuedAt) { this.sessionIssuedAt = sessionIssuedAt; }
    public String getSessionExpiresAt() { return sessionExpiresAt; }
    public void setSessionExpiresAt(String sessionExpiresAt) { this.sessionExpiresAt = sessionExpiresAt; }
    public String getSessionLastSeenAt() { return sessionLastSeenAt; }
    public void setSessionLastSeenAt(String sessionLastSeenAt) { this.sessionLastSeenAt = sessionLastSeenAt; }
    public String getLogoutTime() { return logoutTime; }
    public void setLogoutTime(String logoutTime) { this.logoutTime = logoutTime; }
    public String getAcceptLanguage() { return acceptLanguage; }
    public void setAcceptLanguage(String acceptLanguage) { this.acceptLanguage = acceptLanguage; }
    public String getClientTimezone() { return clientTimezone; }
    public void setClientTimezone(String clientTimezone) { this.clientTimezone = clientTimezone; }
    public String getClientHints() { return clientHints; }
    public void setClientHints(String clientHints) { this.clientHints = clientHints; }
    public Boolean getRiskNewDevice() { return riskNewDevice; }
    public void setRiskNewDevice(Boolean riskNewDevice) { this.riskNewDevice = riskNewDevice; }
    public Boolean getRiskNewCity() { return riskNewCity; }
    public void setRiskNewCity(Boolean riskNewCity) { this.riskNewCity = riskNewCity; }
    public String getRiskSummary() { return riskSummary; }
    public void setRiskSummary(String riskSummary) { this.riskSummary = riskSummary; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
