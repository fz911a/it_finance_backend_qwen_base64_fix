package com.example.itfinance.service.impl;

import com.example.itfinance.entity.SalaryRecord;
import com.example.itfinance.service.SalaryService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalaryServiceImpl implements SalaryService {
    private final JdbcTemplate jdbcTemplate;

    public SalaryServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private SalaryRecord mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new SalaryRecord(
                rs.getLong("id"),
                rs.getString("employee_name"),
                rs.getString("position_name"),
                rs.getString("pay_period"),
                rs.getBigDecimal("gross_salary"),
                rs.getString("project_name"),
                rs.getString("allocation_ratio"));
    }

    @Override
    public List<SalaryRecord> list() {
        String sql = "SELECT sr.id, e.name employee_name, e.position_name, sr.pay_period, sr.gross_salary, "
                + "COALESCE(p.project_name, '未分配') project_name, "
                + "COALESCE(CONCAT(spa.ratio, '%'), '-') allocation_ratio "
                + "FROM salary_record sr "
                + "LEFT JOIN employee e ON sr.employee_id = e.id "
                + "LEFT JOIN salary_project_allocation spa ON spa.salary_id = sr.id "
                + "LEFT JOIN project p ON spa.project_id = p.id "
                + "ORDER BY sr.id DESC";
        return jdbcTemplate.query(sql, this::mapRow);
    }

    @Override
    public SalaryRecord create(SalaryRecord record) {
        jdbcTemplate.update(
                "INSERT INTO salary_record(employee_id, pay_period, gross_salary, personal_tax, actual_salary, pay_date) VALUES (?,?,?,0,?,CURDATE())",
                1L, record.getPayPeriod(), record.getGrossSalary(), record.getGrossSalary());
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        List<SalaryRecord> list = jdbcTemplate.query(
                "SELECT sr.id, e.name employee_name, e.position_name, sr.pay_period, sr.gross_salary, '' project_name, '-' allocation_ratio FROM salary_record sr LEFT JOIN employee e ON sr.employee_id=e.id WHERE sr.id=?",
                this::mapRow, id);
        return list.isEmpty() ? null : list.get(0);
    }
}
