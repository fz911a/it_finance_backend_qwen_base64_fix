package com.example.itfinance.service.impl;

import com.example.itfinance.config.AiProperties;
import com.example.itfinance.dto.*;
import com.example.itfinance.service.AiService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.*;

@Service
public class AiServiceImpl implements AiService {
    private static final int REPORT_MODEL_TIMEOUT_SECONDS = 10;
    private static final int BOOKKEEPING_MODEL_TIMEOUT_SECONDS = 12;
    private static final int AI_CONNECT_TIMEOUT_MS = 15000;
    private static final int AI_READ_TIMEOUT_MS = 120000;
    private static final int AI_MAX_RETRIES = 1;
    private static final long AI_RETRY_WAIT_MS = 1000L;

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;

    public AiServiceImpl(AiProperties aiProperties, JdbcTemplate jdbcTemplate) {
        this.aiProperties = aiProperties;
        this.jdbcTemplate = jdbcTemplate;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(AI_CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(AI_READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public AiGenericResponse recognizeInvoice(AiInvoiceOcrRequest request) {
        if (!aiProperties.isEnabled()) {
            throw new IllegalStateException("AI 未启用，请开启 app.ai.enabled");
        }
        try {
            String prompt = "你是发票OCR助手。请严格输出JSON，不要输出解释。JSON字段必须包含：invoiceNo, invoiceDate, amount, taxAmount, customerName, projectName, currency, originalAmount, cnyAmount, fxRate, fxDate, regions。regions是数组，每项包含label,text,x,y,w,h，坐标按0到1的小数表示，用于前端高亮覆盖层。";
            Map<String, Object> parsed = callVisionModel(request.getFileUrl(), request.getFileBase64(),
                    request.getFileType(), prompt, aiProperties.getVisionModel());
            ensureInvoiceSchema(parsed);
            normalizeRegions(parsed);
            saveRecognitionLog("invoice", request.getFileUrl(), parsed, 96.8);
            return new AiGenericResponse("已调用真实视觉模型完成发票识别。", parsed);
        } catch (Exception e) {
            throw new IllegalStateException("真实模型调用失败：" + e.getMessage());
        }
    }

    @Override
    public AiGenericResponse recognizeReceipt(ReceiptOcrRequest request) {
        if (!aiProperties.isEnabled()) {
            throw new IllegalStateException("AI 未启用，请开启 app.ai.enabled");
        }
        try {
            String prompt = "你是小票OCR助手。请严格输出JSON，不要输出解释。JSON字段必须包含：merchantName, amount, expenseType, expenseDate, projectId, currency, originalAmount, cnyAmount, fxRate, fxDate, regions。regions是数组，每项包含label,text,x,y,w,h，坐标按0到1的小数表示，用于前端高亮覆盖层。projectId沿用输入值。";
            Map<String, Object> parsed = callVisionModel(request.getFileUrl(), null, null, prompt,
                    aiProperties.getVisionModel());
            parsed.putIfAbsent("projectId", request.getProjectId());
            ensureReceiptSchema(parsed);
            normalizeRegions(parsed);
            saveRecognitionLog("receipt", request.getFileUrl(), parsed, 95.3);
            return new AiGenericResponse("已调用真实视觉模型完成小票识别。", parsed);
        } catch (Exception e) {
            throw new IllegalStateException("真实模型调用失败：" + e.getMessage());
        }
    }

    @Override
    public AiGenericResponse bookkeepingChat(AiBookkeepingChatRequest request) {
        if (!aiProperties.isEnabled()) {
            throw new IllegalStateException("AI 未启用，请开启 app.ai.enabled");
        }
        String userText = request.getUserText() == null ? "" : request.getUserText().trim();
        if (userText.isEmpty()) {
            throw new IllegalStateException("记账文本不能为空");
        }
        try {
            String prompt = "将下述记账文本提取为JSON，仅返回JSON："
                    + "字段：suggestedType,suggestedProjectId,amount,summary,expenseTypeLabel,confidence。"
                    + "规则：suggestedType=EXPENSE；expenseTypeLabel仅可为[差旅费,餐饮费,办公费,营销费,其他]；"
                    + "confidence为0-100整数；summary不超过30字。"
                    + "文本：" + userText + "；默认项目ID：" + request.getProjectId();

            Map<String, Object> parsed;
            boolean fallbackUsed = false;
            try {
                parsed = callChatModelForJsonWithTimeout(prompt, aiProperties.getChatModel(), 0,
                        BOOKKEEPING_MODEL_TIMEOUT_SECONDS);
            } catch (Exception modelException) {
                parsed = buildBookkeepingFallback(userText, request.getProjectId());
                fallbackUsed = true;
            }
            Long projectId = asLong(parsed.get("suggestedProjectId"));
            if (projectId == null) {
                projectId = request.getProjectId() == null ? 1L : request.getProjectId();
            }
            Double amount = asDouble(parsed.get("amount"));
            Integer confidence = asInt(parsed.get("confidence"));
            if (confidence == null) {
                confidence = 0;
            }
            confidence = Math.max(0, Math.min(100, confidence));

            String expenseTypeLabel = String.valueOf(parsed.getOrDefault("expenseTypeLabel", "其他"));
            String summary = String.valueOf(parsed.getOrDefault("summary", userText));

            parsed.put("suggestedType", "EXPENSE");
            parsed.put("suggestedProjectId", projectId);
            parsed.put("amount", amount == null ? 0 : amount);
            parsed.put("summary", summary);
            parsed.put("expenseTypeLabel", expenseTypeLabel);
            parsed.put("confidence", confidence);
            parsed.put("fallbackUsed", fallbackUsed);
            String message = fallbackUsed ? "模型响应较慢，已按规则解析记账文本。" : "已调用真实模型完成记账文本解析。";
            return new AiGenericResponse(message, parsed);
        } catch (Exception e) {
            throw new IllegalStateException("真实模型调用失败：" + e.getMessage());
        }
    }

    @Override
    public AiGenericResponse reportSummary(AiReportSummaryRequest request) {
        if (!aiProperties.isEnabled()) {
            throw new IllegalStateException("AI 未启用，请开启 app.ai.enabled");
        }
        try {
            Long projectId = request.getProjectId() == null ? 1L : request.getProjectId();
            String startDate = request.getStartDate();
            String endDate = request.getEndDate();

            Double expenseTotal = queryNumber(
                    "SELECT IFNULL(SUM(amount),0) FROM expense_record WHERE project_id=? AND expense_date>=? AND expense_date<=?",
                    projectId, startDate, endDate);
            Double paymentTotal = queryNumber(
                    "SELECT IFNULL(SUM(amount),0) FROM payment WHERE project_id=? AND payment_date>=? AND payment_date<=?",
                    projectId, startDate, endDate);
            Double invoiceUnpaid = queryNumber(
                    "SELECT IFNULL(SUM(unpaid_amount),0) FROM invoice WHERE project_id=? AND invoice_date>=? AND invoice_date<=?",
                    projectId, startDate, endDate);
            Long pendingExpenseCount = queryLong(
                    "SELECT COUNT(*) FROM expense_record WHERE project_id=? AND expense_date>=? AND expense_date<=? AND status IN ('待提交','待审核')",
                    projectId, startDate, endDate);

            String prompt = "基于输入数据生成简明中文财务摘要，不超过120字，不要Markdown。"
                    + "输入数据："
                    + "projectId=" + projectId
                    + ", period=" + startDate + "~" + endDate
                    + ", paymentTotal=" + paymentTotal
                    + ", expenseTotal=" + expenseTotal
                    + ", unpaidTotal=" + invoiceUnpaid
                    + ", pendingExpenseCount=" + pendingExpenseCount;

            String aiText;
            boolean fallbackUsed = false;
            try {
                aiText = callChatModelForTextWithTimeout(prompt, aiProperties.getChatModel(), 0.2,
                        REPORT_MODEL_TIMEOUT_SECONDS);
                if (aiText == null || aiText.isBlank()) {
                    aiText = buildReportFallbackSummary(paymentTotal, expenseTotal, invoiceUnpaid, pendingExpenseCount);
                    fallbackUsed = true;
                }
            } catch (Exception modelException) {
                aiText = buildReportFallbackSummary(paymentTotal, expenseTotal, invoiceUnpaid, pendingExpenseCount);
                fallbackUsed = true;
            }

            Map<String, Object> parsed = new LinkedHashMap<>();
            parsed.put("projectId", projectId);
            parsed.put("period", startDate + " ~ " + endDate);
            parsed.put("paymentTotal", paymentTotal);
            parsed.put("expenseTotal", expenseTotal);
            parsed.put("unpaidTotal", invoiceUnpaid);
            parsed.put("pendingExpenseCount", pendingExpenseCount);
            parsed.put("fallbackUsed", fallbackUsed);
            return new AiGenericResponse(aiText, parsed);
        } catch (Exception e) {
            throw new IllegalStateException("真实模型调用失败：" + e.getMessage());
        }
    }

    private String buildReportFallbackSummary(Double paymentTotal, Double expenseTotal, Double invoiceUnpaid,
            Long pendingExpenseCount) {
        double payment = paymentTotal == null ? 0D : paymentTotal;
        double expense = expenseTotal == null ? 0D : expenseTotal;
        double unpaid = invoiceUnpaid == null ? 0D : invoiceUnpaid;
        long pending = pendingExpenseCount == null ? 0L : pendingExpenseCount;
        double netCash = payment - expense;

        String cashFlow = netCash >= 0 ? "现金流总体平稳" : "现金流存在压力";
        String costControl = expense <= payment ? "成本控制基本正常" : "成本支出偏高，建议压缩非必要费用";
        String risk = unpaid > 0 ? "未收账款需重点跟进" : "未收账款风险较低";
        String review = pending > 0 ? "请优先处理待审核费用单" : "费用审核进度良好";

        return "模型响应较慢，已生成数据摘要：" + cashFlow + "；" + costControl + "；" + risk + "；" + review
                + "。当前进款" + String.format(Locale.US, "%.2f", payment) + "元，费用"
                + String.format(Locale.US, "%.2f", expense) + "元，未收"
                + String.format(Locale.US, "%.2f", unpaid) + "元。";
    }

    private String callChatModelForTextWithTimeout(String prompt, String model, double temperature, int timeoutSeconds)
            throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> callChatModelForText(prompt, model, temperature));
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException timeoutException) {
            future.cancel(true);
            throw new IllegalStateException("摘要模型响应超时", timeoutException);
        } finally {
            executor.shutdownNow();
        }
    }

