package com.example.itfinance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void log(String module, String operation, String content) {
        try {
            Long userId = currentUserId();
            jdbcTemplate.update(
                    "INSERT INTO operation_log(user_id, module_name, operation_type, content, ip) VALUES (?, ?, ?, ?, ?)",
                    userId,
                    module,
                    operation,
                    content,
                    "-");
        } catch (Exception ignored) {
            // Keep business path unaffected when audit logging fails.
        }
    }

    public void logLogin(Long userId, String username, String realName, String role, String loginType,
            String status, String detail) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("username", username);
            payload.put("realName", realName);
            payload.put("role", role);
            payload.put("loginType", loginType);
            payload.put("status", status);
            payload.put("detail", detail);
            jdbcTemplate.update(
                    "INSERT INTO operation_log(user_id, module_name, operation_type, content, ip) VALUES (?, ?, ?, ?, ?)",
                    userId == null ? 0L : userId,
                    "AUTH",
                    loginType,
                    objectMapper.writeValueAsString(payload),
                    "-");
        } catch (Exception ignored) {
            // Keep login path unaffected when audit logging fails.
        }
    }

    public List<Map<String, Object>> listLoginLogs() {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "SELECT id, user_id, module_name, operation_type, content, ip, DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') create_time FROM operation_log WHERE module_name = 'AUTH' ORDER BY id DESC LIMIT 100",
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("userId", rs.getLong("user_id"));
                    row.put("moduleName", rs.getString("module_name"));
                    row.put("operationType", rs.getString("operation_type"));
                    row.put("content", rs.getString("content"));
                    row.put("ip", rs.getString("ip"));
                    row.put("createTime", rs.getString("create_time"));
                    return row;
                });

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(normalizeLoginRow(row));
        }
        return result;
    }

    public void clearLoginLogs() {
        jdbcTemplate.update("DELETE FROM operation_log WHERE module_name = 'AUTH'");
    }

    private Map<String, Object> normalizeLoginRow(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>(row);
        String content = row.get("content") == null ? "" : String.valueOf(row.get("content"));
        try {
            JsonNode node = objectMapper.readTree(content);
            result.put("username", text(node, "username"));
            result.put("realName", text(node, "realName"));
            result.put("role", text(node, "role"));
            String loginType = text(node, "loginType");
            result.put("typeLabel", "FACE".equalsIgnoreCase(loginType) ? "人脸登录" : "账号登录");
            String status = text(node, "status");
            result.put("status", "FAILED".equalsIgnoreCase(status) ? "失败" : "成功");
            result.put("detail", text(node, "detail"));
        } catch (Exception ex) {
            result.put("typeLabel", "账号登录");
            result.put("status", "成功");
            result.put("detail", content);
            result.put("username", "-");
            result.put("realName", "-");
            result.put("role", "-");
        }
        return result;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return 0L;
        }
        String username = authentication.getName();
        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM sys_user WHERE username = ? LIMIT 1",
                (rs, rowNum) -> rs.getLong(1),
                username);
        return ids.isEmpty() ? 0L : ids.get(0);
    }
}
