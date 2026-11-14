package com.bgssai.shortai.loginlog.location;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.Set;

import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Offline, approximate region lookup using the reviewed Note IPv4 dataset. */
@Component
public class LoginIpLocationResolver {
    private static final Logger log = LoggerFactory.getLogger(LoginIpLocationResolver.class);
    private final Searcher searcher;

    public LoginIpLocationResolver() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/ip/ip2region.xdb")) {
            if (input == null) {
                throw new IllegalStateException("Bundled IP region database is missing");
            }
            searcher = Searcher.newWithBuffer(input.readAllBytes());
        }
    }

    public String resolve(String value) {
        return lookup(value).getDisplay();
    }

    public IpGeo lookup(String value) {
        InetAddress address = IpLiteral.parse(value);
        if (address == null) {
            return classified("未知");
        }
        if (address.isLoopbackAddress()) {
            return classified("本地");
        }
        byte[] bytes = address.getAddress();
        if (address.isSiteLocalAddress() || address.isLinkLocalAddress()
                || (bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc)) {
            return classified("内网");
        }
        if (address.isAnyLocalAddress() || address.isMulticastAddress()) {
            return classified("非公网地址");
        }
        if (bytes.length != 4) {
            return classified("未知（IPv6 暂未覆盖）");
        }
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        int third = bytes[2] & 0xff;
        if (first == 0 || first >= 240
                || (first == 100 && second >= 64 && second <= 127)
                || (first == 192 && second == 0 && (third == 0 || third == 2))
                || (first == 198 && (second == 18 || second == 19))
                || (first == 198 && second == 51 && third == 100)
                || (first == 203 && second == 0 && third == 113)) {
            return classified("非公网地址");
        }
        synchronized (searcher) {
            try {
                return parts(searcher.search(address.getHostAddress()));
            } catch (Exception exception) {
                // Neither the raw IP nor resolver data is written to application logs.
                log.debug("Offline login IP location lookup failed");
                return classified("未知");
            }
        }
    }

    private static IpGeo classified(String display) {
        return new IpGeo(display, null, null, null, null);
    }

    static String format(String raw) {
        return parts(raw).getDisplay();
    }

    static IpGeo parts(String raw) {
        if (raw == null || raw.isBlank()) {
            return new IpGeo("未知", null, null, null, null);
        }
        String[] pieces = raw.split("\\|", -1);
        if (pieces.length != 5) {
            return new IpGeo("未知", null, null, null, null);
        }
        String country = place(pieces[0]);
        String province = place(pieces[2]);
        String city = place(pieces[3]);
        String isp = place(pieces[4]);
        Set<String> labels = new LinkedHashSet<>();
        if (country != null) labels.add(country);
        if (province != null) labels.add(province);
        if (city != null) labels.add(city);
        String display = labels.isEmpty() ? "未知" : String.join(" / ", labels);
        return new IpGeo(display, country, province, city, isp);
    }

    private static String place(String value) {
        if (value == null) return null;
        String label = value.trim();
        if (label.isEmpty() || "0".equals(label) || "内网IP".equals(label)) return null;
        return label;
    }
}