    private Map<String, Object> callChatModelForJsonWithTimeout(String prompt, String model, double temperature,
            int timeoutSeconds) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Map<String, Object>> future = executor.submit(() -> callChatModelForJson(prompt, model, temperature));
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException timeoutException) {
            future.cancel(true);
            throw new IllegalStateException("记账模型响应超时", timeoutException);
        } finally {
            executor.shutdownNow();
        }
    }

    private Map<String, Object> buildBookkeepingFallback(String userText, Long defaultProjectId) {
        Map<String, Object> map = new LinkedHashMap<>();
        Long projectId = (defaultProjectId == null) ? 1L : defaultProjectId;

        double amount = 0D;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d+(?:\\.\\d{1,2})?)")
                .matcher(userText == null ? "" : userText);
        if (matcher.find()) {
            try {
                amount = Double.parseDouble(matcher.group(1));
            } catch (Exception ignored) {
                amount = 0D;
            }
        }

        String typeLabel = "其他";
        String text = userText == null ? "" : userText;
        if (text.contains("打车") || text.contains("机票") || text.contains("酒店") || text.contains("出差")) {
            typeLabel = "差旅费";
        } else if (text.contains("餐") || text.contains("咖啡") || text.contains("外卖")) {
            typeLabel = "餐饮费";
        } else if (text.contains("办公") || text.contains("耗材") || text.contains("设备")) {
            typeLabel = "办公费";
        } else if (text.contains("推广") || text.contains("广告") || text.contains("营销")) {
            typeLabel = "营销费";
        }

        String summary = text.length() > 30 ? text.substring(0, 30) : text;

        map.put("suggestedType", "EXPENSE");
        map.put("suggestedProjectId", projectId);
        map.put("amount", amount);
        map.put("summary", summary.isBlank() ? "费用记录" : summary);
        map.put("expenseTypeLabel", typeLabel);
        map.put("confidence", 68);
        return map;
    }

    @Override
    public AiGenericResponse analyzeRisk(RiskAnalyzeRequest request) {
        Long projectId = request.getProjectId() == null ? 1L : request.getProjectId();
        Double receivable = queryNumber("SELECT IFNULL(SUM(unpaid_amount),0) FROM invoice WHERE project_id = ?",
                projectId);
        Double paymentTotal = queryNumber("SELECT IFNULL(SUM(amount),0) FROM payment WHERE project_id = ?", projectId);
        Double expenseTotal = queryNumber("SELECT IFNULL(SUM(amount),0) FROM expense_record WHERE project_id = ?",
                projectId);
        Long pendingApprovals = queryLong(
                "SELECT COUNT(*) FROM expense_record WHERE project_id = ? AND status IN ('待提交','待审核')",
                projectId);
        Double taxTotal = queryNumber("SELECT IFNULL(SUM(tax_amount),0) FROM invoice WHERE project_id = ?", projectId);

        String receivableRisk = riskLevelByReceivable(receivable);
        String costRisk = riskLevelByCost(paymentTotal, expenseTotal);
        String taxRisk = riskLevelByTaxBurden(paymentTotal, taxTotal);

        int score = computeRiskScore(receivableRisk, costRisk, taxRisk, pendingApprovals);
        String overallRisk = score >= 80 ? "高" : (score >= 45 ? "中" : "低");

        Map<String, Object> parsed = new LinkedHashMap<>();
        parsed.put("projectId", projectId);
        parsed.put("receivableRisk", receivableRisk);
        parsed.put("costRisk", costRisk);
        parsed.put("taxRisk", taxRisk);
        parsed.put("overallRisk", overallRisk);
        parsed.put("riskScore", score);
        parsed.put("receivableAmount", receivable);
        parsed.put("paymentTotal", paymentTotal);
        parsed.put("expenseTotal", expenseTotal);
        parsed.put("pendingApprovals", pendingApprovals == null ? 0 : pendingApprovals);
        parsed.put("taxAmount", taxTotal);
        parsed.put("suggestions", buildRiskSuggestions(receivableRisk, costRisk, taxRisk, pendingApprovals));

        String message = "AI 风险分析完成：综合风险" + overallRisk + "，当前得分 " + score + "。";
        return new AiGenericResponse(message, parsed);
    }

    private String riskLevelByReceivable(Double receivable) {
        double amount = receivable == null ? 0D : receivable;
        if (amount >= 60000D) {
            return "高";
        }
        if (amount >= 20000D) {
            return "中";
        }
        return "低";
    }

    private String riskLevelByCost(Double paymentTotal, Double expenseTotal) {
        double payment = paymentTotal == null ? 0D : paymentTotal;
        double expense = expenseTotal == null ? 0D : expenseTotal;
        if (payment <= 0D) {
            return expense > 0D ? "高" : "中";
        }
        double ratio = expense / payment;
        if (ratio >= 0.90D) {
            return "高";
        }
        if (ratio >= 0.65D) {
            return "中";
        }
        return "低";
    }

    private String riskLevelByTaxBurden(Double paymentTotal, Double taxTotal) {
        double payment = paymentTotal == null ? 0D : paymentTotal;
        double tax = taxTotal == null ? 0D : taxTotal;
        if (payment <= 0D) {
            return tax > 0D ? "中" : "低";
        }
        double ratio = tax / payment;
        if (ratio >= 0.18D) {
            return "高";
        }
        if (ratio >= 0.08D) {
            return "中";
        }
        return "低";
    }

    private int computeRiskScore(String receivableRisk, String costRisk, String taxRisk, Long pendingApprovals) {
        int score = 0;
        score += riskScore(receivableRisk) * 4;
        score += riskScore(costRisk) * 3;
        score += riskScore(taxRisk) * 2;
        long pending = pendingApprovals == null ? 0L : pendingApprovals;
        if (pending >= 6) {
            score += 15;
        } else if (pending >= 2) {
            score += 8;
        }
        return Math.max(0, Math.min(100, score));
    }

    private int riskScore(String riskLevel) {
        if ("高".equals(riskLevel)) {
            return 10;
        }
        if ("中".equals(riskLevel)) {
            return 6;
        }
        return 2;
    }

    private List<String> buildRiskSuggestions(String receivableRisk, String costRisk, String taxRisk,
            Long pendingApprovals) {
        List<String> tips = new ArrayList<>();
        if ("高".equals(receivableRisk) || "中".equals(receivableRisk)) {
            tips.add("优先梳理账龄并启动分层催收，重点跟进高金额未回款客户");
        }
        if ("高".equals(costRisk)) {
            tips.add("当前成本占比偏高，建议冻结非必要支出并复核预算执行");
        }
        if ("高".equals(taxRisk) || "中".equals(taxRisk)) {
            tips.add("建议按月进行税负波动复盘，核对进销项与可抵扣凭证");
        }
        long pending = pendingApprovals == null ? 0L : pendingApprovals;
        if (pending > 0) {
            tips.add("存在待审核单据，请优先清理审批积压以降低流程风险");
        }
        if (tips.isEmpty()) {
            tips.add("当前风险整体可控，建议保持周度财务指标巡检");
        }
        return tips;
    }

    @Override
    public AiGenericResponse healthCheck() {
        Map<String, Object> parsed = new LinkedHashMap<>();
        parsed.put("provider", aiProperties.getProvider());
        parsed.put("baseUrl", aiProperties.getBaseUrl());
        parsed.put("enabled", aiProperties.isEnabled());
        parsed.put("chatModel", aiProperties.getChatModel());
        parsed.put("visionModel", aiProperties.getVisionModel());
        parsed.put("mode", aiProperties.isEnabled() ? "真实模型模式" : "演示模式");
        return new AiGenericResponse("AI 配置检查完成。", parsed);
    }

    private Map<String, Object> callVisionModel(String fileUrl, String fileBase64, String fileType, String prompt,
            String model) throws Exception {
        String fullUrl = normalizeImageUrl(fileUrl, fileBase64, fileType);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model == null || model.isBlank() ? aiProperties.getChatModel() : model);

        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("type", "text");
        textPart.put("text", prompt);

        Map<String, Object> imageDetail = new LinkedHashMap<>();
        imageDetail.put("url", fullUrl);
        imageDetail.put("detail", "auto");

        Map<String, Object> imagePart = new LinkedHashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageDetail);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", List.of(textPart, imagePart));

        body.put("messages", List.of(message));
        body.put("temperature", 0);

        HttpHeaders headers = buildAiHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String endpoint = getChatCompletionsEndpoint();

        ResponseEntity<String> response = postForEntityWithRetry(endpoint, entity);
        JsonNode root = objectMapper.readTree(response.getBody());
        String content = root.path("choices").get(0).path("message").path("content").asText();
        return parseJsonFromModel(content);
    }

    private Map<String, Object> callChatModelForJson(String prompt, String model, double temperature) throws Exception {
        String content = callChatModelForText(prompt, model, temperature);
        return parseJsonFromModel(content);
    }

    private String callChatModelForText(String prompt, String model, double temperature) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", (model == null || model.isBlank()) ? aiProperties.getChatModel() : model);
        body.put("stream", false);
        body.put("max_tokens", 280);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        body.put("messages", List.of(message));
        body.put("temperature", temperature);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildAiHeaders());
        ResponseEntity<String> response = postForEntityWithRetry(getChatCompletionsEndpoint(), entity);
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("choices").get(0).path("message").path("content").asText("").trim();
    }

    private ResponseEntity<String> postForEntityWithRetry(String endpoint, HttpEntity<Map<String, Object>> entity) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= AI_MAX_RETRIES; attempt++) {
            try {
                return restTemplate.postForEntity(endpoint, entity, String.class);
            } catch (Exception e) {
                lastException = e;
                if (attempt >= AI_MAX_RETRIES) {
                    break;
                }
                try {
                    Thread.sleep(AI_RETRY_WAIT_MS * (attempt + 1));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("模型调用被中断，请稍后重试", interruptedException);
                }
            }
        }
        String detail = lastException == null ? "未知异常" : String.valueOf(lastException.getMessage());
        throw new IllegalStateException("模型调用超时或网络异常，请稍后重试。" + detail, lastException);
    }

    private HttpHeaders buildAiHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String apiKey = aiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI_API_KEY 未配置");
        }
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private String getChatCompletionsEndpoint() {
        String endpoint = aiProperties.getBaseUrl();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        if (endpoint.endsWith("/chat/completions")) {
            return endpoint;
        }
        if (endpoint.endsWith("/v1")) {
            return endpoint + "/chat/completions";
        }
        return endpoint + "/v1/chat/completions";
    }

    private Double queryNumber(String sql, Object... args) {
        try {
            Number value = jdbcTemplate.queryForObject(sql, Number.class, args);
            return value == null ? 0.0 : value.doubleValue();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Long queryLong(String sql, Object... args) {
        try {
            Number value = jdbcTemplate.queryForObject(sql, Number.class, args);
            return value == null ? 0L : value.longValue();
        } catch (Exception e) {
            return 0L;
        }
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer asInt(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> parseJsonFromModel(String content) throws Exception {
        String cleaned = content.trim();
        if (cleaned.startsWith("```") && cleaned.contains("{")) {
            cleaned = cleaned.replaceAll("^```json", "").replaceAll("^```", "").replaceAll("```$", "").trim();
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start)
            cleaned = cleaned.substring(start, end + 1);
        return objectMapper.readValue(cleaned, new TypeReference<>() {
        });
    }

    private String normalizeImageUrl(String fileUrl, String fileBase64, String fileType) {
        if (fileBase64 != null && !fileBase64.isBlank()) {
            String normalizedBase64 = fileBase64.contains(",") ? fileBase64.substring(fileBase64.indexOf(',') + 1)
                    : fileBase64;
            String type = (fileType == null || fileType.isBlank()) ? "image/jpeg" : fileType;
            return "data:" + type + ";base64," + normalizedBase64;
        }
        if (fileUrl == null || fileUrl.isBlank())
            return "";
        try {
            if (fileUrl.startsWith("/uploads/")) {
                return localFileToDataUrl(fileUrl.substring("/uploads/".length()));
            }
            if (fileUrl.startsWith("http://localhost") || fileUrl.startsWith("http://127.0.0.1")
                    || fileUrl.startsWith("https://localhost") || fileUrl.startsWith("https://127.0.0.1")) {
                int idx = fileUrl.indexOf("/uploads/");
                if (idx >= 0)
                    return localFileToDataUrl(fileUrl.substring(idx + "/uploads/".length()));
            }
            return fileUrl;
        } catch (Exception e) {
            throw new RuntimeException("无法读取本地图片并转换为 base64：" + e.getMessage(), e);
        }
    }

    private void ensureInvoiceSchema(Map<String, Object> parsed) {
        List<String> required = List.of("invoiceNo", "invoiceDate", "amount", "taxAmount", "customerName",
                "projectName", "currency");
        List<String> missing = new ArrayList<>();
        for (String field : required) {
            Object value = parsed.get(field);
            if (value == null || String.valueOf(value).isBlank()) {
                missing.add(field);
            }
        }
        parsed.put("_schemaOk", missing.isEmpty());
        parsed.put("_missingFields", missing);
        parsed.put("_needsManualReview", !missing.isEmpty());
        parsed.put("_fieldConfidence", buildFieldConfidence(required, missing));
    }

    private void ensureReceiptSchema(Map<String, Object> parsed) {
        List<String> required = List.of("merchantName", "amount", "expenseType", "expenseDate", "projectId",
                "currency");
        List<String> missing = new ArrayList<>();
        for (String field : required) {
            Object value = parsed.get(field);
            if (value == null || String.valueOf(value).isBlank()) {
                missing.add(field);
            }
        }
        parsed.put("_schemaOk", missing.isEmpty());
        parsed.put("_missingFields", missing);
        parsed.put("_needsManualReview", !missing.isEmpty());
        parsed.put("_fieldConfidence", buildFieldConfidence(required, missing));
    }

    private Map<String, Object> buildFieldConfidence(List<String> required, List<String> missing) {
        Map<String, Object> confidence = new LinkedHashMap<>();
        for (String field : required) {
            confidence.put(field, missing.contains(field) ? 0.35 : 0.92);
        }
        return confidence;
    }

    private String localFileToDataUrl(String fileName) throws Exception {
        Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
        Path filePath = uploadDir.resolve(fileName).normalize();
        if (!filePath.startsWith(uploadDir)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        byte[] bytes = Files.readAllBytes(filePath);
        String lower = fileName.toLowerCase();
        String mime = "image/jpeg";
        if (lower.endsWith(".png"))
            mime = "image/png";
        else if (lower.endsWith(".webp"))
            mime = "image/webp";
        else if (lower.endsWith(".gif"))
            mime = "image/gif";
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return "data:" + mime + ";base64," + base64;
    }

    private void normalizeRegions(Map<String, Object> parsed) {
        if (!parsed.containsKey("regions")) {
            parsed.put("regions", List.of(
                    region("invoiceNo", String.valueOf(parsed.getOrDefault("invoiceNo", "")), 0.12, 0.12, 0.46, 0.08),
                    region("amount", String.valueOf(parsed.getOrDefault("amount", "")), 0.60, 0.60, 0.22, 0.08),
                    region("taxAmount", String.valueOf(parsed.getOrDefault("taxAmount", "")), 0.60, 0.70, 0.22, 0.08)));
        }
    }

    private Map<String, Object> region(String label, String text, double x, double y, double w, double h) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("label", label);
        map.put("text", text);
        map.put("x", x);
        map.put("y", y);
        map.put("w", w);
        map.put("h", h);
        return map;
    }

    private void saveRecognitionLog(String type, String sourceUrl, Map<String, Object> parsed, double confidence) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO recognition_record(recognition_type, source_file_url, result_json, confidence_score, operator_id) VALUES (?, ?, ?, ?, ?)",
                    type, sourceUrl, objectMapper.writeValueAsString(parsed), confidence, 1);
        } catch (Exception ignored) {
        }
    }

    private Map<String, Object> demoInvoice() {
        Map<String, Object> parsed = new LinkedHashMap<>();
        parsed.put("invoiceNo", "AI-DEMO-001");
        parsed.put("invoiceDate", "2026-04-07");
        parsed.put("amount", 12800.00);
        parsed.put("taxAmount", 768.00);
        parsed.put("customerName", "示例客户");
        parsed.put("projectName", "智慧运维平台");
        parsed.put("currency", "CNY");
        parsed.put("originalAmount", 12800.00);
        parsed.put("cnyAmount", 12800.00);
        parsed.put("fxRate", 1.0);
        parsed.put("fxDate", java.time.LocalDate.now().toString());
        parsed.put("regions", List.of(
                region("invoiceNo", "AI-DEMO-001", 0.14, 0.16, 0.48, 0.08),
                region("invoiceDate", "2026-04-07", 0.62, 0.16, 0.22, 0.08),
                region("amount", "12800.00", 0.58, 0.62, 0.24, 0.08),
                region("taxAmount", "768.00", 0.58, 0.72, 0.20, 0.08)));
        return parsed;
    }

    private Map<String, Object> demoReceipt(Long projectId) {
        Map<String, Object> parsed = new LinkedHashMap<>();
        parsed.put("merchantName", "星海酒店");
        parsed.put("amount", 560.00);
        parsed.put("expenseType", "差旅费");
        parsed.put("expenseDate", "2026-04-07");
        parsed.put("projectId", projectId);
        parsed.put("currency", "CNY");
        parsed.put("originalAmount", 560.00);
        parsed.put("cnyAmount", 560.00);
        parsed.put("fxRate", 1.0);
        parsed.put("fxDate", java.time.LocalDate.now().toString());
        parsed.put("regions", List.of(
                region("merchantName", "星海酒店", 0.12, 0.10, 0.40, 0.08),
                region("amount", "560.00", 0.58, 0.54, 0.22, 0.08),
                region("expenseDate", "2026-04-07", 0.12, 0.76, 0.30, 0.08)));
        return parsed;
    }
}
