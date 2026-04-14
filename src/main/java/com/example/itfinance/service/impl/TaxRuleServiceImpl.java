package com.example.itfinance.service.impl;

import com.example.itfinance.entity.TaxRule;
import com.example.itfinance.service.TaxRuleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaxRuleServiceImpl implements TaxRuleService {
    @Override
    public List<TaxRule> list() {
        return List.of(
                new TaxRule(1L, "个人所得税", "人员", "阶梯", "按人员配置", "3%~45%"),
                new TaxRule(2L, "增值税", "项目", "比例", "按项目配置", "6%"),
                new TaxRule(3L, "企业所得税", "全局", "比例", "全局配置", "25%")
        );
    }
}
