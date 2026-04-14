package com.example.itfinance.service;

import com.example.itfinance.dto.LoginRequest;
import com.example.itfinance.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(String refreshToken);
}
