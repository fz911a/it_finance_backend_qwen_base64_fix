package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/export")
@CrossOrigin
public class ExportController {
    private final AuthService authService;

    public ExportController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/excel")
    public ApiResponse<Map<String, String>> exportExcel(@RequestHeader("Authorization") String token, @RequestBody Map<String, Object> body) {
        try {
            authService.getUserByToken(token);
            // 模拟导出Excel功能
            Map<String, String> result = new HashMap<>();
            result.put("url", "https://example.com/export/excel.xlsx");
            result.put("message", "Excel导出成功");
            return ApiResponse.ok(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/pdf")
    public ApiResponse<Map<String, String>> exportPdf(@RequestHeader("Authorization") String token, @RequestBody Map<String, Object> body) {
        try {
            authService.getUserByToken(token);
            // 模拟导出PDF功能
            Map<String, String> result = new HashMap<>();
            result.put("url", "https://example.com/export/report.pdf");
            result.put("message", "PDF导出成功");
            return ApiResponse.ok(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/csv")
    public ApiResponse<Map<String, String>> exportCsv(@RequestHeader("Authorization") String token, @RequestBody Map<String, Object> body) {
        try {
            authService.getUserByToken(token);
            // 模拟导出CSV功能
            Map<String, String> result = new HashMap<>();
            result.put("url", "https://example.com/export/data.csv");
            result.put("message", "CSV导出成功");
            return ApiResponse.ok(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
