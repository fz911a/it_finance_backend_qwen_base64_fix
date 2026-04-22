package com.example.itfinance.dto;

import jakarta.validation.constraints.NotBlank;

public class FaceRecognizeRequest {
    @NotBlank(message = "图片地址不能为空")
    private String imageUrl;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
