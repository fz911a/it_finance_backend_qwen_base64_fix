package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ocr")
@CrossOrigin
public class OcrController {
    private final JdbcTemplate jdbcTemplate;

    public OcrController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> history() {
        String sql = "SELECT id, recognition_type, source_file_url, result_json, confidence_score, DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') create_time FROM recognition_record WHERE recognition_type IN ('invoice','receipt') ORDER BY id DESC";
        List<Map<String, Object>> data = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rs.getLong("id"));
            m.put("type", rs.getString("recognition_type"));
            m.put("sourceFileUrl", rs.getString("source_file_url"));
            m.put("result", rs.getString("result_json"));
            m.put("confidence", rs.getBigDecimal("confidence_score"));
            m.put("createTime", rs.getString("create_time"));
            return m;
        });
        return ApiResponse.ok(data);
    }

    @GetMapping("/history/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        String sql = "SELECT id, recognition_type, source_file_url, result_json, confidence_score, DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') create_time FROM recognition_record WHERE id = ? LIMIT 1";
        List<Map<String, Object>> data = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rs.getLong("id"));
            m.put("type", rs.getString("recognition_type"));
            m.put("sourceFileUrl", rs.getString("source_file_url"));
            m.put("result", rs.getString("result_json"));
            m.put("confidence", rs.getBigDecimal("confidence_score"));
            m.put("createTime", rs.getString("create_time"));
            return m;
        }, id);
        if (data.isEmpty())
            return ApiResponse.fail("记录不存在");
        return ApiResponse.ok(data.get(0));
    }

    @DeleteMapping("/history/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM recognition_record WHERE id = ?", id);
        return ApiResponse.ok("删除成功", "ok");
    }
}
