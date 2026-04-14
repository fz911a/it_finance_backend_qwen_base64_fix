package com.example.itfinance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String uploadPath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + uploadPath);
        registry.addResourceHandler("/files/**").addResourceLocations("file:" + uploadPath);
        registry.addResourceHandler("/face/**").addResourceLocations("file:" + uploadPath);
    }
}
