package com.example.itfinance.service;

import com.example.itfinance.entity.Payment;

import java.util.List;

public interface PaymentService {
    List<Payment> list();
    Payment create(Payment payment);
}
