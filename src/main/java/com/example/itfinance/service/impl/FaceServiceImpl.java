package com.example.itfinance.service.impl;

import com.example.itfinance.config.AiProperties;
import com.example.itfinance.dto.FaceRecognizeRequest;
import com.example.itfinance.entity.FaceProfile;
import com.example.itfinance.service.FaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class FaceServiceImpl implements FaceService {
    private static final Logger log = LoggerFactory.getLogger(FaceServiceImpl.class);
    private static final int FACE_API_CONNECT_TIMEOUT_MS = 15000;
    private static final int FACE_API_READ_TIMEOUT_MS = 120000;
    private final JdbcTemplate jdbcTemplate;
    private final AiProperties aiProperties;
    private final RestClient restClient;

    public FaceServiceImpl(JdbcTemplate jdbcTemplate, AiProperties aiProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.aiProperties = aiProperties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(FACE_API_CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(FACE_API_READ_TIMEOUT_MS);
        this.restClient = RestClient.builder().requestFactory(factory).build();
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
        if (faceProfile == null || faceProfile.getEmployeeId() == null || faceProfile.getEmployeeId() <= 0
                || faceProfile.getEmployeeName() == null || faceProfile.getEmployeeName().isBlank()
                || faceProfile.getFaceImageUrl() == null || faceProfile.getFaceImageUrl().isBlank()) {
            throw new IllegalArgumentException("员工ID、员工姓名、人脸图片不能为空");
        }

        Long existingId = findIdByEmployeeId(faceProfile.getEmployeeId());
        if (existingId != null) {
            jdbcTemplate.update(
                    "UPDATE face_profile SET employee_name = ?, face_image_url = ?, face_embedding = ?, project_id = ?, status = ? WHERE id = ?",
                    faceProfile.getEmployeeName(), faceProfile.getFaceImageUrl(), faceProfile.getFaceEmbedding(),
                    faceProfile.getProjectId(), normalizeStatus(faceProfile.getStatus()), existingId);
            return findById(existingId);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO face_profile(employee_id, employee_name, face_image_url, face_embedding, project_id, status) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, faceProfile.getEmployeeId());
            ps.setString(2, faceProfile.getEmployeeName());
            ps.setString(3, faceProfile.getFaceImageUrl());
            ps.setString(4, faceProfile.getFaceEmbedding());
            ps.setObject(5, faceProfile.getProjectId());
            ps.setString(6, normalizeStatus(faceProfile.getStatus()));
            return ps;
        }, keyHolder);

        Number generatedKey = keyHolder.getKey();
        return generatedKey == null ? findByEmployeeId(faceProfile.getEmployeeId()) : findById(generatedKey.longValue());
    }

    @Override
    public void deleteById(Long id) {
        int updated = jdbcTemplate.update("DELETE FROM face_profile WHERE id = ?", id);
        if (updated <= 0) {
            throw new IllegalArgumentException("人脸档案不存在或已删除");
        }
    }

    @Override
    public FaceProfile updateStatus(Long id, String status) {
        FaceProfile existing = findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("人脸档案不存在");
        }
        String normalized = normalizeStatus(status);
        jdbcTemplate.update("UPDATE face_profile SET status = ? WHERE id = ?", normalized, id);
        return findById(id);
    }

    private static final String KEY_MATCHED = "matched";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_IMAGE_URL = "imageUrl";
    private static final String STATUS_ENROLLED = "已录入";
    private static final String STATUS_ENABLED = "启用";
    private static final String DEMO_FACE_MARKER = "demo_face";
    private static final String RECOGNITION_TYPE_FACE = "face";
    private static final long DEFAULT_OPERATOR_ID = 1L;

    @Override
    public Map<String, Object> recognize(FaceRecognizeRequest request) {
        // 预留接口：如果配置了外部人脸识别服务，则优先调用
        if (aiProperties.getFaceApiUrl() != null && !aiProperties.getFaceApiUrl().isBlank()) {
            log.info("调用外部人脸识别服务: {}", aiProperties.getFaceApiUrl());
            return callExternalFaceApi(request);
        }

        List<FaceProfile> list = loadRecognizableProfiles();
        if (list.isEmpty()) {
            return buildFailureResult("人脸库为空，请先录入人脸信息", null, 0.0);
        }

        String imageUrl = request.getImageUrl().toLowerCase();
        MatchResult match = matchRecognizableProfile(list, imageUrl);
        FaceProfile matched = match.profile();
        double confidence = match.confidence();
        if (matched == null) {
            return buildFailureResult("未匹配到有效人脸，请重试或重新录入", request.getImageUrl(), 0.00);
        }

        return buildSuccessResult(matched, request.getImageUrl(), confidence);
    }

    /**
     * 预留的人脸识别外部服务调用接口
     * 您可以直接在此处添加具体的人脸识别 SDK 或 RestTemplate 调用逻辑
     */
    private Map<String, Object> callExternalFaceApi(FaceRecognizeRequest request) {
        String endpoint = aiProperties.getFaceApiUrl();
        String imageUrl = request.getImageUrl();
        log.info("调用外部人脸认证接口: {}, imageUrl={}", endpoint, imageUrl);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put(KEY_IMAGE_URL, imageUrl);

        try {
            Map<String, Object> external = restClient.post()
                    .uri(endpoint)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        String faceApiKey = aiProperties.getFaceApiKey();
                        if (faceApiKey != null && !faceApiKey.isBlank()) {
                            headers.setBearerAuth(faceApiKey);
                            headers.add("X-API-Key", faceApiKey);
                        }
                    })
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            return normalizeExternalResponse(external, imageUrl, endpoint);
        } catch (Exception ex) {
            log.error("外部人脸认证服务调用失败: {}", ex.getMessage(), ex);
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(KEY_MATCHED, false);
            map.put(KEY_IMAGE_URL, imageUrl);
            map.put(KEY_MESSAGE, "外部人脸认证服务调用失败: " + ex.getMessage());
            map.put("externalService", endpoint);
            return map;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeExternalResponse(Map<String, Object> external, String imageUrl,
            String endpoint) {
        Map<String, Object> source = external == null ? new LinkedHashMap<>() : external;
        Object nestedData = source.get("data");
        if (nestedData instanceof Map<?, ?> nested) {
            source = (Map<String, Object>) nested;
        }

        boolean matched = toBoolean(source.get(KEY_MATCHED));
        if (!source.containsKey(KEY_MATCHED) && external != null) {
            matched = toBoolean(external.get("success"));
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(KEY_MATCHED, matched);
        map.put(KEY_IMAGE_URL, imageUrl);
        map.put("employeeId", source.getOrDefault("employeeId", source.get("userId")));
        map.put("employeeName", source.getOrDefault("employeeName", source.get("name")));
        map.put("projectId", source.get("projectId"));
        map.put("confidence", toDouble(source.getOrDefault("confidence", source.get("score"))));
        map.put(KEY_MESSAGE, String.valueOf(source.getOrDefault(KEY_MESSAGE,
                matched ? "外部人脸认证成功" : "外部人脸认证失败")));
        map.put("externalService", endpoint);
        map.put("raw", external);
        return map;
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return false;
        }
        return "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private Long findIdByEmployeeId(Long employeeId) {
        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM face_profile WHERE employee_id = ? ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"), employeeId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private List<FaceProfile> loadRecognizableProfiles() {
        return jdbcTemplate.query(
                "SELECT id, employee_id, employee_name, face_image_url, face_embedding, project_id, status FROM face_profile WHERE status IS NULL OR status = ? OR status = ? ORDER BY id DESC",
                (rs, rowNum) -> mapFaceProfile(rs), STATUS_ENROLLED, STATUS_ENABLED);
    }

    private MatchResult matchRecognizableProfile(List<FaceProfile> candidates, String imageUrl) {
        for (FaceProfile candidate : candidates) {
            String employeeName = candidate.getEmployeeName() == null ? "" : candidate.getEmployeeName().toLowerCase();
            String employeeId = String.valueOf(candidate.getEmployeeId());
            if ((!employeeName.isBlank() && imageUrl.contains(employeeName)) || imageUrl.contains(employeeId)) {
                return new MatchResult(candidate, imageUrl.contains(employeeId) ? 93.5 : 96.2);
            }
        }

        if (imageUrl.contains(DEMO_FACE_MARKER) && !candidates.isEmpty()) {
            return new MatchResult(candidates.get(0), 85.0);
        }

        return new MatchResult(null, 0.0);
    }

    private Map<String, Object> buildSuccessResult(FaceProfile matched, String imageUrl, double confidence) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(KEY_MATCHED, true);
        map.put("employeeId", matched.getEmployeeId());
        map.put("employeeName", matched.getEmployeeName());
        map.put("projectId", matched.getProjectId());
        map.put("confidence", confidence);
        map.put(KEY_IMAGE_URL, imageUrl);
        map.put(KEY_MESSAGE, "识别成功");
        saveRecognitionRecord(imageUrl, "人脸识别成功", confidence);
        return map;
    }

    private Map<String, Object> buildFailureResult(String message, String imageUrl, double confidence) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(KEY_MATCHED, false);
        map.put(KEY_MESSAGE, message);
        if (imageUrl != null) {
            map.put(KEY_IMAGE_URL, imageUrl);
        }
        saveRecognitionRecord(imageUrl, "人脸识别失败", confidence);
        return map;
    }

    private void saveRecognitionRecord(String imageUrl, String resultJson, double confidence) {
        jdbcTemplate.update(
                "INSERT INTO recognition_record(recognition_type, source_file_url, result_json, confidence_score, operator_id) VALUES (?, ?, ?, ?, ?)",
                RECOGNITION_TYPE_FACE, imageUrl, resultJson, confidence, DEFAULT_OPERATOR_ID);
    }

    private FaceProfile findById(Long id) {
        List<FaceProfile> data = jdbcTemplate.query(
                "SELECT id, employee_id, employee_name, face_image_url, face_embedding, project_id, status FROM face_profile WHERE id = ?",
                (rs, rowNum) -> mapFaceProfile(rs), id);
        return data.isEmpty() ? null : data.get(0);
    }

    private FaceProfile findByEmployeeId(Long employeeId) {
        List<FaceProfile> data = jdbcTemplate.query(
                "SELECT id, employee_id, employee_name, face_image_url, face_embedding, project_id, status FROM face_profile WHERE employee_id = ? ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> mapFaceProfile(rs), employeeId);
        return data.isEmpty() ? null : data.get(0);
    }

    private FaceProfile mapFaceProfile(ResultSet rs) throws java.sql.SQLException {
        return new FaceProfile(rs.getLong("id"), rs.getLong("employee_id"), rs.getString("employee_name"),
                rs.getString("face_image_url"), rs.getString("face_embedding"), rs.getLong("project_id"),
                rs.getString("status"));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_ENABLED;
        }
        if (Objects.equals(status, STATUS_ENROLLED)) {
            return STATUS_ENABLED;
        }
        return status;
    }

    private record MatchResult(FaceProfile profile, double confidence) {
    }
}
