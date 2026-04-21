package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.service.RedisService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/redis")
@Validated
public class RedisController {
    private final RedisService redisService;

    public RedisController(RedisService redisService) {
        this.redisService = redisService;
    }

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("ping", redisService.ping());
        return ApiResponse.ok(data);
    }

    @PostMapping("/set")
    public ApiResponse<Map<String, Object>> set(
            @RequestParam @NotBlank String key,
            @RequestParam @NotBlank String value,
            @RequestParam(required = false) @Positive Long ttlSeconds) {
        if (ttlSeconds == null) {
            redisService.set(key, value);
        } else {
            redisService.set(key, value, ttlSeconds);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("value", value);
        data.put("ttlSeconds", ttlSeconds);
        return ApiResponse.ok("写入成功", data);
    }

    @GetMapping("/get")
    public ApiResponse<Map<String, Object>> get(@RequestParam @NotBlank String key) {
        String value = redisService.get(key);
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("value", value);
        data.put("exists", value != null);
        return ApiResponse.ok(data);
    }

    @DeleteMapping("/delete")
    public ApiResponse<Map<String, Object>> delete(@RequestParam @NotBlank String key) {
        boolean deleted = redisService.delete(key);
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("deleted", deleted);
        return ApiResponse.ok(data);
    }

    @PostMapping("/expire")
    public ApiResponse<Map<String, Object>> expire(
            @RequestParam @NotBlank String key,
            @RequestParam @Positive long ttlSeconds) {
        boolean expired = redisService.expire(key, ttlSeconds);
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("ttlSeconds", ttlSeconds);
        data.put("updated", expired);
        return ApiResponse.ok(data);
    }
}
