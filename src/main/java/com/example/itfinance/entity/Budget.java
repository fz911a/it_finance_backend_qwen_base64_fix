package com.example.itfinance.entity;

public class Budget {
    private Long id;
    private String name;
    private String period;
    private Double total;
    private Double used;
    private String description;
    private Long userId;

    public Budget() {}
    public Budget(Long id, String name, String period, Double total, Double used, String description, Long userId) {
        this.id = id; this.name = name; this.period = period; this.total = total; this.used = used; this.description = description; this.userId = userId;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public Double getUsed() { return used; }
    public void setUsed(Double used) { this.used = used; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}