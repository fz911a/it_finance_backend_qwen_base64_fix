package com.example.itfinance.service.impl;

import com.example.itfinance.service.ReportService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {
    @Override
    public Map<String, Object> profitReport(Long projectId, String startDate, String endDate) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("projectId", projectId);
        map.put("startDate", startDate);
        map.put("endDate", endDate);
        map.put("totalIncome", new BigDecimal("286000"));
        map.put("totalCost", new BigDecimal("158000"));
        map.put("totalTax", new BigDecimal("24150"));
        map.put("netProfit", new BigDecimal("103850"));
        map.put("cashFlow", new BigDecimal("86000"));
        map.put("receivable", new BigDecimal("42000"));
        return map;
    }
}
