package com.example.itfinance.service;

public interface RedisService {
    void set(String key, String value);

    void set(String key, String value, long ttlSeconds);

    String get(String key);

    boolean delete(String key);

    boolean expire(String key, long ttlSeconds);

    String ping();
}
