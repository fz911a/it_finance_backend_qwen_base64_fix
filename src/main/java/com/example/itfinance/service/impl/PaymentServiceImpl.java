package com.example.itfinance.service.impl;

import com.example.itfinance.entity.Payment;
import com.example.itfinance.service.PaymentService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final JdbcTemplate jdbcTemplate;

    public PaymentServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Payment> list() {
        String sql = "SELECT p.id, DATE_FORMAT(p.payment_date, '%Y-%m-%d') payment_date, p.amount, p.method, pr.project_name, GROUP_CONCAT(i.invoice_no SEPARATOR ', ') invoice_nos, p.remark FROM payment p LEFT JOIN project pr ON p.project_id = pr.id LEFT JOIN payment_invoice pi ON p.id = pi.payment_id LEFT JOIN invoice i ON pi.invoice_id = i.id GROUP BY p.id, p.payment_date, p.amount, p.method, pr.project_name, p.remark ORDER BY p.id DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new Payment(rs.getLong("id"), rs.getString("payment_date"), rs.getBigDecimal("amount"), rs.getString("method"), rs.getString("project_name"), rs.getString("invoice_nos"), rs.getString("remark")));
    }

    @Override
    public Payment create(Payment payment) {
        Long projectId = null;
        if (payment.getProjectName() != null && !payment.getProjectName().isBlank()) {
            List<Long> ids = jdbcTemplate.query("SELECT id FROM project WHERE project_name = ? LIMIT 1", (rs, rowNum) -> rs.getLong(1), payment.getProjectName());
            if (!ids.isEmpty()) projectId = ids.get(0);
        }
        jdbcTemplate.update("INSERT INTO payment(project_id, payment_date, amount, method, remark, create_by) VALUES (?, ?, ?, ?, ?, ?)",
                projectId, payment.getPaymentDate(), payment.getAmount(), payment.getMethod(), payment.getRemark(), 1);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return list().stream().filter(x -> x.getId().equals(id)).findFirst().orElse(payment);
    }
}
