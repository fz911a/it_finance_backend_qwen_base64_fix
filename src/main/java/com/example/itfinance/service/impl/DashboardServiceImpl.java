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
            return new DashboardSummary(totalIncome, totalCost, totalTax, netProfit, cashFlow, receivable);
        } catch (Exception ex) {
            return new DashboardSummary(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO);
        }
    }
}
