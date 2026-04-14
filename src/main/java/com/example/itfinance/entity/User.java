package com.example.itfinance.entity;

public class User {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String role;

    public User() {}
    public User(Long id, String username, String password, String realName, String role) {
        this.id = id; this.username = username; this.password = password; this.realName = realName; this.role = role;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
