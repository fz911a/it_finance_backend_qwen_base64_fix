package com.example.itfinance.service;

import java.util.Map;

public interface ReportService {
    Map<String, Object> profitReport(Long projectId, String startDate, String endDate);

    Map<String, Object> getTrendData(Long projectId, String startDate, String endDate);

    Map<String, Object> getCategoryData(Long projectId, String startDate, String endDate);

    Map<String, Object> getMonthlyComparisonData(Long projectId, String year);

    // 瀵煎嚭鍔熻兘
    byte[] exportReport(String format, Long projectId, String startDate, String endDate);
}
