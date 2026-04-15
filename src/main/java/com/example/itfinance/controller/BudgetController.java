package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.entity.Budget;
import com.example.itfinance.service.AuthService;
import com.example.itfinance.service.BudgetService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budget")
@CrossOrigin
public class BudgetController {
    private final BudgetService budgetService;
    private final AuthService authService;

    public BudgetController(BudgetService budgetService, AuthService authService) {
        this.budgetService = budgetService;
        this.authService = authService;
    }

    @PostMapping("/create")
    public ApiResponse<Budget> createBudget(@RequestHeader("Authorization") String token, @RequestBody Budget budget) {
        try {
            Long userId = authService.getUserByToken(token).getId();
            budget.setUserId(userId);
            Budget createdBudget = budgetService.createBudget(budget);
            return ApiResponse.ok(createdBudget);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PutMapping("/update")
    public ApiResponse<Budget> updateBudget(@RequestHeader("Authorization") String token, @RequestBody Budget budget) {
        try {
            Long userId = authService.getUserByToken(token).getId();
            Budget existingBudget = budgetService.getBudgetById(budget.getId());
            if (existingBudget == null || !existingBudget.getUserId().equals(userId)) {
                throw new IllegalArgumentException("预算不存在或无权限");
            }
            Budget updatedBudget = budgetService.updateBudget(budget);
            return ApiResponse.ok(updatedBudget);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> deleteBudget(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        try {
            Long userId = authService.getUserByToken(token).getId();
            Budget budget = budgetService.getBudgetById(id);
            if (budget == null || !budget.getUserId().equals(userId)) {
                throw new IllegalArgumentException("预算不存在或无权限");
            }
            budgetService.deleteBudget(id);
            return ApiResponse.ok("预算删除成功");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/get/{id}")
    public ApiResponse<Budget> getBudgetById(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        try {
            Long userId = authService.getUserByToken(token).getId();
            Budget budget = budgetService.getBudgetById(id);
            if (budget == null || !budget.getUserId().equals(userId)) {
                throw new IllegalArgumentException("预算不存在或无权限");
            }
            return ApiResponse.ok(budget);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/list")
    public ApiResponse<List<Budget>> getBudgetsByUserId(@RequestHeader("Authorization") String token) {
        try {
            Long userId = authService.getUserByToken(token).getId();
            List<Budget> budgets = budgetService.getBudgetsByUserId(userId);
            return ApiResponse.ok(budgets);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/period/{period}")
    public ApiResponse<List<Budget>> getBudgetsByPeriod(@RequestHeader("Authorization") String token, @PathVariable String period) {
        try {
            Long userId = authService.getUserByToken(token).getId();
            List<Budget> budgets = budgetService.getBudgetsByPeriod(period);
            // 过滤出当前用户的预算
            budgets.removeIf(budget -> !budget.getUserId().equals(userId));
            return ApiResponse.ok(budgets);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
