package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.dto.*;
import com.example.itfinance.service.AiService;
import com.example.itfinance.service.IdempotencyService;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AiController {
    private final AiService aiService;
    private final IdempotencyService idempotencyService;

    public AiController(AiService aiService, IdempotencyService idempotencyService) {
        this.aiService = aiService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping("/health")
    public ApiResponse<AiGenericResponse> health() {
        return ApiResponse.ok(aiService.healthCheck());
    }

    @PostMapping("/invoice/ocr")
    public ApiResponse<AiGenericResponse> invoiceOcr(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody AiInvoiceOcrRequest request) {
        String fallback = "invoice:auto:"
                + Objects.hash(request.getFileUrl(), request.getFileBase64(), request.getFileType());
        String key = (idempotencyKey == null || idempotencyKey.isBlank()) ? fallback : "invoice:" + idempotencyKey;
        if (!idempotencyService.tryAcquire(key)) {
            return ApiResponse.fail("重复请求过于频繁，请稍后重试");
        }
        return ApiResponse.ok(aiService.recognizeInvoice(request));
    }

    @PostMapping("/receipt/ocr")
    public ApiResponse<AiGenericResponse> receiptOcr(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ReceiptOcrRequest request) {
        String fallback = "receipt:auto:" + Objects.hash(request.getFileUrl(), request.getProjectId());
        String key = (idempotencyKey == null || idempotencyKey.isBlank()) ? fallback : "receipt:" + idempotencyKey;
        if (!idempotencyService.tryAcquire(key)) {
            return ApiResponse.fail("重复请求过于频繁，请稍后重试");
        }
        return ApiResponse.ok(aiService.recognizeReceipt(request));
    }

    @PostMapping("/bookkeeping/chat")
    public ApiResponse<AiGenericResponse> bookkeepingChat(@RequestBody AiBookkeepingChatRequest request) {
        return ApiResponse.ok(aiService.bookkeepingChat(request));
    }

    @PostMapping("/report/summary")
    public ApiResponse<AiGenericResponse> reportSummary(@RequestBody AiReportSummaryRequest request) {
        return ApiResponse.ok(aiService.reportSummary(request));
    }

    @PostMapping("/risk/analyze")
    public ApiResponse<AiGenericResponse> riskAnalyze(@RequestBody RiskAnalyzeRequest request) {
        return ApiResponse.ok(aiService.analyzeRisk(request));
    }
}
