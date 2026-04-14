package com.example.itfinance.dto;

public class ReceiptOcrRequest {
    private String fileUrl;
    private Long projectId;
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
}
