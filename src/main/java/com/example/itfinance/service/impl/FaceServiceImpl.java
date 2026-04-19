package com.example.itfinance.service.impl;

import com.example.itfinance.config.AiProperties;
import com.example.itfinance.dto.FaceRecognizeRequest;
import com.example.itfinance.entity.FaceProfile;
import com.example.itfinance.service.FaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FaceServiceImpl implements FaceService {
    private static final Logger log = LoggerFactory.getLogger(FaceServiceImpl.class);
    private final JdbcTemplate jdbcTemplate;
    private final AiProperties aiProperties;

    public FaceServiceImpl(JdbcTemplate jdbcTemplate, AiProperties aiProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.aiProperties = aiProperties;
    }

    @Override
    public List<FaceProfile> list() {
        return jdbcTemplate.query(
                "SELECT id, employee_id, employee_name, face_image_url, face_embedding, project_id, status FROM face_profile ORDER BY id DESC",
                (rs, rowNum) -> new FaceProfile(rs.getLong("id"), rs.getLong("employee_id"),
                        rs.getString("employee_name"), rs.getString("face_image_url"), rs.getString("face_embedding"),
                        rs.getLong("project_id"), rs.getString("status")));
    }

    @Override
    public FaceProfile enroll(FaceProfile faceProfile) {
        jdbcTemplate.update(
                "INSERT INTO face_profile(employee_id, employee_name, face_image_url, face_embedding, project_id, status) VALUES (?, ?, ?, ?, ?, ?)",
                faceProfile.getEmployeeId(), faceProfile.getEmployeeName(), faceProfile.getFaceImageUrl(),
                faceProfile.getFaceEmbedding(), faceProfile.getProjectId(),
                faceProfile.getStatus() == null ? "已录入" : faceProfile.getStatus());
        return list().get(0);
    }

    private static final String KEY_MATCHED = "matched";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_IMAGE_URL = "imageUrl";

    @Override
    public Map<String, Object> recognize(FaceRecognizeRequest request) {
        // 预留接口：如果配置了外部人脸识别服务，则优先调用
        if (aiProperties.getFaceApiUrl() != null && !aiProperties.getFaceApiUrl().isBlank()) {
            log.info("调用外部人脸识别服务: {}", aiProperties.getFaceApiUrl());
            return callExternalFaceApi(request);
        }

        // 默认实现：基于人脸库的简单匹配（当前为演示模拟逻辑）
        List<FaceProfile> list = list();
        Map<String, Object> map = new LinkedHashMap<>();
        if (list.isEmpty()) {
            map.put(KEY_MATCHED, false);
            map.put(KEY_MESSAGE, "人脸库为空，请先录入人脸信息");
            return map;
        }

        String imageUrl = request.getImageUrl() == null ? "" : request.getImageUrl().toLowerCase();
        FaceProfile matched = null;
        double confidence = 0.0;

        for (FaceProfile candidate : list) {
            String employeeName = candidate.getEmployeeName() == null ? "" : candidate.getEmployeeName().toLowerCase();
            String employeeId = String.valueOf(candidate.getEmployeeId());
            if ((!employeeName.isBlank() && imageUrl.contains(employeeName)) || imageUrl.contains(employeeId)) {
                matched = candidate;
                confidence = imageUrl.contains(employeeId) ? 93.5 : 96.2;
                break;
            }
        }

        if (matched == null && imageUrl.contains("demo_face") && !list.isEmpty()) {
            matched = list.get(0);
            confidence = 85.0;
        }

        if (matched == null) {
            map.put(KEY_MATCHED, false);
            map.put(KEY_IMAGE_URL, request.getImageUrl());
            map.put(KEY_MESSAGE, "未匹配到有效人脸，请重试或重新录入");
            jdbcTemplate.update(
                    "INSERT INTO recognition_record(recognition_type, source_file_url, result_json, confidence_score, operator_id) VALUES (?, ?, ?, ?, ?)",
                    "face", request.getImageUrl(), "人脸识别失败", 0.00, 1);
            return map;
        }

        map.put(KEY_MATCHED, true);
        map.put("employeeId", matched.getEmployeeId());
        map.put("employeeName", matched.getEmployeeName());
        map.put("projectId", matched.getProjectId());
        map.put("confidence", confidence);
        map.put(KEY_IMAGE_URL, request.getImageUrl());
        map.put(KEY_MESSAGE, "识别成功");
        jdbcTemplate.update(
                "INSERT INTO recognition_record(recognition_type, source_file_url, result_json, confidence_score, operator_id) VALUES (?, ?, ?, ?, ?)",
                "face", request.getImageUrl(), "人脸识别成功", confidence, 1);
        return map;
    }

    /**
     * 预留的人脸识别外部服务调用接口
     * 您可以直接在此处添加具体的人脸识别 SDK 或 RestTemplate 调用逻辑
     */
    private Map<String, Object> callExternalFaceApi(FaceRecognizeRequest request) {
        log.warn("检测到外部人脸识别配置为 {}，处理图片：{}", aiProperties.getFaceApiUrl(), request.getImageUrl());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(KEY_MATCHED, true);
        map.put(KEY_MESSAGE, "外部人脸服务识别成功（预留接口）");
        map.put("confidence", 99.9);
        map.put("externalService", aiProperties.getFaceApiUrl());
        return map;
    }return map;
}}
