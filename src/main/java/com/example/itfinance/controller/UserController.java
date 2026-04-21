package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.dto.AdminCreateUserWithFaceRequest;
import com.example.itfinance.enums.RoleType;
import com.example.itfinance.entity.User;
import com.example.itfinance.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {
    private final AuthService authService;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public UserController(AuthService authService, JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public ApiResponse<User> getProfile(@RequestHeader("Authorization") String token) {
        try {
            User user = authService.getUserByToken(token);
            return ApiResponse.ok(user);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PutMapping("/profile")
    public ApiResponse<User> updateProfile(@RequestHeader("Authorization") String token, @RequestBody User user) {
        try {
            User updatedUser = authService.updateUserProfile(token, user);
            return ApiResponse.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ApiResponse<String> changePassword(@RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {
        try {
            String oldPassword = body.get("oldPassword");
            String newPassword = body.get("newPassword");
            authService.changePassword(token, oldPassword, newPassword);
            return ApiResponse.ok("Password changed successfully");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/upload-avatar")
    public ApiResponse<String> uploadAvatar(@RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {
        try {
            String avatar = body.get("avatar");
            authService.updateAvatar(token, avatar);
            return ApiResponse.ok("Avatar uploaded successfully");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/bind-phone")
    public ApiResponse<String> bindPhone(@RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {
        try {
            String phone = body.get("phone");
            authService.bindPhone(token, phone);
            return ApiResponse.ok("Phone bound successfully");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @Transactional
    @PostMapping("/admin/create-with-face")
    public ApiResponse<Map<String, Object>> adminCreateUserWithFace(
            @Valid @RequestBody AdminCreateUserWithFaceRequest request) {
        try {
            String normalizedRole = request.getRole() == null ? "" : request.getRole().trim().toUpperCase();
            RoleType.valueOf(normalizedRole);

            Integer duplicate = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_user WHERE username = ?",
                    Integer.class,
                    request.getUsername());
            if (duplicate != null && duplicate > 0) {
                return ApiResponse.fail("用户名已存在");
            }

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO sys_user(username, password, real_name, role, status) VALUES (?, ?, ?, ?, 1)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, request.getUsername());
                ps.setString(2, passwordEncoder.encode(request.getPassword()));
                ps.setString(3, request.getRealName());
                ps.setString(4, normalizedRole);
                return ps;
            }, keyHolder);

            Number newUserId = keyHolder.getKey();
            if (newUserId == null) {
                throw new IllegalStateException("创建用户失败");
            }

            jdbcTemplate.update(
                    "INSERT INTO face_profile(employee_id, employee_name, face_image_url, face_embedding, project_id, status) VALUES (?, ?, ?, ?, ?, ?)",
                    newUserId.longValue(),
                    request.getRealName(),
                    request.getFaceImageUrl(),
                    request.getFaceEmbedding() == null || request.getFaceEmbedding().isBlank() ? "admin-create-vector"
                            : request.getFaceEmbedding(),
                    request.getProjectId(),
                    "启用");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("userId", newUserId.longValue());
            result.put("username", request.getUsername());
            result.put("realName", request.getRealName());
            result.put("role", normalizedRole);
            result.put("projectId", request.getProjectId());
            result.put("faceImageUrl", request.getFaceImageUrl());
            result.put("faceBound", true);
            return ApiResponse.ok("创建账号并绑定人脸成功", result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail("角色不合法，仅支持 ADMIN/FINANCE/PROJECT_MANAGER");
        }
    }
}
