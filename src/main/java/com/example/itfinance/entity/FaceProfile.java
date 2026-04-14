package com.example.itfinance.entity;

public class FaceProfile {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String faceImageUrl;
    private String faceEmbedding;
    private Long projectId;
    private String status;
    public FaceProfile() {}
    public FaceProfile(Long id, Long employeeId, String employeeName, String faceImageUrl, String faceEmbedding, Long projectId, String status) { this.id=id; this.employeeId=employeeId; this.employeeName=employeeName; this.faceImageUrl=faceImageUrl; this.faceEmbedding=faceEmbedding; this.projectId=projectId; this.status=status; }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; } public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; } public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getFaceImageUrl() { return faceImageUrl; } public void setFaceImageUrl(String faceImageUrl) { this.faceImageUrl = faceImageUrl; }
    public String getFaceEmbedding() { return faceEmbedding; } public void setFaceEmbedding(String faceEmbedding) { this.faceEmbedding = faceEmbedding; }
    public Long getProjectId() { return projectId; } public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
}
