package com.bgssai.shortai.loginlog.detail;

import java.util.ArrayList;
import java.util.List;

/**
 * Compares this login with earlier successful logins that already stored a device or city.
 * Missing history stays unknown. This is not a risk score.
 */
public final class LoginRiskSignals {

    public static final class Prior {
        private final String deviceKey;
        private final String city;

        public Prior(String deviceKey, String city) {
            this.deviceKey = deviceKey;
            this.city = city;
        }
    }

    public static final class Result {
        private final Boolean newDevice;
        private final Boolean newCity;

        Result(Boolean newDevice, Boolean newCity) {
            this.newDevice = newDevice;
            this.newCity = newCity;
        }

        public Boolean getNewDevice() { return newDevice; }
        public Boolean getNewCity() { return newCity; }
    }

    private LoginRiskSignals() {
    }

    public static Result compare(String deviceKey, String city, List<Prior> priors) {
        List<Prior> history = priors == null ? List.of() : priors;
        boolean anyDevice = false;
        boolean seenDevice = false;
        boolean anyCity = false;
        boolean seenCity = false;
        for (Prior prior : history) {
            if (prior == null) continue;
            if (prior.deviceKey != null && !prior.deviceKey.isBlank()) {
                anyDevice = true;
                if (deviceKey != null && deviceKey.equals(prior.deviceKey)) {
                    seenDevice = true;
                }
            }
            if (isCity(prior.city)) {
                anyCity = true;
                if (isCity(city) && city.equals(prior.city)) {
                    seenCity = true;
                }
            }
        }
        Boolean newDevice = deviceKey == null || deviceKey.isBlank() || !anyDevice ? null : !seenDevice;
        Boolean newCity = !isCity(city) || !anyCity ? null : !seenCity;
        return new Result(newDevice, newCity);
    }

    public static String summary(Boolean newDevice, Boolean newCity) {
        List<String> parts = new ArrayList<>();
        if (Boolean.TRUE.equals(newDevice)) parts.add("新设备");
        else if (Boolean.FALSE.equals(newDevice)) parts.add("曾用设备");
        if (Boolean.TRUE.equals(newCity)) parts.add("新城市");
        else if (Boolean.FALSE.equals(newCity)) parts.add("曾出现城市");
        return parts.isEmpty() ? null : String.join("、", parts);
    }

    private static boolean isCity(String city) {
        return city != null && !city.isBlank();
    }
}
