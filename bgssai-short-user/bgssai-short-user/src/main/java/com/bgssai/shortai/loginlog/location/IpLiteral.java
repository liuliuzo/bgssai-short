package com.bgssai.shortai.loginlog.location;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Numeric IP literals only: never resolve a hostname or accept a forwarded list. */
public final class IpLiteral {
    private IpLiteral() {
    }

    public static InetAddress parse(String value) {
        if (value == null || value.isBlank() || value.length() > 45) {
            return null;
        }
        String ip = value.trim();
        if (ip.indexOf(':') >= 0) {
            if (!ip.matches("[0-9a-fA-F:.]+")) {
                return null;
            }
        } else {
            String[] parts = ip.split("\\.", -1);
            if (parts.length != 4) {
                return null;
            }
            for (String part : parts) {
                if (!part.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(part) > 255) {
                    return null;
                }
            }
        }
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException exception) {
            return null;
        }
    }
}
