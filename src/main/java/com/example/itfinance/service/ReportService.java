package com.example.itfinance.service;

import java.util.Map;

public interface ReportService {
    Map<String, Object> profitReport(Long projectId, String startDate, String endDate);
}
