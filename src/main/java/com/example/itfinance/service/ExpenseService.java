package com.example.itfinance.service;

import com.example.itfinance.entity.ExpenseRecord;

import java.util.List;

public interface ExpenseService {
    List<ExpenseRecord> list();
    ExpenseRecord getById(Long id);
    ExpenseRecord create(ExpenseRecord record);
    ExpenseRecord updateStatus(Long id, String status);
}
