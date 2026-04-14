package com.example.itfinance.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleException(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "服务器内部异常";
        }
        return ApiResponse.fail(message);
    }
}
