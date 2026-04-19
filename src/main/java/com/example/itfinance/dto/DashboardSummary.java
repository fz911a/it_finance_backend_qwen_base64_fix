package com.example.itfinance.dto;

import java.math.BigDecimal;

public class DashboardSummary {
    private BigDecimal totalIncome;
    private BigDecimal totalCost;
    private BigDecimal totalTax;
    private BigDecimal netProfit;
    private BigDecimal cashFlow;
    private BigDecimal receivable;
    private Long invoiceCount;
    private BigDecimal monthlyIncome;
    private Long pendingSalaryCount;

    public DashboardSummary() {
    }

    public DashboardSummary(BigDecimal totalIncome, BigDecimal totalCost, BigDecimal totalTax, BigDecimal netProfit,
            BigDecimal cashFlow, BigDecimal receivable, Long invoiceCount, BigDecimal monthlyIncome,
            Long pendingSalaryCount) {
        this.totalIncome = totalIncome;
        this.totalCost = totalCost;
        this.totalTax = totalTax;
        this.netProfit = netProfit;
        this.cashFlow = cashFlow;
        this.receivable = receivable;
        this.invoiceCount = invoiceCount;
        this.monthlyIncome = monthlyIncome;
        this.pendingSalaryCount = pendingSalaryCount;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
    }

    public BigDecimal getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(BigDecimal netProfit) {
        this.netProfit = netProfit;
    }

    public BigDecimal getCashFlow() {
        return cashFlow;
    }

    public void setCashFlow(BigDecimal cashFlow) {
        this.cashFlow = cashFlow;
    }

    public BigDecimal getReceivable() {
        return receivable;
    }

    public void setReceivable(BigDecimal receivable) {
        this.receivable = receivable;
    }

    public Long getInvoiceCount() {
        return invoiceCount;
    }

    public void setInvoiceCount(Long invoiceCount) {
        this.invoiceCount = invoiceCount;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public Long getPendingSalaryCount() {
        return pendingSalaryCount;
    }

    public void setPendingSalaryCount(Long pendingSalaryCount) {
        this.pendingSalaryCount = pendingSalaryCount;
    }
}
