package com.example.itfinance.entity;

import java.math.BigDecimal;

public class SalaryRecord {
    private Long id;
    private String employeeName;
    private String jobTitle;
    private String payPeriod;
    private BigDecimal grossSalary;
    private String projectName;
    private String allocationRatio;

    public SalaryRecord() {}
    public SalaryRecord(Long id, String employeeName, String jobTitle, String payPeriod, BigDecimal grossSalary, String projectName, String allocationRatio) {
        this.id = id; this.employeeName = employeeName; this.jobTitle = jobTitle; this.payPeriod = payPeriod; this.grossSalary = grossSalary; this.projectName = projectName; this.allocationRatio = allocationRatio;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getPayPeriod() { return payPeriod; }
    public void setPayPeriod(String payPeriod) { this.payPeriod = payPeriod; }
    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getAllocationRatio() { return allocationRatio; }
    public void setAllocationRatio(String allocationRatio) { this.allocationRatio = allocationRatio; }
}
