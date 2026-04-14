package com.example.itfinance.dto;

public class AiBookkeepingChatRequest {
    private String userText;
    private Long projectId;

    public String getUserText() { return userText; }
    public void setUserText(String userText) { this.userText = userText; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
}
