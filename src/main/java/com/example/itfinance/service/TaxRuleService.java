package com.example.itfinance.service;

import com.example.itfinance.entity.TaxRule;

import java.util.List;

public interface TaxRuleService {
    List<TaxRule> list();

    TaxRule create(TaxRule taxRule);

    TaxRule update(TaxRule taxRule);

    void deleteById(Long id);
}
