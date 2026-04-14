package com.example.itfinance.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {
    private final JdbcTemplate jdbcTemplate;

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
