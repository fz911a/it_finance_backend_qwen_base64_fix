package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.service.AuthService;
import com.example.itfinance.service.InvoiceService;
import com.example.itfinance.service.PaymentService;
import com.example.itfinance.entity.Invoice;
import com.example.itfinance.entity.Payment;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/export")
@CrossOrigin
public class ExportController {
    private final AuthService authService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    public ExportController(AuthService authService, InvoiceService invoiceService, PaymentService paymentService) {
        this.authService = authService;
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
    }

    @PostMapping("/csv")
    public void exportCsv(@RequestHeader("Authorization") String token, @RequestBody Map<String, Object> body,
            HttpServletResponse response) throws IOException {
        try {
            authService.getUserByToken(token);
            String type = (String) body.getOrDefault("type", "invoice");

            response.setContentType("text/csv; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + type + "_export.csv");

            // 写入BOM头，防止Excel中文乱码
            response.getOutputStream().write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });

            PrintWriter writer = new PrintWriter(response.getOutputStream(), false, StandardCharsets.UTF_8);

            if ("invoice".equalsIgnoreCase(type)) {
                writer.println("发票号,日期,金额,税额,客户,项目,状态,未回款");
                List<Invoice> list = invoiceService.list();
                for (Invoice inv : list) {
                    writer.printf("%s,%s,%s,%s,%s,%s,%s,%s\n",
                            inv.getInvoiceNo(), inv.getInvoiceDate(), inv.getAmount(),
                            inv.getTaxAmount(), inv.getCustomerName(), inv.getProjectName(),
                            inv.getStatus(), inv.getUnpaidAmount());
                }
            } else if ("payment".equalsIgnoreCase(type)) {
                writer.println("日期,金额,方式,项目,关联发票,备注");
                List<Payment> list = paymentService.list();
                for (Payment p : list) {
                    writer.printf("%s,%s,%s,%s,%s,%s\n",
                            p.getPaymentDate(), p.getAmount(), p.getMethod(),
                            p.getProjectName(), p.getInvoiceNos() != null ? p.getInvoiceNos() : "",
                            p.getRemark() != null ? p.getRemark() : "");
                }
            }
            writer.flush();
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    @PostMapping("/excel")
    public ApiResponse<Map<String, String>> exportExcel(@RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {
        try {
            authService.getUserByToken(token);
            Map<String, String> result = new HashMap<>();
            result.put("url", "https://example.com/export/excel.xlsx");
            result.put("message", "Excel导出功能目前通过CSV接口提供真实数据下载，此处仅供演示");
            return ApiResponse.ok(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/pdf")
    public ApiResponse<Map<String, String>> exportPdf(@RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {
        try {
            authService.getUserByToken(token);
            Map<String, String> result = new HashMap<>();
            result.put("url", "https://example.com/export/report.pdf");
            result.put("message", "PDF报表导出目前仅支持预览，如需下载请在CSV接口中选择");
            return ApiResponse.ok(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
