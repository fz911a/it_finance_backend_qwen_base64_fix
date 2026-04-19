package com.example.itfinance.service.impl;

import com.example.itfinance.entity.ExpenseRecord;
import com.example.itfinance.service.AuditLogService;
import com.example.itfinance.service.ExpenseService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpenseServiceImpl implements ExpenseService {
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public ExpenseServiceImpl(JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    private ExpenseRecord map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ExpenseRecord(rs.getLong("id"), rs.getLong("project_id"), rs.getLong("employee_id"),
                rs.getString("expense_type"), rs.getBigDecimal("amount"), rs.getString("expense_date"),
                rs.getString("merchant_name"), rs.getString("receipt_url"), rs.getString("ai_summary"),
                rs.getString("status"));
    }

    @Override
    public List<ExpenseRecord> list() {
        return jdbcTemplate.query(
                "SELECT id, project_id, employee_id, expense_type, amount, DATE_FORMAT(expense_date, '%Y-%m-%d') expense_date, merchant_name, receipt_url, ai_summary, status FROM expense_record ORDER BY id DESC",
                this::map);
    }

    @Override
    public ExpenseRecord getById(Long id) {
        List<ExpenseRecord> list = jdbcTemplate.query(
                "SELECT id, project_id, employee_id, expense_type, amount, DATE_FORMAT(expense_date, '%Y-%m-%d') expense_date, merchant_name, receipt_url, ai_summary, status FROM expense_record WHERE id = ?",
                this::map, id);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public ExpenseRecord create(ExpenseRecord record) {
        jdbcTemplate.update(
                "INSERT INTO expense_record(project_id, employee_id, expense_type, amount, expense_date, merchant_name, receipt_url, ai_summary, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                record.getProjectId(), record.getEmployeeId(), record.getExpenseType(), record.getAmount(),
                record.getExpenseDate(), record.getMerchantName(), record.getReceiptUrl(), record.getAiSummary(),
                record.getStatus() == null ? "待提交" : record.getStatus());
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        auditLogService.log("费用报销", "创建申请", "创建费用申请，金额: " + record.getAmount() + ", 类型: " + record.getExpenseType());
        return getById(id);
    }

    @Override
    public ExpenseRecord updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE expense_record SET status = ? WHERE id = ?", status, id);
        auditLogService.log("费用报销", "状态变更", "费用申请(ID:" + id + ")状态变更为: " + status);
        return getById(id);
    }
}
