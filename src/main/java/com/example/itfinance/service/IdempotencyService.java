package com.example.itfinance.service;

import com.example.itfinance.config.IdempotencyProperties;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {
    private final Map<String, Long> recentKeys = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public IdempotencyService(IdempotencyProperties properties) {
        this.ttlMillis = Math.max(10, properties.getTtlSeconds()) * 1000;
    }

    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        cleanup(now);
        Long previous = recentKeys.putIfAbsent(key, now);
        if (previous == null) {
            return true;
        }
        if (now - previous > ttlMillis) {
            recentKeys.put(key, now);
            return true;
        }
        return false;
    }

    private void cleanup(long now) {
        recentKeys.entrySet().removeIf(entry -> now - entry.getValue() > ttlMillis);
    }
}
