package com.example.itfinance.entity;

import java.math.BigDecimal;

public class Invoice {
    private Long id;
    private String invoiceNo;
    private String invoiceDate;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private String customerName;
    private String projectName;
    private String status;
    private BigDecimal unpaidAmount;
    private String fileUrl;
    private String ocrResult;

    public Invoice() {
    }

    public Invoice(Long id, String invoiceNo, String invoiceDate, BigDecimal amount, BigDecimal taxAmount,
            String customerName, String projectName, String status, BigDecimal unpaidAmount, String fileUrl,
            String ocrResult) {
        this.id = id;
        this.invoiceNo = invoiceNo;
        this.invoiceDate = invoiceDate;
        this.amount = amount;
        this.taxAmount = taxAmount;
        this.customerName = customerName;
        this.projectName = projectName;
        this.status = status;
        this.unpaidAmount = unpaidAmount;
        this.fileUrl = fileUrl;
        this.ocrResult = ocrResult;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(String invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getUnpaidAmount() {
        return unpaidAmount;
    }

    public void setUnpaidAmount(BigDecimal unpaidAmount) {
        this.unpaidAmount = unpaidAmount;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getOcrResult() {
        return ocrResult;
    }

    public void setOcrResult(String ocrResult) {
        this.ocrResult = ocrResult;
    }
}
