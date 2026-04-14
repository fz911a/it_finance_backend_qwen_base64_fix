package com.example.itfinance.service;

import com.example.itfinance.dto.*;

public interface AiService {
    AiGenericResponse recognizeInvoice(AiInvoiceOcrRequest request);
    AiGenericResponse recognizeReceipt(ReceiptOcrRequest request);
    AiGenericResponse bookkeepingChat(AiBookkeepingChatRequest request);
    AiGenericResponse reportSummary(AiReportSummaryRequest request);
    AiGenericResponse analyzeRisk(RiskAnalyzeRequest request);
    AiGenericResponse healthCheck();
}
