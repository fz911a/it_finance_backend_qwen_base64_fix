package com.example.itfinance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String chatModel;
    private String visionModel;
    private boolean enabled;
    private String faceApiUrl;
    private String faceApiKey;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public String getVisionModel() {
        return visionModel;
    }

    public void setVisionModel(String visionModel) {
        this.visionModel = visionModel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFaceApiUrl() {
        return faceApiUrl;
    }

    public void setFaceApiUrl(String faceApiUrl) {
        this.faceApiUrl = faceApiUrl;
    }

    public String getFaceApiKey() {
        return faceApiKey;
    }

    public void setFaceApiKey(String faceApiKey) {
        this.faceApiKey = faceApiKey;
    }
}
