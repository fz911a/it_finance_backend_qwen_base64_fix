package com.example.itfinance.entity;

public class TeamMember {
    private Long id;
    private Long teamId;
    private Long userId;
    private String role;

    public TeamMember() {}
    public TeamMember(Long id, Long teamId, Long userId, String role) {
        this.id = id; this.teamId = teamId; this.userId = userId; this.role = role;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}