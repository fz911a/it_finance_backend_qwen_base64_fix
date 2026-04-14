package com.example.itfinance.entity;

public class TaxRule {
    private Long id;
    private String ruleName;
    private String taxType;
    private String calcType;
    private String scopeType;
    private String ruleValue;

    public TaxRule() {}
    public TaxRule(Long id, String ruleName, String taxType, String calcType, String scopeType, String ruleValue) {
        this.id = id; this.ruleName = ruleName; this.taxType = taxType; this.calcType = calcType; this.scopeType = scopeType; this.ruleValue = ruleValue;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getTaxType() { return taxType; }
    public void setTaxType(String taxType) { this.taxType = taxType; }
    public String getCalcType() { return calcType; }
    public void setCalcType(String calcType) { this.calcType = calcType; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getRuleValue() { return ruleValue; }
    public void setRuleValue(String ruleValue) { this.ruleValue = ruleValue; }
}
