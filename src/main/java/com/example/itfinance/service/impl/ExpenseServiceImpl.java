package com.example.itfinance.service.impl;

import com.example.itfinance.entity.ExpenseRecord;
import com.example.itfinance.service.ExpenseService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpenseServiceImpl implements ExpenseService {
    private final JdbcTemplate jdbcTemplate;
    public ExpenseServiceImpl(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    private ExpenseRecord map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ExpenseRecord(rs.getLong("id"), rs.getLong("project_id"), rs.getLong("employee_id"), rs.getString("expense_type"), rs.getBigDecimal("amount"), rs.getString("expense_date"), rs.getString("merchant_name"), rs.getString("receipt_url"), rs.getString("ai_summary"), rs.getString("status"));
    }
    @Override
    public List<ExpenseRecord> list() {
        return jdbcTemplate.query("SELECT id, project_id, employee_id, expense_type, amount, DATE_FORMAT(expense_date, '%Y-%m-%d') expense_date, merchant_name, receipt_url, ai_summary, status FROM expense_record ORDER BY id DESC", this::map);
    }
    @Override
    public ExpenseRecord getById(Long id) {
        List<ExpenseRecord> list = jdbcTemplate.query("SELECT id, project_id, employee_id, expense_type, amount, DATE_FORMAT(expense_date, '%Y-%m-%d') expense_date, merchant_name, receipt_url, ai_summary, status FROM expense_record WHERE id = ?", this::map, id);
        return list.isEmpty() ? null : list.get(0);
    }
    @Override
    public ExpenseRecord create(ExpenseRecord record) {
        jdbcTemplate.update("INSERT INTO expense_record(project_id, employee_id, expense_type, amount, expense_date, merchant_name, receipt_url, ai_summary, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                record.getProjectId(), record.getEmployeeId(), record.getExpenseType(), record.getAmount(), record.getExpenseDate(), record.getMerchantName(), record.getReceiptUrl(), record.getAiSummary(), record.getStatus() == null ? "待提交" : record.getStatus());
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return getById(id);
    }
    @Override
    public ExpenseRecord updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE expense_record SET status = ? WHERE id = ?", status, id);
        return getById(id);
    }
}
