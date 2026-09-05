package com.bgssai.shortai.login.otp;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/** 登录验证码内存存储。key=channel:target，TTL 5 分钟，消费后删除。 */
@Service
public class InMemoryOtpStore {

    private static final long TTL_MS = 5 * 60 * 1000L;
    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    public void save(String channel, String target, String code) {
        store.put(key(channel, target), new Entry(code, System.currentTimeMillis() + TTL_MS));
    }

    public boolean consume(String channel, String target, String code) {
        if (target == null || code == null) {
            return false;
        }
        String k = key(channel, target);
        Entry entry = store.get(k);
        if (entry == null || entry.expireAt < System.currentTimeMillis() || !entry.code.equals(code)) {
            return false;
        }
        store.remove(k);
        return true;
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

        private Entry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
