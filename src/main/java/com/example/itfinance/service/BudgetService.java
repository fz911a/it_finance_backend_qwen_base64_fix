package com.example.itfinance.service;

import com.example.itfinance.entity.Budget;
import java.util.List;

public interface BudgetService {
    Budget createBudget(Budget budget);
    Budget updateBudget(Budget budget);
    void deleteBudget(Long id);
    Budget getBudgetById(Long id);
    List<Budget> getBudgetsByUserId(Long userId);
    List<Budget> getBudgetsByPeriod(String period);
}