package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.entity.TaxRule;
import com.example.itfinance.service.TaxRuleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tax")
@CrossOrigin
public class TaxRuleController {
    private final TaxRuleService taxRuleService;

    public TaxRuleController(TaxRuleService taxRuleService) {
        this.taxRuleService = taxRuleService;
    }

    @GetMapping("/rule/list")
    public ApiResponse<List<TaxRule>> list() {
        return ApiResponse.ok(taxRuleService.list());
    }
}
