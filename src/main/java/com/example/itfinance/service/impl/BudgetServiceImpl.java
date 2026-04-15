package com.example.itfinance.service.impl;

import com.example.itfinance.entity.Budget;
import com.example.itfinance.service.BudgetService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BudgetServiceImpl implements BudgetService {
    private final Map<Long, Budget> budgetMap = new HashMap<>();
    private Long nextId = 1L;

    @Override
    public Budget createBudget(Budget budget) {
        budget.setId(nextId++);
        budgetMap.put(budget.getId(), budget);
        return budget;
    }

    @Override
    public Budget updateBudget(Budget budget) {
        if (budgetMap.containsKey(budget.getId())) {
            budgetMap.put(budget.getId(), budget);
            return budget;
        }
        return null;
    }

    @Override
    public void deleteBudget(Long id) {
        budgetMap.remove(id);
    }

    @Override
    public Budget getBudgetById(Long id) {
        return budgetMap.get(id);
    }

    @Override
    public List<Budget> getBudgetsByUserId(Long userId) {
        List<Budget> budgets = new ArrayList<>();
        for (Budget budget : budgetMap.values()) {
            if (budget.getUserId().equals(userId)) {
                budgets.add(budget);
            }
        }
        return budgets;
    }

    @Override
    public List<Budget> getBudgetsByPeriod(String period) {
        List<Budget> budgets = new ArrayList<>();
        for (Budget budget : budgetMap.values()) {
            if (budget.getPeriod().equals(period)) {
                budgets.add(budget);
            }
        }
        return budgets;
    }
}