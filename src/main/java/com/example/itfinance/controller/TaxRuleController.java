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

    @PostMapping("/rule/add")
    public ApiResponse<TaxRule> add(@RequestBody TaxRule taxRule) {
        try {
            return ApiResponse.ok("新增成功", taxRuleService.create(taxRule));
        } catch (Exception e) {
            return ApiResponse.fail("税务规则新增失败：" + e.getMessage());
        }
    }

    @PutMapping("/rule/update/{id}")
    public ApiResponse<TaxRule> update(@PathVariable Long id, @RequestBody TaxRule taxRule) {
        taxRule.setId(id);
        try {
            return ApiResponse.ok("更新成功", taxRuleService.update(taxRule));
        } catch (Exception e) {
            return ApiResponse.fail("税务规则更新失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/rule/delete/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        taxRuleService.deleteById(id);
        return ApiResponse.ok("删除成功", "ok");
    }
}
