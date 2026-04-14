package com.example.itfinance.entity;

import java.math.BigDecimal;

public class Payment {
    private Long id;
    private String paymentDate;
    private BigDecimal amount;
    private String method;
    private String projectName;
    private String invoiceNos;
    private String remark;

    public Payment() {}
    public Payment(Long id, String paymentDate, BigDecimal amount, String method, String projectName, String invoiceNos, String remark) {
        this.id = id; this.paymentDate = paymentDate; this.amount = amount; this.method = method; this.projectName = projectName; this.invoiceNos = invoiceNos; this.remark = remark;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getInvoiceNos() { return invoiceNos; }
    public void setInvoiceNos(String invoiceNos) { this.invoiceNos = invoiceNos; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
