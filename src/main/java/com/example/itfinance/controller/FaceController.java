package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.dto.FaceRecognizeRequest;
import com.example.itfinance.entity.FaceProfile;
import com.example.itfinance.service.FaceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/face")
@CrossOrigin
public class FaceController {
    private final FaceService faceService;
    private final JdbcTemplate jdbcTemplate;
    public FaceController(FaceService faceService, JdbcTemplate jdbcTemplate) { this.faceService = faceService; this.jdbcTemplate = jdbcTemplate; }
    @GetMapping("/list")
    public ApiResponse<List<FaceProfile>> list() { return ApiResponse.ok(faceService.list()); }
    @GetMapping("/logs")
    public ApiResponse<List<Map<String,Object>>> logs() {
        String sql = "SELECT id, recognition_type, source_file_url, result_json, confidence_score, DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') create_time FROM recognition_record WHERE recognition_type = 'face' ORDER BY id DESC";
        List<Map<String,Object>> data = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", rs.getLong("id")); m.put("type", rs.getString("recognition_type")); m.put("sourceFileUrl", rs.getString("source_file_url")); m.put("result", rs.getString("result_json")); m.put("confidence", rs.getBigDecimal("confidence_score")); m.put("createTime", rs.getString("create_time")); return m; });
        return ApiResponse.ok(data);
    }
    @DeleteMapping("/logs/{id}")
    public ApiResponse<String> deleteLog(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM recognition_record WHERE id = ? AND recognition_type = ?", id, "face");
        return ApiResponse.ok("删除成功", "ok");
    }

    @PostMapping("/enroll")
    public ApiResponse<FaceProfile> enroll(@RequestBody FaceProfile faceProfile) { return ApiResponse.ok("录入成功", faceService.enroll(faceProfile)); }
    @PostMapping("/recognize")
    public ApiResponse<Map<String, Object>> recognize(@RequestBody FaceRecognizeRequest request) { return ApiResponse.ok(faceService.recognize(request)); }
}
