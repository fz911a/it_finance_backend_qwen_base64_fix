package com.example.itfinance.service.impl;

import com.example.itfinance.service.ReportService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {
    private final JdbcTemplate jdbcTemplate;

    public ReportServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private BigDecimal q(String sql, Object... args) {
        BigDecimal value = args.length == 0 ? jdbcTemplate.queryForObject(sql, BigDecimal.class)
                : jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long count(String sql, Object... args) {
        Long value = args.length == 0 ? jdbcTemplate.queryForObject(sql, Long.class)
                : jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private String normalizeDate(String date) {
        return date == null ? null : date.trim();
    }

    private void appendProjectFilter(StringBuilder sql, List<Object> params, Long projectId, String column) {
        if (projectId != null) {
            sql.append(" AND ").append(column).append(" = ?");
            params.add(projectId);
        }
    }

    private void appendDateFilter(StringBuilder sql, List<Object> params, String column, String startDate,
            String endDate) {
        String start = normalizeDate(startDate);
        String end = normalizeDate(endDate);
        if (start != null && !start.isBlank()) {
            sql.append(" AND ").append(column).append(" >= ?");
            params.add(start);
        }
        if (end != null && !end.isBlank()) {
            sql.append(" AND ").append(column).append(" <= ?");
            params.add(end);
        }
    }

    private BigDecimal queryAmount(StringBuilder sql, List<Object> params) {
        return q(sql.toString(), params.toArray());
    }

    private BigDecimal sumPayments(Long projectId, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(amount),0) FROM payment WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendProjectFilter(sql, params, projectId, "project_id");
        appendDateFilter(sql, params, "payment_date", startDate, endDate);
        return queryAmount(sql, params);
    }

    private BigDecimal sumExpenses(Long projectId, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(amount),0) FROM expense_record WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendProjectFilter(sql, params, projectId, "project_id");
        appendDateFilter(sql, params, "expense_date", startDate, endDate);
        return queryAmount(sql, params);
    }

    private BigDecimal sumSalaryAllocationCost(Long projectId, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(SUM(spa.amount),0) FROM salary_project_allocation spa LEFT JOIN salary_record sr ON sr.id = spa.salary_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendProjectFilter(sql, params, projectId, "spa.project_id");
        appendDateFilter(sql, params, "sr.pay_date", startDate, endDate);
        return queryAmount(sql, params);
    }

    private BigDecimal sumInvoiceTax(Long projectId, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(tax_amount),0) FROM invoice WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendProjectFilter(sql, params, projectId, "project_id");
        appendDateFilter(sql, params, "invoice_date", startDate, endDate);
        return queryAmount(sql, params);
    }

    private BigDecimal sumReceivable(Long projectId, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(unpaid_amount),0) FROM invoice WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendProjectFilter(sql, params, projectId, "project_id");
        appendDateFilter(sql, params, "invoice_date", startDate, endDate);
        return queryAmount(sql, params);
    }

    private BigDecimal sumPersonalTax(Long projectId, String startDate, String endDate) {
        String start = normalizeDate(startDate);
        String end = normalizeDate(endDate);

        if (projectId == null) {
            StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(personal_tax),0) FROM salary_record WHERE 1=1");
            List<Object> params = new ArrayList<>();
            appendDateFilter(sql, params, "pay_date", start, end);
            return queryAmount(sql, params);
        }

        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(SUM(COALESCE(sr.personal_tax,0) * COALESCE(spa.ratio,0) / 100),0) FROM salary_project_allocation spa JOIN salary_record sr ON sr.id = spa.salary_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendProjectFilter(sql, params, projectId, "spa.project_id");
        appendDateFilter(sql, params, "sr.pay_date", start, end);
        return queryAmount(sql, params);
    }

    @Override
    public Map<String, Object> profitReport(Long projectId, String startDate, String endDate) {
        BigDecimal totalIncome = sumPayments(projectId, startDate, endDate);
        BigDecimal totalCost = sumExpenses(projectId, startDate, endDate).add(sumSalaryAllocationCost(projectId, startDate, endDate));
        BigDecimal invoiceTax = sumInvoiceTax(projectId, startDate, endDate);
        BigDecimal salaryTax = sumPersonalTax(projectId, startDate, endDate);
        BigDecimal totalTax = invoiceTax.add(salaryTax);
        BigDecimal receivable = sumReceivable(projectId, startDate, endDate);
        BigDecimal cashFlow = totalIncome.subtract(totalCost);
        BigDecimal netProfit = cashFlow.subtract(totalTax);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalIncome", totalIncome);
        map.put("totalCost", totalCost);
        map.put("totalTax", totalTax);
        map.put("invoiceTax", invoiceTax);
        map.put("salaryTax", salaryTax);
        map.put("netProfit", netProfit);
        map.put("cashFlow", cashFlow);
        map.put("receivable", receivable);
        return map;
    }

    @Override
    public Map<String, Object> getTrendData(Long projectId, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT mon, SUM(income) AS income, SUM(expense) AS expense FROM (");
        sql.append(" SELECT DATE_FORMAT(payment_date,'%Y-%m') mon, SUM(amount) income, 0 expense FROM payment WHERE 1=1");
        List<Object> paymentParams = new ArrayList<>();
        appendProjectFilter(sql, paymentParams, projectId, "project_id");
        appendDateFilter(sql, paymentParams, "payment_date", startDate, endDate);
        sql.append(" GROUP BY DATE_FORMAT(payment_date,'%Y-%m')");
        sql.append(" UNION ALL");
        sql.append(" SELECT DATE_FORMAT(expense_date,'%Y-%m') mon, 0 income, SUM(amount) expense FROM expense_record WHERE 1=1");
        List<Object> expenseParams = new ArrayList<>();
        appendProjectFilter(sql, expenseParams, projectId, "project_id");
        appendDateFilter(sql, expenseParams, "expense_date", startDate, endDate);
        sql.append(" GROUP BY DATE_FORMAT(expense_date,'%Y-%m')");
        sql.append(" ) t GROUP BY mon ORDER BY mon");

        List<Object> params = new ArrayList<>();
        params.addAll(paymentParams);
        params.addAll(expenseParams);

        List<Map<String, Object>> rows = jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("mon", rs.getString("mon"));
            m.put("income", rs.getBigDecimal("income"));
            m.put("expense", rs.getBigDecimal("expense"));
            return m;
        });

        List<String> categories = new ArrayList<>();
        List<Object> incomeData = new ArrayList<>();
        List<Object> expenseData = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            categories.add(String.valueOf(row.get("mon")));
            incomeData.add(row.get("income"));
            expenseData.add(row.get("expense"));
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("categories", categories);
        List<Map<String, Object>> series = new ArrayList<>();
        Map<String, Object> incomeSeries = new LinkedHashMap<>();
        incomeSeries.put("name", "收入");
        incomeSeries.put("data", incomeData);
        series.add(incomeSeries);
        Map<String, Object> expenseSeries = new LinkedHashMap<>();
        expenseSeries.put("name", "支出");
        expenseSeries.put("data", expenseData);
        series.add(expenseSeries);
        map.put("series", series);
        return map;
    }

    @Override
    public Map<String, Object> getCategoryData(Long projectId, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder(
                "SELECT expense_type, SUM(amount) total FROM expense_record WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendProjectFilter(sql, params, projectId, "project_id");
        appendDateFilter(sql, params, "expense_date", startDate, endDate);
        sql.append(" GROUP BY expense_type ORDER BY total DESC");

        List<Map<String, Object>> rows = jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", rs.getString("expense_type"));
            m.put("data", rs.getBigDecimal("total"));
            return m;
        });

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("series", rows);
        return map;
    }

    @Override
    public Map<String, Object> getMonthlyComparisonData(Long projectId, String year) {
        String y = (year == null || year.isBlank()) ? String.valueOf(java.time.Year.now().getValue()) : year.trim();
        String lastY = String.valueOf(Integer.parseInt(y) - 1);
        List<String> months = Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12");
        List<Object> thisYearData = new ArrayList<>();
        List<Object> lastYearData = new ArrayList<>();

        for (String month : months) {
            String currentMonth = y + "-" + month;
            String prevMonth = lastY + "-" + month;

            StringBuilder currentSql = new StringBuilder("SELECT COALESCE(SUM(amount),0) FROM payment WHERE DATE_FORMAT(payment_date,'%Y-%m') = ?");
            List<Object> currentParams = new ArrayList<>();
            currentParams.add(currentMonth);
            appendProjectFilter(currentSql, currentParams, projectId, "project_id");

            StringBuilder prevSql = new StringBuilder("SELECT COALESCE(SUM(amount),0) FROM payment WHERE DATE_FORMAT(payment_date,'%Y-%m') = ?");
            List<Object> prevParams = new ArrayList<>();
            prevParams.add(prevMonth);
            appendProjectFilter(prevSql, prevParams, projectId, "project_id");

            thisYearData.add(q(currentSql.toString(), currentParams.toArray()));
            lastYearData.add(q(prevSql.toString(), prevParams.toArray()));
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("categories", Arrays.asList("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"));
        List<Map<String, Object>> series = new ArrayList<>();
        Map<String, Object> currentSeries = new LinkedHashMap<>();
        currentSeries.put("name", "本年(" + y + ")");
        currentSeries.put("data", thisYearData);
        series.add(currentSeries);
        Map<String, Object> prevSeries = new LinkedHashMap<>();
        prevSeries.put("name", "去年(" + lastY + ")");
        prevSeries.put("data", lastYearData);
        series.add(prevSeries);
        map.put("series", series);
        return map;
    }

    @Override
    public byte[] exportReport(String format, Long projectId, String startDate, String endDate) {
        Map<String, Object> data = profitReport(projectId, startDate, endDate);
        return switch (format.toLowerCase()) {
            case "csv" -> generateCsv(data);
            case "excel", "xlsx" -> generateExcel(data);
            case "pdf" -> generatePdf(data);
            default -> "Unsupported format".getBytes(StandardCharsets.UTF_8);
        };
    }

    private byte[] generateCsv(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("指标,金额\n");
        data.forEach((k, v) -> sb.append(k).append(",").append(v).append("\n"));
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateExcel(Map<String, Object> data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("财务报表");
            int rowNum = 0;
            Row header = sheet.createRow(rowNum++);
            header.createCell(0).setCellValue("指标名称");
            header.createCell(1).setCellValue("数值");

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(String.valueOf(entry.getValue()));
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            return "Excel Error".getBytes(StandardCharsets.UTF_8);
        }
    }

    private byte[] generatePdf(Map<String, Object> data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.add(new Paragraph("IT Finance Report").setBold().setFontSize(18));

            float[] columnWidths = { 150f, 150f };
            Table table = new Table(columnWidths);
            table.addCell("Metric");
            table.addCell("Value");

            data.forEach((k, v) -> {
                table.addCell(k);
                table.addCell(String.valueOf(v));
            });
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            return "PDF Error".getBytes(StandardCharsets.UTF_8);
        }
    }
}
