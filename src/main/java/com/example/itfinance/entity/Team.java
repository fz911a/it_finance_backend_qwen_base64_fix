package com.example.itfinance.entity;

public class Team {
    private Long id;
    private String name;
    private String description;
    private Long creatorId;

    public Team() {}
    public Team(Long id, String name, String description, Long creatorId) {
        this.id = id; this.name = name; this.description = description; this.creatorId = creatorId;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
}