package com.example.itfinance.service;

import java.util.Map;

public interface ReportService {
    Map<String, Object> profitReport(Long projectId, String startDate, String endDate);

    Map<String, Object> getTrendData(String startDate, String endDate);

    Map<String, Object> getCategoryData(String startDate, String endDate);

    Map<String, Object> getMonthlyComparisonData(String year);

    // 导出功能
    byte[] exportReport(String format, Long projectId, String startDate, String endDate);
}
