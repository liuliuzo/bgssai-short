package com.bgssai.shortai.login.service;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Records last-seen on the login row at most once every ten minutes per session. */
@Component
public class LoginSessionSeen {

    private static final long GAP_MS = 10L * 60L * 1000L;
    private final ConcurrentHashMap<String, Long> recent = new ConcurrentHashMap<>();

    @Autowired
    private LoginAuditService loginAuditService;

    public void touch(String sessionId) {
        if (sessionId == null || sessionId.isBlank() || loginAuditService == null) return;
        long now = System.currentTimeMillis();
        Long previous = recent.get(sessionId);
        if (previous != null && now - previous < GAP_MS) return;
        recent.put(sessionId, now);
        if (recent.size() > 10000) {
            long cutoff = now - GAP_MS;
            recent.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        }
        loginAuditService.touchLastSeen(sessionId, new Date(now));
    }
}
