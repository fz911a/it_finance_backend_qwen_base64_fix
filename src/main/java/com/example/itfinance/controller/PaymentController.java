package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.entity.Payment;
import com.example.itfinance.service.AuditLogService;
import com.example.itfinance.service.PaymentService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin
public class PaymentController {
    private final PaymentService paymentService;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public PaymentController(PaymentService paymentService, JdbcTemplate jdbcTemplate,
            AuditLogService auditLogService) {
        this.paymentService = paymentService;
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/list")
    public ApiResponse<List<Payment>> list() {
        return ApiResponse.ok(paymentService.list());
    }

    @PostMapping("/add")
    public ApiResponse<Payment> add(@RequestBody Payment payment) {
        return ApiResponse.ok("新增成功", paymentService.create(payment));
    }

    @PostMapping("/allocate-auto/{paymentId}")
    public ApiResponse<Map<String, Object>> allocateAuto(@PathVariable Long paymentId) {
        List<Map<String, Object>> paymentRows = jdbcTemplate.query(
                "SELECT id, project_id, amount FROM payment WHERE id = ? LIMIT 1",
                (rs, rowNum) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("projectId", rs.getObject("project_id") == null ? null : rs.getLong("project_id"));
                    m.put("amount", rs.getBigDecimal("amount"));
                    return m;
                },
                paymentId);
        if (paymentRows.isEmpty()) {
            return ApiResponse.fail("回款记录不存在");
        }

        Map<String, Object> payment = paymentRows.get(0);
        Long projectId = (Long) payment.get("projectId");
        BigDecimal amount = payment.get("amount") instanceof BigDecimal bd ? bd : BigDecimal.ZERO;
        BigDecimal allocatedBefore = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(allocated_amount),0) FROM payment_invoice WHERE payment_id = ?",
                BigDecimal.class,
                paymentId);
        BigDecimal remaining = amount.subtract(allocatedBefore == null ? BigDecimal.ZERO : allocatedBefore);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.fail("该回款已全部核销");
        }

        String invoiceSql = "SELECT id, invoice_no, unpaid_amount FROM invoice WHERE unpaid_amount > 0"
                + (projectId == null ? "" : " AND project_id = ?")
                + " ORDER BY invoice_date ASC, id ASC";

        List<Map<String, Object>> invoices = projectId == null
                ? jdbcTemplate.query(invoiceSql, (rs, rowNum) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("invoiceNo", rs.getString("invoice_no"));
                    m.put("unpaid", rs.getBigDecimal("unpaid_amount"));
                    return m;
                })
                : jdbcTemplate.query(invoiceSql, (rs, rowNum) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("invoiceNo", rs.getString("invoice_no"));
                    m.put("unpaid", rs.getBigDecimal("unpaid_amount"));
                    return m;
                }, projectId);

        List<Map<String, Object>> allocationRows = new ArrayList<>();
        for (Map<String, Object> invoice : invoices) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            Long invoiceId = (Long) invoice.get("id");
            BigDecimal unpaid = invoice.get("unpaid") instanceof BigDecimal bd ? bd : BigDecimal.ZERO;
            if (unpaid.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal allocate = remaining.min(unpaid);
            jdbcTemplate.update(
                    "INSERT INTO payment_invoice(payment_id, invoice_id, allocated_amount) VALUES (?, ?, ?)",
                    paymentId,
                    invoiceId,
                    allocate);

            jdbcTemplate.update(
                    "UPDATE invoice SET paid_amount = COALESCE(paid_amount,0) + ?, unpaid_amount = GREATEST(COALESCE(unpaid_amount,0) - ?, 0), "
                            + "status = CASE WHEN GREATEST(COALESCE(unpaid_amount,0) - ?, 0)=0 THEN '已回款' ELSE '部分回款' END WHERE id = ?",
                    allocate,
                    allocate,
                    allocate,
                    invoiceId);

            remaining = remaining.subtract(allocate);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("invoiceId", invoiceId);
            item.put("invoiceNo", invoice.get("invoiceNo"));
            item.put("allocatedAmount", allocate);
            allocationRows.add(item);
        }

        BigDecimal allocatedNow = allocationRows.stream()
                .map(x -> (BigDecimal) x.get("allocatedAmount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        auditLogService.log("payment", "allocate-auto",
                "自动核销 paymentId=" + paymentId + ", allocated=" + allocatedNow);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentId", paymentId);
        result.put("allocatedAmount", allocatedNow);
        result.put("remainingAmount", remaining);
        result.put("allocations", allocationRows);
        return ApiResponse.ok("自动核销完成", result);
    }

    @PostMapping("/allocate-manual/{paymentId}")
    public ApiResponse<Map<String, Object>> allocateManual(@PathVariable Long paymentId,
            @RequestBody Map<String, Object> body) {
        Object invoiceIdObj = body.get("invoiceId");
        Object amountObj = body.get("amount");
        if (invoiceIdObj == null || amountObj == null) {
            return ApiResponse.fail("invoiceId 和 amount 必填");
        }

        Long invoiceId = Long.valueOf(String.valueOf(invoiceIdObj));
        BigDecimal allocateAmount = new BigDecimal(String.valueOf(amountObj));
        if (allocateAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.fail("amount 必须大于 0");
        }

        List<BigDecimal> paymentAmounts = jdbcTemplate.query(
                "SELECT amount FROM payment WHERE id = ? LIMIT 1",
                (rs, rowNum) -> rs.getBigDecimal("amount"),
                paymentId);
        if (paymentAmounts.isEmpty() || paymentAmounts.get(0) == null) {
            return ApiResponse.fail("回款记录不存在");
        }
        BigDecimal paymentAmount = paymentAmounts.get(0);

        BigDecimal allocatedBefore = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(allocated_amount),0) FROM payment_invoice WHERE payment_id = ?",
                BigDecimal.class,
                paymentId);
        BigDecimal paymentRemaining = paymentAmount
                .subtract(allocatedBefore == null ? BigDecimal.ZERO : allocatedBefore);
        if (allocateAmount.compareTo(paymentRemaining) > 0) {
            return ApiResponse.fail("分配金额超过回款剩余可核销金额");
        }

        BigDecimal unpaid = jdbcTemplate.queryForObject("SELECT unpaid_amount FROM invoice WHERE id = ?",
                BigDecimal.class,
                invoiceId);
        if (unpaid == null) {
            return ApiResponse.fail("发票不存在");
        }
        if (allocateAmount.compareTo(unpaid) > 0) {
            return ApiResponse.fail("分配金额超过发票未回款金额");
        }

        jdbcTemplate.update("INSERT INTO payment_invoice(payment_id, invoice_id, allocated_amount) VALUES (?, ?, ?)",
                paymentId,
                invoiceId,
                allocateAmount);
        jdbcTemplate.update(
                "UPDATE invoice SET paid_amount = COALESCE(paid_amount,0) + ?, unpaid_amount = GREATEST(COALESCE(unpaid_amount,0)-?,0), "
                        + "status = CASE WHEN GREATEST(COALESCE(unpaid_amount,0)-?,0)=0 THEN '已回款' ELSE '部分回款' END WHERE id = ?",
                allocateAmount,
                allocateAmount,
                allocateAmount,
                invoiceId);

        auditLogService.log("payment", "allocate-manual",
                "手动核销 paymentId=" + paymentId + ", invoiceId=" + invoiceId + ", amount=" + allocateAmount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentId", paymentId);
        result.put("invoiceId", invoiceId);
        result.put("allocatedAmount", allocateAmount);
        return ApiResponse.ok("手动核销完成", result);
    }
}
