package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.dto.ExpenseConfirmFromOcrRequest;
import com.example.itfinance.entity.ExpenseRecord;
import com.example.itfinance.service.AuditLogService;
import com.example.itfinance.service.ExpenseService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expense")
@CrossOrigin
public class ExpenseController {
    private final ExpenseService expenseService;
    private final AuditLogService auditLogService;
    private final JdbcTemplate jdbcTemplate;

    public ExpenseController(ExpenseService expenseService, AuditLogService auditLogService,
            JdbcTemplate jdbcTemplate) {
        this.expenseService = expenseService;
        this.auditLogService = auditLogService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/list")
    public ApiResponse<List<ExpenseRecord>> list() {
        return ApiResponse.ok(expenseService.list());
    }

    @GetMapping("/detail/{id}")
    public ApiResponse<ExpenseRecord> detail(@PathVariable Long id) {
        return ApiResponse.ok(expenseService.getById(id));
    }

    @PostMapping("/add")
    public ApiResponse<ExpenseRecord> add(@RequestBody ExpenseRecord record) {
        ExpenseRecord created = expenseService.create(record);
        auditLogService.log("expense", "add", "新增费用单 id=" + (created == null ? null : created.getId()));
        return ApiResponse.ok("新增成功", created);
    }

    @PostMapping("/update-status/{id}")
    public ApiResponse<ExpenseRecord> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "待审核");
        ExpenseRecord updated = expenseService.updateStatus(id, status);
        auditLogService.log("expense", "update-status", "更新费用单状态 id=" + id + ", status=" + status);
        return ApiResponse.ok("状态更新成功", updated);
    }

    @GetMapping("/approval/pending")
    public ApiResponse<List<ExpenseRecord>> approvalPending() {
        List<ExpenseRecord> list = jdbcTemplate.query(
                "SELECT id, project_id, employee_id, expense_type, amount, DATE_FORMAT(expense_date, '%Y-%m-%d') expense_date, merchant_name, receipt_url, ai_summary, status "
                        + "FROM expense_record WHERE status IN ('待提交','待审核') ORDER BY id DESC",
                (rs, rowNum) -> new ExpenseRecord(
                        rs.getLong("id"),
                        rs.getLong("project_id"),
                        rs.getLong("employee_id"),
                        rs.getString("expense_type"),
                        rs.getBigDecimal("amount"),
                        rs.getString("expense_date"),
                        rs.getString("merchant_name"),
                        rs.getString("receipt_url"),
                        rs.getString("ai_summary"),
                        rs.getString("status")));
        return ApiResponse.ok(list);
    }

    @PostMapping("/approval/submit/{id}")
    public ApiResponse<ExpenseRecord> submitApproval(@PathVariable Long id) {
        ExpenseRecord record = expenseService.getById(id);
        if (record == null) {
            return ApiResponse.fail("费用单不存在");
        }
        if (!("待提交".equals(record.getStatus()) || "已驳回".equals(record.getStatus()))) {
            return ApiResponse.fail("当前状态不允许提交审批：" + record.getStatus());
        }
        ExpenseRecord updated = expenseService.updateStatus(id, "待审核");
        auditLogService.log("expense", "approval-submit", "提交审批 expenseId=" + id);
        return ApiResponse.ok("提交审批成功", updated);
    }

    @PostMapping("/approval/review/{id}")
    public ApiResponse<Map<String, Object>> reviewApproval(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        ExpenseRecord record = expenseService.getById(id);
        if (record == null) {
            return ApiResponse.fail("费用单不存在");
        }
        if (!"待审核".equals(record.getStatus())) {
            return ApiResponse.fail("当前状态不可审批：" + record.getStatus());
        }

        String action = body.getOrDefault("action", "approve").trim().toLowerCase();
        String comment = body.getOrDefault("comment", "");
        String targetStatus;
        if ("approve".equals(action) || "pass".equals(action)) {
            targetStatus = "已通过";
        } else if ("reject".equals(action) || "refuse".equals(action)) {
            targetStatus = "已驳回";
        } else {
            return ApiResponse.fail("action 仅支持 approve/reject");
        }

        ExpenseRecord updated = expenseService.updateStatus(id, targetStatus);
        auditLogService.log("expense", "approval-review",
                "审批费用单 expenseId=" + id + ", action=" + action + ", comment=" + comment);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expenseId", id);
        result.put("status", targetStatus);
        result.put("comment", comment);
        result.put("record", updated);
        return ApiResponse.ok("审批处理成功", result);
    }

    @PostMapping("/confirm-from-ocr")
    public ApiResponse<ExpenseRecord> confirmFromOcr(@RequestBody ExpenseConfirmFromOcrRequest request) {
        if (request.getProjectId() == null || request.getAmount() == null
                || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.fail("projectId 和 amount 必填，且 amount 必须大于 0");
        }

        ExpenseRecord record = new ExpenseRecord();
        record.setProjectId(request.getProjectId());
        record.setEmployeeId(request.getEmployeeId() == null ? 1L : request.getEmployeeId());
        record.setExpenseType(request.getExpenseType() == null || request.getExpenseType().isBlank() ? "其他"
                : request.getExpenseType());
        record.setAmount(request.getAmount());
        record.setExpenseDate(request.getExpenseDate());
        record.setMerchantName(request.getMerchantName());
        record.setReceiptUrl(request.getReceiptUrl());
        record.setAiSummary(request.getAiSummary());
        record.setStatus("待审核");

        ExpenseRecord created = expenseService.create(record);
        String rid = request.getRecognitionId() == null ? "-" : String.valueOf(request.getRecognitionId());
        auditLogService.log("expense", "confirm-from-ocr",
                "识别确认入账 recognitionId=" + rid + ", expenseId=" + (created == null ? null : created.getId()));
        return ApiResponse.ok("识别结果已确认并生成费用单", created);
    }
}
