package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.dto.DuplicateCheckRequest;
import com.example.itfinance.service.AuditLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/finance")
@CrossOrigin
public class FinanceAssistController {
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public FinanceAssistController(JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/verify-invoice/{id}")
    public ApiResponse<Map<String, Object>> verify(@PathVariable Long id) {
        String sql = "SELECT invoice_no, DATE_FORMAT(invoice_date, '%Y-%m-%d') invoice_date, amount, tax_amount, customer_name "
                + "FROM invoice WHERE id = ? LIMIT 1";
        List<Map<String, Object>> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("invoiceNo", rs.getString("invoice_no"));
            m.put("invoiceDate", rs.getString("invoice_date"));
            m.put("amount", rs.getBigDecimal("amount"));
            m.put("taxAmount", rs.getBigDecimal("tax_amount"));
            m.put("customerName", rs.getString("customer_name"));
            return m;
        }, id);

        if (rows.isEmpty()) {
            return ApiResponse.fail("未找到对应发票");
        }

        Map<String, Object> invoice = rows.get(0);
        String invoiceNo = String.valueOf(invoice.getOrDefault("invoiceNo", ""));
        String invoiceDate = String.valueOf(invoice.getOrDefault("invoiceDate", ""));
        BigDecimal amount = invoice.get("amount") instanceof BigDecimal bd ? bd : BigDecimal.ZERO;

