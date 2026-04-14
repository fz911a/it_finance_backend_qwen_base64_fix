package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@CrossOrigin
public class SystemController {
    private final JdbcTemplate jdbcTemplate;

    public SystemController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/db-status")
    public ApiResponse<Map<String, Object>> dbStatus() {
        Map<String, Object> map = new LinkedHashMap<>();
        try {
            map.put("database", jdbcTemplate.queryForObject("SELECT DATABASE()", String.class));
            map.put("invoiceCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoice", Integer.class));
            map.put("paymentCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payment", Integer.class));
            map.put("faceCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM face_profile", Integer.class));
            map.put("expenseCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM expense_record", Integer.class));
        } catch (Exception ex) {
            map.put("database", "unavailable");
            map.put("invoiceCount", 0);
            map.put("paymentCount", 0);
            map.put("faceCount", 0);
            map.put("expenseCount", 0);
            map.put("message", "数据库暂不可用，请检查连接配置");
        }
        return ApiResponse.ok(map);
    }
}
