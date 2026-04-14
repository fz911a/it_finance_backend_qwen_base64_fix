package com.example.itfinance.service.impl;

import com.example.itfinance.entity.SalaryRecord;
import com.example.itfinance.service.SalaryService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SalaryServiceImpl implements SalaryService {
    @Override
    public List<SalaryRecord> list() {
        return List.of(
                new SalaryRecord(1L, "张三", "前端开发", "2026-03", new BigDecimal("12000"), "智慧运维平台", "70%"),
                new SalaryRecord(2L, "李四", "后端开发", "2026-03", new BigDecimal("15000"), "企业数据中台", "100%")
        );
    }
}