        Integer sameNoCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoice WHERE invoice_no = ?",
                Integer.class, invoiceNo);

        boolean numberValid = invoiceNo.matches("^[A-Za-z0-9]{8,32}$");
        boolean dateValid = false;
        try {
            LocalDate parsedDate = LocalDate.parse(invoiceDate);
            dateValid = !parsedDate.isAfter(LocalDate.now());
        } catch (Exception ignored) {
            dateValid = false;
        }
        boolean amountValid = amount.compareTo(BigDecimal.ZERO) > 0;
        boolean duplicate = sameNoCount != null && sameNoCount > 1;
        boolean verified = numberValid && dateValid && amountValid && !duplicate;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("invoiceId", id);
        map.put("invoiceNo", invoiceNo);
        map.put("verified", verified);
        map.put("duplicate", duplicate);
        map.put("sameInvoiceNoCount", sameNoCount == null ? 0 : sameNoCount);
        map.put("checks", Map.of(
                "invoiceNoFormat", numberValid,
                "invoiceDate", dateValid,
                "amount", amountValid));
        map.put("message", verified ? "发票规则验真通过" : "发票规则验真未通过，请复核票号/日期/金额或查重结果");
        return ApiResponse.ok(map);
    }

    @GetMapping("/duplicate-check/{id}")
    public ApiResponse<Map<String, Object>> duplicate(@PathVariable Long id) {
        String invoiceNo = jdbcTemplate.queryForObject("SELECT invoice_no FROM invoice WHERE id = ?", String.class, id);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoice WHERE invoice_no = ?", Integer.class,
                invoiceNo);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("invoiceId", id);
        map.put("invoiceNo", invoiceNo);
        map.put("duplicate", count != null && count > 1);
        map.put("count", count == null ? 0 : count);
        return ApiResponse.ok(map);
    }

    @PostMapping("/quality-check")
    public ApiResponse<Map<String, Object>> quality(@RequestBody Map<String, String> body) {
        Map<String, Object> map = new LinkedHashMap<>();
        String imageUrl = body.getOrDefault("imageUrl", "");
        String lower = imageUrl.toLowerCase();
        double score = 0.92;
        if (lower.contains("blur") || lower.contains("模糊"))
            score -= 0.35;
        if (lower.contains("dark") || lower.contains("over") || lower.contains("暗"))
            score -= 0.20;
        if (lower.contains("tilt") || lower.contains("skew") || lower.contains("倾斜"))
            score -= 0.15;
        if (lower.contains("small") || lower.contains("tiny"))
            score -= 0.10;
        score = Math.max(0.2, Math.min(0.99, score));

        boolean clear = score >= 0.70;
        map.put("imageUrl", imageUrl);
        map.put("clear", clear);
        map.put("score", score);
        map.put("message", clear ? "清晰度良好，可继续识别" : "图像质量一般，建议重拍（正对票据、补光、避免抖动）");
        map.put("suggestions", clear
                ? List.of("可直接识别", "如金额字段置信度低再人工复核")
                : List.of("靠近票据填满画面", "提高亮度", "保持镜头稳定", "避免倾斜拍摄"));
        return ApiResponse.ok(map);
    }

    @PostMapping("/duplicate-check-advanced")
    public ApiResponse<Map<String, Object>> duplicateAdvanced(@RequestBody DuplicateCheckRequest request) {
        if (request.getInvoiceNo() == null || request.getInvoiceNo().isBlank()) {
            return ApiResponse.fail("invoiceNo 不能为空");
        }

        String sql = "SELECT id, invoice_no, DATE_FORMAT(invoice_date, '%Y-%m-%d') invoice_date, amount, customer_name "
                + "FROM invoice WHERE invoice_no = ? ORDER BY id DESC";
        List<Map<String, Object>> candidates = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rs.getLong("id"));
            m.put("invoiceNo", rs.getString("invoice_no"));
            m.put("invoiceDate", rs.getString("invoice_date"));
            m.put("amount", rs.getBigDecimal("amount"));
            m.put("customerName", rs.getString("customer_name"));
            return m;
        }, request.getInvoiceNo());

        int exactMatches = 0;
        for (Map<String, Object> c : candidates) {
            boolean dateMatch = request.getInvoiceDate() == null || request.getInvoiceDate().isBlank()
                    || request.getInvoiceDate().equals(String.valueOf(c.get("invoiceDate")));
            boolean amountMatch = request.getAmount() == null
                    || (c.get("amount") instanceof BigDecimal bd && bd.compareTo(request.getAmount()) == 0);
            boolean customerMatch = request.getCustomerName() == null || request.getCustomerName().isBlank()
                    || request.getCustomerName().equals(String.valueOf(c.get("customerName")));
            if (dateMatch && amountMatch && customerMatch) {
                exactMatches++;
            }
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("invoiceNo", request.getInvoiceNo());
        map.put("duplicate", exactMatches > 0 || candidates.size() > 1);
        map.put("exactMatchCount", exactMatches);
        map.put("sameInvoiceNoCount", candidates.size());
        map.put("candidates", candidates);
        auditLogService.log("finance", "duplicate-check-advanced", "发票查重 invoiceNo=" + request.getInvoiceNo());
        return ApiResponse.ok(map);
    }

    @GetMapping("/approval-todo")
    public ApiResponse<List<Map<String, Object>>> approvalTodo() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("title", "待审核费用单");
        a.put("count", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM expense_record WHERE status IN ('待提交','待审核')",
                Integer.class));
        list.add(a);
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("title", "待核验发票");
        b.put("count", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoice WHERE status = '未回款'", Integer.class));
        list.add(b);
        return ApiResponse.ok(list);
    }

    @GetMapping("/risk-alerts")
    public ApiResponse<List<Map<String, Object>>> riskAlerts() {
        List<Map<String, Object>> list = new ArrayList<>();
        BigDecimal receivable = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(unpaid_amount),0) FROM invoice",
                BigDecimal.class);
        if (receivable != null && receivable.compareTo(new BigDecimal("30000")) > 0) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("level", "高");
            m.put("title", "未收账款偏高");
            m.put("message", "当前未收账款达到 " + receivable + " 元，建议优先催收。");
            list.add(m);
        }
        Integer pending = jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM expense_record WHERE status IN ('待提交','待审核')", Integer.class);
        if (pending != null && pending > 0) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("level", "中");
            m.put("title", "费用待审核");
            m.put("message", "当前有 " + pending + " 条费用记录待处理。");
            list.add(m);
        }
        if (list.isEmpty()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("level", "低");
            m.put("title", "暂无明显风险");
            m.put("message", "当前关键指标正常。");
            list.add(m);
        }
        return ApiResponse.ok(list);
    }

    @GetMapping("/receivable-aging")
    public ApiResponse<Map<String, Object>> receivableAging(
            @RequestParam(value = "projectId", required = false) Long projectId) {
        String baseSql = "SELECT invoice_no, invoice_date, unpaid_amount, customer_name, project_id FROM invoice WHERE unpaid_amount > 0";
        String sql = projectId == null ? baseSql : baseSql + " AND project_id = ?";

        List<Map<String, Object>> rows = projectId == null
                ? jdbcTemplate.query(sql, (rs, rowNum) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("invoiceNo", rs.getString("invoice_no"));
                    m.put("invoiceDate", rs.getDate("invoice_date"));
                    m.put("unpaidAmount", rs.getBigDecimal("unpaid_amount"));
                    m.put("customerName", rs.getString("customer_name"));
                    m.put("projectId", rs.getLong("project_id"));
                    return m;
                })
                : jdbcTemplate.query(sql, (rs, rowNum) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("invoiceNo", rs.getString("invoice_no"));
                    m.put("invoiceDate", rs.getDate("invoice_date"));
                    m.put("unpaidAmount", rs.getBigDecimal("unpaid_amount"));
                    m.put("customerName", rs.getString("customer_name"));
                    m.put("projectId", rs.getLong("project_id"));
                    return m;
                }, projectId);

        BigDecimal b0_30 = BigDecimal.ZERO;
        BigDecimal b31_60 = BigDecimal.ZERO;
        BigDecimal b61_90 = BigDecimal.ZERO;
        BigDecimal b90Plus = BigDecimal.ZERO;
        List<Map<String, Object>> detail = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Map<String, Object> row : rows) {
            java.sql.Date date = (java.sql.Date) row.get("invoiceDate");
            BigDecimal unpaid = row.get("unpaidAmount") instanceof BigDecimal bd ? bd : BigDecimal.ZERO;
            long days = date == null ? 0 : java.time.temporal.ChronoUnit.DAYS.between(date.toLocalDate(), today);

            String bucket;
            if (days <= 30) {
                b0_30 = b0_30.add(unpaid);
                bucket = "0-30";
            } else if (days <= 60) {
                b31_60 = b31_60.add(unpaid);
                bucket = "31-60";
            } else if (days <= 90) {
                b61_90 = b61_90.add(unpaid);
                bucket = "61-90";
            } else {
                b90Plus = b90Plus.add(unpaid);
                bucket = "90+";
            }

            Map<String, Object> item = new LinkedHashMap<>(row);
            item.put("overdueDays", Math.max(days, 0));
            item.put("agingBucket", bucket);
            detail.add(item);
        }

        BigDecimal total = b0_30.add(b31_60).add(b61_90).add(b90Plus);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectId", projectId);
        result.put("totalUnpaid", total);
        result.put("bucket0To30", b0_30);
        result.put("bucket31To60", b31_60);
        result.put("bucket61To90", b61_90);
        result.put("bucket90Plus", b90Plus);
        result.put("details", detail);
        return ApiResponse.ok(result);
    }

    @GetMapping("/receivable-followups")
    public ApiResponse<List<Map<String, Object>>> receivableFollowups() {
        String sql = "SELECT id, invoice_no, DATE_FORMAT(invoice_date, '%Y-%m-%d') invoice_date, customer_name, unpaid_amount FROM invoice WHERE unpaid_amount > 0 ORDER BY unpaid_amount DESC, invoice_date ASC";
        List<Map<String, Object>> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            String invoiceDate = rs.getString("invoice_date");
            LocalDate date = null;
            try {
                date = LocalDate.parse(invoiceDate);
            } catch (Exception ignored) {
            }
            long overdueDays = date == null ? 0 : java.time.temporal.ChronoUnit.DAYS.between(date, LocalDate.now());

            BigDecimal unpaid = rs.getBigDecimal("unpaid_amount");
            String priority;
            if (overdueDays > 90 || unpaid.compareTo(new BigDecimal("50000")) >= 0) {
                priority = "高";
            } else if (overdueDays > 45 || unpaid.compareTo(new BigDecimal("20000")) >= 0) {
                priority = "中";
            } else {
                priority = "低";
            }

            m.put("invoiceId", rs.getLong("id"));
            m.put("invoiceNo", rs.getString("invoice_no"));
            m.put("customerName", rs.getString("customer_name"));
            m.put("invoiceDate", invoiceDate);
            m.put("unpaidAmount", unpaid);
            m.put("overdueDays", Math.max(0, overdueDays));
            m.put("priority", priority);
            m.put("suggestion",
                    "高".equals(priority) ? "48小时内电话催收并发函确认回款计划" : ("中".equals(priority) ? "本周内完成催收沟通并登记结果" : "例行跟进"));
            return m;
        });
        return ApiResponse.ok(list);
    }
}
