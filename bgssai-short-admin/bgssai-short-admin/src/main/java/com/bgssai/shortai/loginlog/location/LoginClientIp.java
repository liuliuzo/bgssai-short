package com.bgssai.shortai.loginlog.location;

import java.net.InetAddress;

import jakarta.servlet.http.HttpServletRequest;

/** Login audit IP collection for the existing same-host nginx deployment. */
public final class LoginClientIp {
    private LoginClientIp() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        InetAddress peer = IpLiteral.parse(request.getRemoteAddr());
        if (peer == null) {
            return null;
        }
        // Authority: workflows/jenkins/install/provision-nginx-vhosts.py appends
        // $remote_addr to XFF and proxies to the same host. Never trust remote peers.
        if (peer.isLoopbackAddress()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && forwarded.length() <= 4096) {
                String last = forwarded.substring(forwarded.lastIndexOf(',') + 1).trim();
                InetAddress client = IpLiteral.parse(last);
                if (client != null) {
                    return client.getHostAddress();
                }
            }
        }
        return peer.getHostAddress();
    }
}
