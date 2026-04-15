package com.example.itfinance.service.impl;

import com.example.itfinance.service.ReportService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

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

    @Override
    public Map<String, Object> getTrendData(String startDate, String endDate) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("categories", Arrays.asList("1月", "2月", "3月", "4月", "5月", "6月"));
        
        List<Map<String, Object>> series = new ArrayList<>();
        Map<String, Object> incomeSeries = new LinkedHashMap<>();
        incomeSeries.put("name", "收入");
        incomeSeries.put("data", Arrays.asList(12000, 15000, 18000, 16000, 20000, 22000));
        series.add(incomeSeries);
        
        Map<String, Object> expenseSeries = new LinkedHashMap<>();
        expenseSeries.put("name", "支出");
        expenseSeries.put("data", Arrays.asList(8000, 9000, 10000, 11000, 12000, 13000));
        series.add(expenseSeries);
        
        map.put("series", series);
        return map;
    }

    @Override
    public Map<String, Object> getCategoryData(String startDate, String endDate) {
        Map<String, Object> map = new LinkedHashMap<>();
        
        List<Map<String, Object>> series = new ArrayList<>();
        Map<String, Object> category1 = new LinkedHashMap<>();
        category1.put("name", "差旅费");
        category1.put("data", 3000);
        series.add(category1);
        
        Map<String, Object> category2 = new LinkedHashMap<>();
        category2.put("name", "餐饮费");
        category2.put("data", 2500);
        series.add(category2);
        
        Map<String, Object> category3 = new LinkedHashMap<>();
        category3.put("name", "办公用品");
        category3.put("data", 1500);
        series.add(category3);
        
        Map<String, Object> category4 = new LinkedHashMap<>();
        category4.put("name", "其他");
        category4.put("data", 1000);
        series.add(category4);
        
        map.put("series", series);
        return map;
    }

    @Override
    public Map<String, Object> getMonthlyComparisonData(String year) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("categories", Arrays.asList("1月", "2月", "3月", "4月", "5月", "6月"));
        
        List<Map<String, Object>> series = new ArrayList<>();
        Map<String, Object> currentYearSeries = new LinkedHashMap<>();
        currentYearSeries.put("name", "本年");
        currentYearSeries.put("data", Arrays.asList(12000, 15000, 18000, 16000, 20000, 22000));
        series.add(currentYearSeries);
        
        Map<String, Object> lastYearSeries = new LinkedHashMap<>();
        lastYearSeries.put("name", "去年");
        lastYearSeries.put("data", Arrays.asList(10000, 12000, 14000, 13000, 16000, 18000));
        series.add(lastYearSeries);
        
        map.put("series", series);
        return map;
    }
}
