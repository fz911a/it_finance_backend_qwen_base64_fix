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
    public ApiResponse<Map<String, Object>> getTrendData(@RequestParam(required = false) String startDate,
                                                         @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(reportService.getTrendData(startDate, endDate));
    }

    @GetMapping("/category")
    public ApiResponse<Map<String, Object>> getCategoryData(@RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(reportService.getCategoryData(startDate, endDate));
    }

    @GetMapping("/monthly-comparison")
    public ApiResponse<Map<String, Object>> getMonthlyComparisonData(@RequestParam(required = false) String year) {
        return ApiResponse.ok(reportService.getMonthlyComparisonData(year));
    }
}
