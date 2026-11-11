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
        InetAddress address = IpLiteral.parse(value);
        if (address == null) {
            return "未知";
        }
        if (address.isLoopbackAddress()) {
            return "本地";
        }
        byte[] bytes = address.getAddress();
        if (address.isSiteLocalAddress() || address.isLinkLocalAddress()
                || (bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc)) {
            return "内网";
        }
        if (address.isAnyLocalAddress() || address.isMulticastAddress()) {
            return "非公网地址";
        }
        if (bytes.length != 4) {
            return "未知（IPv6 暂未覆盖）";
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
            return "非公网地址";
        }
        synchronized (searcher) {
            try {
                return format(searcher.search(address.getHostAddress()));
            } catch (Exception exception) {
                // Neither the raw IP nor resolver data is written to application logs.
                log.debug("Offline login IP location lookup failed");
                return "未知";
            }
        }
    }

    static String format(String raw) {
        if (raw == null || raw.isBlank()) {
            return "未知";
        }
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 5) {
            return "未知";
        }
        Set<String> labels = new LinkedHashSet<>();
        for (int index : new int[] {0, 2, 3}) {
            String label = parts[index].trim();
            if (!label.isEmpty() && !"0".equals(label) && !"内网IP".equals(label)) {
                labels.add(label);
            }
        }
        return labels.isEmpty() ? "未知" : String.join(" / ", labels);
    }
}
