package com.example.itfinance.entity;

import java.math.BigDecimal;

public class ExpenseRecord {
    private Long id;
    private Long projectId;
    private Long employeeId;
    private String expenseType;
    private BigDecimal amount;
    private String expenseDate;
    private String merchantName;
    private String receiptUrl;
    private String aiSummary;
    private String status;

    public ExpenseRecord() {}
    public ExpenseRecord(Long id, Long projectId, Long employeeId, String expenseType, BigDecimal amount, String expenseDate, String merchantName, String receiptUrl, String aiSummary, String status) {
        this.id = id; this.projectId = projectId; this.employeeId = employeeId; this.expenseType = expenseType; this.amount = amount; this.expenseDate = expenseDate; this.merchantName = merchantName; this.receiptUrl = receiptUrl; this.aiSummary = aiSummary; this.status = status;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String expenseType) { this.expenseType = expenseType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getExpenseDate() { return expenseDate; }
    public void setExpenseDate(String expenseDate) { this.expenseDate = expenseDate; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
