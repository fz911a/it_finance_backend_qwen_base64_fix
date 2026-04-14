package com.example.itfinance.service;

import com.example.itfinance.entity.Invoice;

import java.util.List;

public interface InvoiceService {
    List<Invoice> list();
    Invoice getById(Long id);
    Invoice create(Invoice invoice);
    void deleteById(Long id);
}
