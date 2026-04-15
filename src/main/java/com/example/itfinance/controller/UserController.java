package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import com.example.itfinance.entity.User;
import com.example.itfinance.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {
    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/profile")
    public ApiResponse<User> getProfile(@RequestHeader("Authorization") String token) {
        try {
            User user = authService.getUserByToken(token);
            return ApiResponse.ok(user);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PutMapping("/profile")
    public ApiResponse<User> updateProfile(@RequestHeader("Authorization") String token, @RequestBody User user) {
        try {
            User updatedUser = authService.updateUserProfile(token, user);
            return ApiResponse.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ApiResponse<String> changePassword(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> body) {
        try {
            String oldPassword = body.get("oldPassword");
            String newPassword = body.get("newPassword");
            authService.changePassword(token, oldPassword, newPassword);
            return ApiResponse.ok("Password changed successfully");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/upload-avatar")
    public ApiResponse<String> uploadAvatar(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> body) {
        try {
            String avatar = body.get("avatar");
            authService.updateAvatar(token, avatar);
            return ApiResponse.ok("Avatar uploaded successfully");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/bind-phone")
    public ApiResponse<String> bindPhone(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> body) {
        try {
            String phone = body.get("phone");
            authService.bindPhone(token, phone);
            return ApiResponse.ok("Phone bound successfully");
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
