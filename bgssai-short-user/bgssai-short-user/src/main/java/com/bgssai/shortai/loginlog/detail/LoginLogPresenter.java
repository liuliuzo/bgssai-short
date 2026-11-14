package com.bgssai.shortai.loginlog.detail;

import com.bgssai.shortai.common.ShanghaiClock;
import com.bgssai.shortai.loginlog.dto.LoginLogItem;
import com.bgssai.shortai.loginlog.location.IpGeo;
import com.bgssai.shortai.loginlog.location.LoginIpLocationResolver;
import com.bgssai.shortai.model.LoginLog;

/** Maps a stored login row onto the list item, filling only facts that can be derived. */
public final class LoginLogPresenter {

    private LoginLogPresenter() {
    }

    public static LoginLogItem toItem(LoginLog row, LoginIpLocationResolver resolver) {
        LoginLogItem item = new LoginLogItem();
        if (row == null) {
            return item;
        }
        item.setId(row.getId());
        item.setUserId(row.getUserId());
        item.setAction(row.getAction());
        applyResult(item, row);
        item.setLoginMethod(row.getLoginMethod());
        item.setIdentifierType(row.getIdentifierType());
        item.setIdentifierMasked(row.getIdentifierMasked());
        item.setClientChannel(row.getClientChannel());
        applyDevice(item, row);
        item.setIpAddress(row.getIpAddress());
        item.setUserAgent(row.getUserAgent());
        applyGeo(item, row, resolver);
        item.setRequestScheme(row.getRequestScheme());
        item.setTlsProtocol(row.getTlsProtocol());
        item.setRefererHost(row.getRefererHost());
        item.setEntryPath(row.getEntryPath());
        item.setRequestId(looksJwt(row.getRequestId()) ? null : row.getRequestId());
        item.setSessionId(safeSession(row.getSessionId()));
        item.setSessionIssuedAt(ShanghaiClock.format(row.getSessionIssuedAt()));
        item.setSessionExpiresAt(ShanghaiClock.format(row.getSessionExpiresAt()));
        item.setSessionLastSeenAt(ShanghaiClock.format(row.getSessionLastSeenAt()));
        item.setLogoutTime(ShanghaiClock.format(row.getLogoutTime()));
        item.setAcceptLanguage(row.getAcceptLanguage());
        item.setClientTimezone(row.getClientTimezone());
        item.setClientHints(row.getClientHints());
        item.setRiskNewDevice(row.getRiskNewDevice());
        item.setRiskNewCity(row.getRiskNewCity());
        item.setRiskSummary(LoginRiskSignals.summary(row.getRiskNewDevice(), row.getRiskNewCity()));
        item.setCreateTime(ShanghaiClock.format(row.getCreateTime()));
        return item;
    }

    private static void applyResult(LoginLogItem item, LoginLog row) {
        if (row.getLoginResult() != null) {
            item.setLoginResult(row.getLoginResult());
            item.setResultReason(row.getResultReason());
            return;
        }
        String action = row.getAction();
        if ("LOGIN_SUCCESS".equals(action)) {
            item.setLoginResult("SUCCESS");
            item.setResultReason("登录成功");
        } else if ("LOGIN_FAIL_PASSWORD".equals(action)) {
            item.setLoginResult("FAILURE");
            item.setResultReason("账号或密码不正确");
        } else if ("LOGIN_FAIL_ROLE".equals(action)) {
            item.setLoginResult("FAILURE");
            item.setResultReason("无管理端权限");
        } else if ("LOGIN_FAIL_OTP".equals(action)) {
            item.setLoginResult("FAILURE");
            item.setResultReason("验证码错误或已过期");
        } else if ("LOGIN_FAIL_OAUTH".equals(action)) {
            item.setLoginResult("FAILURE");
            item.setResultReason("第三方登录未完成");
        }
    }

    private static void applyDevice(LoginLogItem item, LoginLog row) {
        if (row.getBrowserName() != null || row.getOsName() != null || row.getDeviceLabel() != null) {
            item.setBrowserName(row.getBrowserName());
            item.setOsName(row.getOsName());
            item.setDeviceType(row.getDeviceType());
            item.setDeviceLabel(row.getDeviceLabel());
            return;
        }
        LoginUserAgent.Summary summary = LoginUserAgent.parse(row.getUserAgent());
        item.setBrowserName(summary.getBrowserName());
        item.setOsName(summary.getOsName());
        item.setDeviceType(summary.getDeviceType());
        item.setDeviceLabel(summary.getDeviceLabel());
    }

    private static void applyGeo(LoginLogItem item, LoginLog row, LoginIpLocationResolver resolver) {
        String live = "未知";
        if (resolver != null) {
            try {
                live = resolver.resolve(row.getIpAddress());
            } catch (RuntimeException ex) {
                live = "未知";
            }
        }
        item.setIpLocation(live);
        boolean stored = row.getIpCountry() != null || row.getIpProvince() != null
                || row.getIpCity() != null || row.getIpIsp() != null;
        if (stored) {
            item.setIpCountry(row.getIpCountry());
            item.setIpProvince(row.getIpProvince());
            item.setIpCity(row.getIpCity());
            item.setIpIsp(row.getIpIsp());
            item.setIpGeoSource("snapshot");
            return;
        }
        if (resolver == null) {
            item.setIpGeoSource("none");
            return;
        }
        try {
            IpGeo geo = resolver.lookup(row.getIpAddress());
            if (geo != null && (geo.getCountry() != null || geo.getProvince() != null
                    || geo.getCity() != null || geo.getIsp() != null)) {
                item.setIpCountry(geo.getCountry());
                item.setIpProvince(geo.getProvince());
                item.setIpCity(geo.getCity());
                item.setIpIsp(geo.getIsp());
                item.setIpGeoSource("lookup");
                return;
            }
        } catch (RuntimeException ignored) {
            // The display string above already says the lookup did not yield a place.
        }
        item.setIpGeoSource("none");
    }

    private static String safeSession(String value) {
        if (value == null || value.isBlank() || looksJwt(value)) return null;
        return value;
    }

    private static boolean looksJwt(String value) {
        return value != null && (value.startsWith("eyJ")
                || value.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"));
    }
}
