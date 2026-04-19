package com.example.itfinance.service.impl;

import com.example.itfinance.entity.TaxRule;
import com.example.itfinance.service.TaxRuleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaxRuleServiceImpl implements TaxRuleService {
    private final JdbcTemplate jdbcTemplate;

    public TaxRuleServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private TaxRule mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        TaxRule t = new TaxRule();
        t.setId(rs.getLong("id"));
        t.setRuleName(rs.getString("rule_name"));
        t.setTaxType(rs.getString("tax_type"));
        t.setCalcType(rs.getString("calc_type"));
        t.setScopeType(rs.getString("scope_type"));
        // 展示用：比例类型显示 rate，阶梯类型显示 ladder_json
        String calcType = rs.getString("calc_type");
        if ("比例".equals(calcType)) {
            java.math.BigDecimal rate = rs.getBigDecimal("rate");
            t.setRuleValue(rate != null ? rate.stripTrailingZeros().toPlainString() + "%" : "-");
        } else {
            t.setRuleValue(rs.getString("ladder_json") != null ? rs.getString("ladder_json") : "-");
        }
        return t;
    }

    @Override
    public List<TaxRule> list() {
        return jdbcTemplate.query(
                "SELECT id, rule_name, tax_type, calc_type, rate, ladder_json, scope_type FROM tax_rule WHERE status=1 ORDER BY id",
                this::mapRow);
    }

    @Override
    public TaxRule create(TaxRule taxRule) {
        jdbcTemplate.update(
                "INSERT INTO tax_rule(rule_name, tax_type, calc_type, scope_type, status) VALUES (?,?,?,?,1)",
                taxRule.getRuleName(), taxRule.getTaxType(), taxRule.getCalcType(), taxRule.getScopeType());
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        List<TaxRule> list = jdbcTemplate.query(
                "SELECT id, rule_name, tax_type, calc_type, rate, ladder_json, scope_type FROM tax_rule WHERE id=?",
                this::mapRow, id);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public TaxRule update(TaxRule taxRule) {
        jdbcTemplate.update(
                "UPDATE tax_rule SET rule_name=?, tax_type=?, calc_type=?, scope_type=? WHERE id=?",
                taxRule.getRuleName(), taxRule.getTaxType(), taxRule.getCalcType(),
                taxRule.getScopeType(), taxRule.getId());
        List<TaxRule> list = jdbcTemplate.query(
                "SELECT id, rule_name, tax_type, calc_type, rate, ladder_json, scope_type FROM tax_rule WHERE id=?",
                this::mapRow, taxRule.getId());
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("UPDATE tax_rule SET status=0 WHERE id=?", id);
    }
}
