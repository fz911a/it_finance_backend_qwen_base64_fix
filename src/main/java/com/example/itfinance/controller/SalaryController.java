package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.entity.SalaryRecord;
import com.example.itfinance.service.SalaryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salary")
@CrossOrigin
public class SalaryController {
    private final SalaryService salaryService;

    public SalaryController(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    @GetMapping("/list")
    public ApiResponse<List<SalaryRecord>> list() {
        return ApiResponse.ok(salaryService.list());
    }

    @PostMapping("/add")
    public ApiResponse<SalaryRecord> add(@RequestBody SalaryRecord record) {
        try {
            return ApiResponse.ok("新增成功", salaryService.create(record));
        } catch (Exception e) {
            return ApiResponse.fail("工资记录新增失败：" + e.getMessage());
        }
    }
}
