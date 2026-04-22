package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/report")
@CrossOrigin
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/project-profit")
    public ApiResponse<Map<String, Object>> projectProfit(@RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(reportService.profitReport(projectId, startDate, endDate));
    }

    @GetMapping("/trend")
    public ApiResponse<Map<String, Object>> getTrendData(@RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(reportService.getTrendData(projectId, startDate, endDate));
    }

    @GetMapping("/category")
    public ApiResponse<Map<String, Object>> getCategoryData(@RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(reportService.getCategoryData(projectId, startDate, endDate));
    }

    @GetMapping("/monthly-comparison")
    public ApiResponse<Map<String, Object>> getMonthlyComparisonData(@RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String year) {
        return ApiResponse.ok(reportService.getMonthlyComparisonData(projectId, year));
    }

    @GetMapping("/export/{format}")
    public void exportReport(@PathVariable String format,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        byte[] data = reportService.exportReport(format, projectId, startDate, endDate);
        String ext = format.toLowerCase().equals("excel") ? "xlsx" : format.toLowerCase();
        String filename = "report_" + System.currentTimeMillis() + "." + ext;

        response.setContentType(getContentType(format));
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.getOutputStream().write(data);
    }

    private String getContentType(String format) {
        return switch (format.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "excel", "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "csv" -> "text/csv";
            default -> "application/octet-stream";
        };
    }
}
