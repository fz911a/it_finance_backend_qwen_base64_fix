package com.example.itfinance.service.impl;

import com.example.itfinance.service.ReportService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ReportServiceImpl implements ReportService {
    private final JdbcTemplate jdbcTemplate;

    public ReportServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private BigDecimal q(String sql, Object... args) {
        BigDecimal v = args.length == 0 ? jdbcTemplate.queryForObject(sql, BigDecimal.class)
                : jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return v == null ? BigDecimal.ZERO : v;
    }

    @Override
    public Map<String, Object> profitReport(Long projectId, String startDate, String endDate) {
        String projectFilter = projectId != null ? " AND project_id = " + projectId : "";
        BigDecimal totalIncome = q("SELECT COALESCE(SUM(amount),0) FROM payment WHERE 1=1" + projectFilter);
        BigDecimal totalCost = q("SELECT COALESCE(SUM(amount),0) FROM expense_record WHERE 1=1" + projectFilter)
                .add(q("SELECT COALESCE(SUM(spa.amount),0) FROM salary_project_allocation spa WHERE 1=1"
                        + (projectId != null ? " AND spa.project_id=" + projectId : "")));
        BigDecimal totalTax = q("SELECT COALESCE(SUM(tax_amount),0) FROM invoice WHERE 1=1" + projectFilter)
                .add(q("SELECT COALESCE(SUM(personal_tax),0) FROM salary_record"));
        BigDecimal receivable = q("SELECT COALESCE(SUM(unpaid_amount),0) FROM invoice WHERE 1=1" + projectFilter);
        BigDecimal cashFlow = totalIncome.subtract(totalCost);
        BigDecimal netProfit = cashFlow.subtract(totalTax);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalIncome", totalIncome);
        map.put("totalCost", totalCost);
        map.put("totalTax", totalTax);
        map.put("netProfit", netProfit);
        map.put("cashFlow", cashFlow);
        map.put("receivable", receivable);
        return map;
    }

    @Override
    public Map<String, Object> getTrendData(String startDate, String endDate) {
        // 按月汇总近6个月收入与支出
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "SELECT DATE_FORMAT(payment_date,'%Y-%m') mon, SUM(amount) income FROM payment GROUP BY mon ORDER BY mon DESC LIMIT 6",
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("mon", rs.getString("mon"));
                    m.put("income", rs.getBigDecimal("income"));
                    return m;
                });
        Collections.reverse(rows);
        List<String> categories = new ArrayList<>();
        List<Object> incomeData = new ArrayList<>();
        List<Object> expenseData = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String mon = (String) r.get("mon");
            categories.add(mon);
            incomeData.add(r.get("income"));
            BigDecimal exp = q(
                    "SELECT COALESCE(SUM(amount),0) FROM expense_record WHERE DATE_FORMAT(expense_date,'%Y-%m')=?",
                    mon);
            expenseData.add(exp);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("categories", categories);
        List<Map<String, Object>> series = new ArrayList<>();
        Map<String, Object> s1 = new LinkedHashMap<>();
        s1.put("name", "收入");
        s1.put("data", incomeData);
        series.add(s1);
        Map<String, Object> s2 = new LinkedHashMap<>();
        s2.put("name", "支出");
        s2.put("data", expenseData);
        series.add(s2);
        map.put("series", series);
        return map;
    }

    @Override
    public Map<String, Object> getCategoryData(String startDate, String endDate) {
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "SELECT expense_type, SUM(amount) total FROM expense_record GROUP BY expense_type ORDER BY total DESC",
                (rs, i) -> {
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
    public Map<String, Object> getMonthlyComparisonData(String year) {
        String y = (year == null || year.isBlank()) ? String.valueOf(java.time.Year.now().getValue()) : year;
        String lastY = String.valueOf(Integer.parseInt(y) - 1);
        List<String> months = Arrays.asList("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12");
        List<Object> thisYearData = new ArrayList<>();
        List<Object> lastYearData = new ArrayList<>();
        for (String m : months) {
            thisYearData.add(q("SELECT COALESCE(SUM(amount),0) FROM payment WHERE DATE_FORMAT(payment_date,'%Y-%m')=?",
                    y + "-" + m));
            lastYearData.add(q("SELECT COALESCE(SUM(amount),0) FROM payment WHERE DATE_FORMAT(payment_date,'%Y-%m')=?",
                    lastY + "-" + m));
        }
        List<String> categories = Arrays.asList("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月",
                "12月");
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("categories", categories);
        List<Map<String, Object>> series = new ArrayList<>();
        Map<String, Object> s1 = new LinkedHashMap<>();
        s1.put("name", "本年(" + y + ")");
        s1.put("data", thisYearData);
        series.add(s1);
        Map<String, Object> s2 = new LinkedHashMap<>();
        s2.put("name", "去年(" + lastY + ")");
        s2.put("data", lastYearData);
        series.add(s2);
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
        sb.append("项目,金额\n");
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
                row.createCell(1).setCellValue(entry.getValue().toString());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            return "Excel Error".getBytes();
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
                table.addCell(v.toString());
            });
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            return "PDF Error".getBytes();
        }
    }
}
