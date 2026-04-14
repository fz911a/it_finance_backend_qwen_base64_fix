package com.example.itfinance.entity;

import java.math.BigDecimal;

public class Project {
    private Long id;
    private String projectName;
    private String projectCode;
    private String managerName;
    private BigDecimal budgetAmount;

    public Project() {}
    public Project(Long id, String projectName, String projectCode, String managerName, BigDecimal budgetAmount) {
        this.id = id; this.projectName = projectName; this.projectCode = projectCode; this.managerName = managerName; this.budgetAmount = budgetAmount;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount; }
}
