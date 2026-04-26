package com.ecommerce.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String token, long expireAtMillis) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        cleanup();
        blacklist.put(token.trim(), expireAtMillis);
    }

    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        Long expiresAt = blacklist.get(token.trim());
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt <= System.currentTimeMillis()) {
            blacklist.remove(token.trim());
            return false;
        }
        return true;
    }

    private void cleanup() {
        long now = Instant.now().toEpochMilli();
        Iterator<Map.Entry<String, Long>> iterator = blacklist.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= now) {
                iterator.remove();
            }
        }
    }
}
