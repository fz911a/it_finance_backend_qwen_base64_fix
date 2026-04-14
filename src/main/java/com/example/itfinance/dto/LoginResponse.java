package com.example.itfinance.dto;

public class LoginResponse {
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String token;
    private String refreshToken;

    public LoginResponse() {
    }

    public LoginResponse(Long userId, String username, String realName, String role, String token) {
        this(userId, username, realName, role, token, null);
    }

    public LoginResponse(Long userId, String username, String realName, String role, String token,
            String refreshToken) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.role = role;
        this.token = token;
        this.refreshToken = refreshToken;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
