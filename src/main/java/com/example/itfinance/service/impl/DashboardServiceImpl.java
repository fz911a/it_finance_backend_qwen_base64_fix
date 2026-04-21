package com.example.itfinance.service.impl;

import com.example.itfinance.dto.DashboardSummary;
import com.example.itfinance.service.DashboardService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DashboardServiceImpl implements DashboardService {
    private final JdbcTemplate jdbcTemplate;

    public DashboardServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private BigDecimal sum(String sql) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long pendingSalaryCount() {
        try {
            // Compatibility path for schemas that keep a status column.
            Long value = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM salary_record WHERE status = '待发放'", Long.class);
            return value == null ? 0L : value;
        } catch (Exception ignored) {
            // Current schema marks unpaid salaries by missing pay_date.
            Long value = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM salary_record WHERE pay_date IS NULL", Long.class);
            return value == null ? 0L : value;
        }
    }

    @Override
    public DashboardSummary getSummary() {
        try {
            BigDecimal totalIncome = sum("SELECT COALESCE(SUM(amount), 0) FROM payment");
            BigDecimal totalCost = sum("SELECT COALESCE(SUM(amount), 0) FROM salary_project_allocation")
                    .add(sum("SELECT COALESCE(SUM(amount), 0) FROM expense_record"));
            BigDecimal totalTax = sum("SELECT COALESCE(SUM(tax_amount), 0) FROM invoice")
                    .add(sum("SELECT COALESCE(SUM(personal_tax), 0) FROM salary_record"));
            BigDecimal receivable = sum("SELECT COALESCE(SUM(unpaid_amount), 0) FROM invoice");
            BigDecimal cashFlow = totalIncome.subtract(totalCost);
            BigDecimal netProfit = totalIncome.subtract(totalCost).subtract(totalTax);

            // 新增业务统计数据
            Long invoiceCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoice", Long.class);
            BigDecimal monthlyIncome = sum(
                    "SELECT COALESCE(SUM(amount), 0) FROM payment WHERE DATE_FORMAT(create_time, '%Y-%m') = DATE_FORMAT(CURRENT_DATE, '%Y-%m')");
            Long pendingSalaryCount = pendingSalaryCount();

            DashboardSummary summary = new DashboardSummary(
                    totalIncome, totalCost, totalTax, netProfit, cashFlow,
                    receivable,
                    invoiceCount == null ? 0L : invoiceCount,
                    monthlyIncome,
                    pendingSalaryCount == null ? 0L : pendingSalaryCount);

            return summary;
        } catch (Exception ex) {
            return new DashboardSummary(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0L,
                    BigDecimal.ZERO,
                    0L);
        }
    }
}
