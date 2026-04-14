package com.example.itfinance.service.impl;

import com.example.itfinance.config.JwtProperties;
import com.example.itfinance.dto.LoginRequest;
import com.example.itfinance.dto.LoginResponse;
import com.example.itfinance.security.JwtTokenProvider;
import com.example.itfinance.service.AuthService;
import io.jsonwebtoken.Claims;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AuthServiceImpl(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider, JwtProperties jwtProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String sql = "SELECT id, username, password, real_name, role FROM sys_user WHERE username = ? AND status = 1 LIMIT 1";
        List<LoginUserRow> rows;
        try {
            rows = jdbcTemplate.query(sql, (rs, rowNum) -> new LoginUserRow(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("real_name"),
                    rs.getString("role")), request.getUsername());
        } catch (Exception ex) {
            if (jwtProperties.isAllowDemoLogin()) {
                LoginResponse fallback = tryDemoLogin(request);
                if (fallback != null) {
                    return fallback;
                }
            }
            throw new IllegalArgumentException("登录失败：数据库不可用，请检查数据库配置");
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        LoginUserRow user = rows.get(0);
        boolean passwordOk;
        if (user.password().startsWith("$2a$") || user.password().startsWith("$2b$")
                || user.password().startsWith("$2y$")) {
            passwordOk = passwordEncoder.matches(request.getPassword(), user.password());
        } else {
            passwordOk = request.getPassword().equals(user.password());
        }

        if (!passwordOk) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(user.id(), user.username(), user.role(), user.realName());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.id(), user.username(), user.role(),
                user.realName());
        return new LoginResponse(user.id(), user.username(), user.realName(), user.role(), token, refreshToken);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken 不能为空");
        }
        try {
            Claims claims = jwtTokenProvider.parseClaims(refreshToken);
            String tokenType = String.valueOf(claims.get("tokenType"));
            if (!"refresh".equals(tokenType)) {
                throw new IllegalArgumentException("refreshToken 非法");
            }

            Long userId = claims.get("userId", Number.class).longValue();
            String username = claims.getSubject();
            String role = String.valueOf(claims.get("role"));
            String realName = String.valueOf(claims.get("realName"));

            String newAccessToken = jwtTokenProvider.generateToken(userId, username, role, realName);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, username, role, realName);
            return new LoginResponse(userId, username, realName, role, newAccessToken, newRefreshToken);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("refreshToken 已失效，请重新登录");
        }
    }

    private LoginResponse tryDemoLogin(LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return null;
        }
        Map<String, LoginUserRow> demoUsers = Map.of(
                "admin", new LoginUserRow(1L, "admin", "123456", "系统管理员", "ADMIN"),
                "finance", new LoginUserRow(2L, "finance", "123456", "财务人员", "FINANCE"),
                "manager1", new LoginUserRow(3L, "manager1", "123456", "项目负责人", "PROJECT_MANAGER"));
        LoginUserRow user = demoUsers.get(request.getUsername());
        if (user == null || !user.password().equals(request.getPassword())) {
            return null;
        }
        String token = jwtTokenProvider.generateToken(user.id(), user.username(), user.role(), user.realName());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.id(), user.username(), user.role(),
                user.realName());
        return new LoginResponse(user.id(), user.username(), user.realName(), user.role(), token, refreshToken);
    }

    private record LoginUserRow(Long id, String username, String password, String realName, String role) {
    }
}
