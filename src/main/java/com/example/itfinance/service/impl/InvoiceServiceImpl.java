package com.example.itfinance.service.impl;

import com.example.itfinance.entity.Invoice;
import com.example.itfinance.service.InvoiceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class InvoiceServiceImpl implements InvoiceService {
    private final JdbcTemplate jdbcTemplate;

    public InvoiceServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Invoice mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Invoice(
                rs.getLong("id"),
                rs.getString("invoice_no"),
                rs.getString("invoice_date"),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("tax_amount"),
                rs.getString("customer_name"),
                rs.getString("project_name"),
                rs.getString("status"),
                rs.getBigDecimal("unpaid_amount"),
                rs.getString("file_url"),
                rs.getString("ocr_result"));
    }

    @Override
    public List<Invoice> list() {
        String sql = "SELECT i.id, i.invoice_no, DATE_FORMAT(i.invoice_date, '%Y-%m-%d') invoice_date, i.amount, i.tax_amount, i.customer_name, p.project_name, i.status, i.unpaid_amount, i.file_url, i.ocr_result FROM invoice i LEFT JOIN project p ON i.project_id = p.id ORDER BY i.id DESC";
        return jdbcTemplate.query(sql, this::mapRow);
    }

    @Override
    public Invoice getById(Long id) {
        String sql = "SELECT i.id, i.invoice_no, DATE_FORMAT(i.invoice_date, '%Y-%m-%d') invoice_date, i.amount, i.tax_amount, i.customer_name, p.project_name, i.status, i.unpaid_amount, i.file_url, i.ocr_result FROM invoice i LEFT JOIN project p ON i.project_id = p.id WHERE i.id = ?";
        List<Invoice> list = jdbcTemplate.query(sql, this::mapRow, id);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Invoice create(Invoice invoice) {
        Long projectId = null;
        if (invoice.getProjectName() != null && !invoice.getProjectName().isBlank()) {
            List<Long> ids = jdbcTemplate.query("SELECT id FROM project WHERE project_name = ? LIMIT 1",
                    (rs, rowNum) -> rs.getLong(1), invoice.getProjectName());
            if (!ids.isEmpty())
                projectId = ids.get(0);
        }
        if (invoice.getStatus() == null || invoice.getStatus().isBlank())
            invoice.setStatus("未回款");
        if (invoice.getUnpaidAmount() == null && invoice.getAmount() != null)
            invoice.setUnpaidAmount(invoice.getAmount());
        String invoiceDate = normalizeDate(invoice.getInvoiceDate());
        String invoiceNo = (invoice.getInvoiceNo() == null || invoice.getInvoiceNo().isBlank())
                ? ("AI-" + System.currentTimeMillis())
                : invoice.getInvoiceNo();
        String customerName = (invoice.getCustomerName() == null || invoice.getCustomerName().isBlank()) ? "未命名客户"
                : invoice.getCustomerName();
        String fileUrl = (invoice.getFileUrl() == null || invoice.getFileUrl().isBlank()) ? null : invoice.getFileUrl();
        String ocrResult = (invoice.getOcrResult() == null || invoice.getOcrResult().isBlank()) ? null
                : invoice.getOcrResult();
        jdbcTemplate.update(
                "INSERT INTO invoice(invoice_no, invoice_date, amount, tax_amount, customer_name, project_id, file_url, ocr_result, unpaid_amount, status, create_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                invoiceNo, invoiceDate, invoice.getAmount(), invoice.getTaxAmount(), customerName, projectId, fileUrl,
                ocrResult, invoice.getUnpaidAmount(), invoice.getStatus(), 1);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return getById(id);
    }

    private String normalizeDate(String raw) {
        if (raw == null || raw.isBlank())
            return null;
        String value = raw.trim().replace("/", "-").replace(".", "-").replace("年", "-").replace("月", "-")
                .replace("日", "").replaceAll("--+", "-");
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-M-d")).toString();
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString();
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM payment_invoice WHERE invoice_id = ?", id);
        jdbcTemplate.update("DELETE FROM invoice WHERE id = ?", id);
    }
}
