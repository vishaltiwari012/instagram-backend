package com.instagram.backend.service;

import com.instagram.backend.dtos.request.ForgotPasswordRequest;
import com.instagram.backend.dtos.request.LoginRequest;
import com.instagram.backend.dtos.request.RegisterRequest;
import com.instagram.backend.dtos.request.ResetPasswordRequest;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.LoginResponse;
import com.instagram.backend.dtos.response.RegistrationResponse;

public interface AuthService {
    ApiResponse<RegistrationResponse> registerUser(RegisterRequest request, String appUrl);
    ApiResponse<String> verifyUser(String token);
//    ApiResponse<LoginResponse> login(LoginRequest request);
    ApiResponse<String> login(LoginRequest request);
    ApiResponse<LoginResponse> verifyOtp(String tempToken, String enteredOtp);
    ApiResponse<String> resendOtp(String tempToken);
    ApiResponse<LoginResponse> refreshToken(String refreshToken);
    ApiResponse<String> resendTokenByEmail(String email, String appUrl);
    ApiResponse<String> forgotPassword(ForgotPasswordRequest request, String appUrl);
    ApiResponse<String> resetPassword(ResetPasswordRequest request, String token);
}
