package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.entity.Invoice;
import com.example.itfinance.service.InvoiceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoice")
@CrossOrigin
public class InvoiceController {
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/list")
    public ApiResponse<List<Invoice>> list() {
        return ApiResponse.ok(invoiceService.list());
    }

    @GetMapping("/detail/{id}")
    public ApiResponse<Invoice> detail(@PathVariable Long id) {
        Invoice invoice = invoiceService.getById(id);
        if (invoice == null)
            return ApiResponse.fail("未找到发票");
        return ApiResponse.ok(invoice);
    }

    @PostMapping("/add")
    public ApiResponse<Invoice> add(@RequestBody Invoice invoice) {
        try {
            return ApiResponse.ok("新增成功", invoiceService.create(invoice));
        } catch (Exception e) {
            return ApiResponse.fail("发票保存失败：" + e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public ApiResponse<Invoice> update(@PathVariable Long id, @RequestBody Invoice invoice) {
        invoice.setId(id);
        try {
            return ApiResponse.ok("更新成功", invoiceService.update(invoice));
        } catch (Exception e) {
            return ApiResponse.fail("发票更新失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        invoiceService.deleteById(id);
        return ApiResponse.ok("删除成功", "ok");
    }
}
