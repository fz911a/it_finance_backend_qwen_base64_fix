package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.dto.FaceLoginRequest;
import com.example.itfinance.dto.LoginRequest;
import com.example.itfinance.dto.LoginResponse;
import com.example.itfinance.service.AuthService;
import com.example.itfinance.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {
    private final AuthService authService;
    private final AuditLogService auditLogService;

    public AuthController(AuthService authService, AuditLogService auditLogService) {
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ApiResponse.ok(authService.login(request));
        } catch (IllegalArgumentException e) {
            auditLogService.logLogin(0L, request.getUsername(), null, null, "PASSWORD", "FAILED",
                    e.getMessage());
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/face-login")
    public ApiResponse<LoginResponse> faceLogin(@Valid @RequestBody FaceLoginRequest request) {
        try {
            return ApiResponse.ok(authService.faceLogin(request.getImageUrl()));
        } catch (IllegalArgumentException e) {
            auditLogService.logLogin(0L, "-", "-", "-", "FACE", "FAILED", e.getMessage());
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body == null ? null : body.get("refreshToken");
            return ApiResponse.ok(authService.refreshToken(refreshToken));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/login-logs")
    public ApiResponse<List<Map<String, Object>>> loginLogs() {
        return ApiResponse.ok(auditLogService.listLoginLogs());
    }

    @DeleteMapping("/login-logs")
    public ApiResponse<String> clearLoginLogs() {
        auditLogService.clearLoginLogs();
        return ApiResponse.ok("登录日志已清空", "ok");
    }
}
