package com.example.itfinance.entity;

public class User {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String role;
    private String avatar;
    private String email;
    private String phone;
    private String company;
    private String department;

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
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
