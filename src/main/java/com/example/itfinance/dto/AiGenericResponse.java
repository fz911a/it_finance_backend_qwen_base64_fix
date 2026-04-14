package com.example.itfinance.dto;

import java.util.Map;

public class AiGenericResponse {
    private String content;
    private Map<String, Object> parsed;

    public AiGenericResponse() {}
    public AiGenericResponse(String content, Map<String, Object> parsed) {
        this.content = content; this.parsed = parsed;
    }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Map<String, Object> getParsed() { return parsed; }
    public void setParsed(Map<String, Object> parsed) { this.parsed = parsed; }
}
