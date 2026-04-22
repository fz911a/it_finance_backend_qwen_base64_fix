package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.dto.AiBookkeepingChatRequest;
import com.example.itfinance.dto.AiGenericResponse;
import com.example.itfinance.dto.AiInvoiceOcrRequest;
import com.example.itfinance.dto.AiReportSummaryRequest;
import com.example.itfinance.dto.RiskAnalyzeRequest;
import com.example.itfinance.dto.ReceiptOcrRequest;
import com.example.itfinance.service.AiService;
import com.example.itfinance.service.IdempotencyService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String key = "invoice:" + idempotencyKey;
            if (!idempotencyService.tryAcquire(key)) {
                return ApiResponse.fail("重复请求过于频繁，请稍后重试");
            }
        }
        return ApiResponse.ok(aiService.recognizeInvoice(request));
    }

    @PostMapping("/receipt/ocr")
    public ApiResponse<AiGenericResponse> receiptOcr(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ReceiptOcrRequest request) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String key = "receipt:" + idempotencyKey;
            if (!idempotencyService.tryAcquire(key)) {
                return ApiResponse.fail("重复请求过于频繁，请稍后重试");
            }
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
