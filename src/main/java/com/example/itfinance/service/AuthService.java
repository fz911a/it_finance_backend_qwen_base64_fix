package com.example.itfinance.service;

import com.example.itfinance.dto.LoginRequest;
import com.example.itfinance.dto.LoginResponse;
import com.example.itfinance.entity.User;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse faceLogin(String imageUrl);

    LoginResponse refreshToken(String refreshToken);

    User getUserByToken(String token);

    User updateUserProfile(String token, User user);

    void changePassword(String token, String oldPassword, String newPassword);

    void updateAvatar(String token, String avatar);

    void bindPhone(String token, String phone);
}
