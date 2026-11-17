package com.bgssai.shortai.login.otp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

/** 登录验证码内存存储。key=channel:target，TTL 5 分钟，原子单次消费并限制连续错误尝试。 */
@Service
public class InMemoryOtpStore {

    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final int MAX_ATTEMPTS = 5;
    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    public void save(String channel, String target, String code) {
        if (channel == null || target == null || code == null || target.isBlank() || code.isBlank()) {
            return;
        }
        store.put(key(channel, target), new Entry(code, System.currentTimeMillis() + TTL_MS, 0));
    }

    public boolean consume(String channel, String target, String code) {
        if (channel == null || target == null || code == null || target.isBlank() || code.isBlank()) {
            return false;
        }
        String k = key(channel, target);
        long now = System.currentTimeMillis();
        AtomicBoolean matched = new AtomicBoolean(false);
        store.computeIfPresent(k, (ignored, entry) -> {
            if (entry.expireAt < now) {
                return null;
            }
            if (MessageDigest.isEqual(
                    entry.code.getBytes(StandardCharsets.UTF_8),
                    code.getBytes(StandardCharsets.UTF_8))) {
                matched.set(true);
                return null;
            }
            int nextFailures = entry.failures + 1;
            if (nextFailures >= MAX_ATTEMPTS) {
                return null;
            }
            return new Entry(entry.code, entry.expireAt, nextFailures);
        });
        return matched.get();
    }

    public void delete(String channel, String target) {
        store.remove(key(channel, target));
    }

    private static String key(String channel, String target) {
        return channel + ":" + target;
    }

    private static final class Entry {
        private final String code;
        private final long expireAt;
        private final int failures;

        private Entry(String code, long expireAt, int failures) {
            this.code = code;
            this.expireAt = expireAt;
            this.failures = failures;
        }
    }
}
