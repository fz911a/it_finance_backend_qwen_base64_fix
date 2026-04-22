package com.example.itfinance.service.impl;

import com.example.itfinance.config.JwtProperties;
import com.example.itfinance.dto.FaceRecognizeRequest;
import com.example.itfinance.dto.LoginRequest;
import com.example.itfinance.dto.LoginResponse;
import com.example.itfinance.entity.User;
import com.example.itfinance.security.JwtTokenProvider;
import com.example.itfinance.service.AuthService;
import com.example.itfinance.service.AuditLogService;
import com.example.itfinance.service.FaceService;
import io.jsonwebtoken.Claims;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final FaceService faceService;
    private final AuditLogService auditLogService;
    private final Map<Long, User> userMap = new HashMap<>();

    public AuthServiceImpl(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider, JwtProperties jwtProperties, FaceService faceService,
            AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.faceService = faceService;
        this.auditLogService = auditLogService;
        initDemoUsers();
    }

    private void initDemoUsers() {
        User admin = new User(1L, "admin", "123456", "系统管理员", "ADMIN");
        admin.setEmail("admin@example.com");
        admin.setPhone("13800138000");
        admin.setCompany("示例公司");
        admin.setDepartment("IT部门");
        userMap.put(admin.getId(), admin);

        User finance = new User(2L, "finance", "123456", "财务人员", "FINANCE");
        finance.setEmail("finance@example.com");
        finance.setPhone("13800138001");
        finance.setCompany("示例公司");
        finance.setDepartment("财务部门");
        userMap.put(finance.getId(), finance);

        User manager = new User(3L, "manager1", "123456", "项目负责人", "PROJECT_MANAGER");
        manager.setEmail("manager@example.com");
        manager.setPhone("13800138002");
        manager.setCompany("示例公司");
        manager.setDepartment("项目部门");
        userMap.put(manager.getId(), manager);
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
                    auditLogService.logLogin(fallback.getUserId(), fallback.getUsername(), fallback.getRealName(),
                            fallback.getRole(), "PASSWORD", "SUCCESS", "Demo login");
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
        auditLogService.logLogin(user.id(), user.username(), user.realName(), user.role(), "PASSWORD", "SUCCESS",
                "Password login");
        return new LoginResponse(user.id(), user.username(), user.realName(), user.role(), token, refreshToken);
    }

    @Override
    public LoginResponse faceLogin(String imageUrl) {
        FaceRecognizeRequest recognizeRequest = new FaceRecognizeRequest();
        recognizeRequest.setImageUrl(imageUrl);
        Map<String, Object> result = faceService.recognize(recognizeRequest);
        if (!Boolean.TRUE.equals(result.get("matched"))) {
            String message = String.valueOf(result.getOrDefault("message", "人脸识别失败"));
            throw new IllegalArgumentException(message);
        }

        LoginUserRow user = resolveUserByFaceResult(result);
        String token = jwtTokenProvider.generateToken(user.id(), user.username(), user.role(), user.realName());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.id(), user.username(), user.role(),
                user.realName());
        auditLogService.logLogin(user.id(), user.username(), user.realName(), user.role(), "FACE", "SUCCESS",
                "Face login");
        return new LoginResponse(user.id(), user.username(), user.realName(), user.role(), token, refreshToken);
    }

    private LoginUserRow resolveUserByFaceResult(Map<String, Object> faceResult) {
        Long employeeId = toLong(faceResult.get("employeeId"));
        String employeeName = faceResult.get("employeeName") == null ? null
                : String.valueOf(faceResult.get("employeeName"));

        if (employeeId != null) {
            List<LoginUserRow> usersById = queryActiveUserById(employeeId);
            if (!usersById.isEmpty()) {
                return usersById.get(0);
            }
        }

        if (employeeName != null && !employeeName.isBlank()) {
            List<LoginUserRow> usersByName = queryActiveUserByRealName(employeeName);
            if (!usersByName.isEmpty()) {
                return usersByName.get(0);
            }
        }

        throw new IllegalArgumentException("识别到的人脸未绑定系统账号，请先在系统中创建并绑定对应用户");
    }

    private List<LoginUserRow> queryActiveUserById(Long id) {
        String sql = "SELECT id, username, password, real_name, role FROM sys_user WHERE id = ? AND status = 1 LIMIT 1";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new LoginUserRow(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("real_name"),
                rs.getString("role")), id);
    }

    private List<LoginUserRow> queryActiveUserByRealName(String realName) {
        String sql = "SELECT id, username, password, real_name, role FROM sys_user WHERE real_name = ? AND status = 1 LIMIT 1";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new LoginUserRow(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("real_name"),
                rs.getString("role")), realName);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
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

    @Override
    public User getUserByToken(String token) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(token.replace("Bearer ", ""));
            Long userId = claims.get("userId", Number.class).longValue();
            return userMap.get(userId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Token 无效");
        }
    }

    @Override
    public User updateUserProfile(String token, User user) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(token.replace("Bearer ", ""));
            Long userId = claims.get("userId", Number.class).longValue();
            User existingUser = userMap.get(userId);
            if (existingUser == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            existingUser.setRealName(user.getRealName());
            existingUser.setEmail(user.getEmail());
            existingUser.setPhone(user.getPhone());
            existingUser.setCompany(user.getCompany());
            existingUser.setDepartment(user.getDepartment());
            userMap.put(userId, existingUser);
            return existingUser;
        } catch (Exception e) {
            throw new IllegalArgumentException("更新用户信息失败");
        }
    }

    @Override
    public void changePassword(String token, String oldPassword, String newPassword) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(token.replace("Bearer ", ""));
            Long userId = claims.get("userId", Number.class).longValue();
            User user = userMap.get(userId);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            if (!user.getPassword().equals(oldPassword)) {
                throw new IllegalArgumentException("原密码错误");
            }
            user.setPassword(newPassword);
            userMap.put(userId, user);
        } catch (Exception e) {
            throw new IllegalArgumentException("修改密码失败");
        }
    }

    @Override
    public void updateAvatar(String token, String avatar) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(token.replace("Bearer ", ""));
            Long userId = claims.get("userId", Number.class).longValue();
            User user = userMap.get(userId);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            user.setAvatar(avatar);
            userMap.put(userId, user);
        } catch (Exception e) {
            throw new IllegalArgumentException("更新头像失败");
        }
    }

    @Override
    public void bindPhone(String token, String phone) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(token.replace("Bearer ", ""));
            Long userId = claims.get("userId", Number.class).longValue();
            User user = userMap.get(userId);
            if (user == null) {
                throw new IllegalArgumentException("用户不存在");
            }
            user.setPhone(phone);
            userMap.put(userId, user);
        } catch (Exception e) {
            throw new IllegalArgumentException("绑定手机失败");
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
