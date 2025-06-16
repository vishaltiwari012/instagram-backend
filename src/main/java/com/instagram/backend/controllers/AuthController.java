package com.instagram.backend.controllers;

import com.instagram.backend.dtos.request.*;
import com.instagram.backend.dtos.response.ApiResponse;
import com.instagram.backend.dtos.response.LoginResponse;
import com.instagram.backend.dtos.response.RegistrationResponse;
import com.instagram.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Auth APIs")
@Slf4j
public class AuthController {

    private final AuthService authService;


    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user and send verification email")
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {
        log.info("POST /register called for email: {}", request.getEmail());
        String appUrl = getApplicationUrl(servletRequest);
        return new ResponseEntity<>(authService.registerUser(request, appUrl), HttpStatus.CREATED);
    }


    @GetMapping("/verify-email")
    @Operation(summary = "Verify user email", description = "Verify the user's email using the token sent in the verification email")
    public ResponseEntity<ApiResponse<String>> verify(@RequestParam("token") String token) {
        log.info("GET /verify-email called with token");
        return ResponseEntity.ok(authService.verifyUser(token));
    }

//    @Operation(
//            summary = "User login",
//            description = "Authenticate user and return JWT token on success"
//    )
//    @PostMapping("/login")
//    public ResponseEntity<ApiResponse<LoginResponse>> loginUser(@Valid @RequestBody LoginRequest request,  HttpServletResponse response) {
//        log.info("POST /login called for username/email: {}", request.getUsername());
//
//        ApiResponse<LoginResponse> apiResponse  = authService.login(request);
//        setRefreshTokenCookie(response, apiResponse.getData().getRefreshToken());
//
//        // Mask refresh token in response body
//        apiResponse.getData().setRefreshToken("Securely saved");
//
//        log.info("User {} logged in successfully and refresh token cookie set", request.getUsername());
//        return ResponseEntity.ok(apiResponse);
//    }



    @PostMapping("/login")
    @Operation(
            summary = "Login with email/username and password",
            description = "Step 1 of 2FA. Verifies credentials and sends OTP to registered email."
    )
    public ResponseEntity<ApiResponse<String>> loginUser(@Valid @RequestBody LoginRequest request) {
        log.info("POST /login called for username/email: {}", request.getUsernameOrEmail());
        return ResponseEntity.ok(authService.login(request));
    }


    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Step 2 of 2FA. Verifies the OTP sent to the user's email and completes login.")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyOtp(
            @RequestParam("token") String tempToken,
            @RequestParam("otp") String otp,
            HttpServletResponse response
    ) {
        ApiResponse<LoginResponse> apiResponse = authService.verifyOtp(tempToken, otp);
        setRefreshTokenCookie(response, apiResponse.getData().getRefreshToken());
        apiResponse.getData().setRefreshToken("Securely saved");
        log.info("User {} logged in successfully", apiResponse.getData().getUsername());
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP", description = "Resends OTP to the user's registered email using the session token from login.")
    public ResponseEntity<ApiResponse<String>> resendOtp(@RequestParam("token") String tempToken) {
        return ResponseEntity.ok(authService.resendOtp(tempToken));
    }


    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh Token", description = "Refresh JWT token using refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        log.info("POST /refresh-token called");
        ApiResponse<LoginResponse> apiResponse = authService.refreshToken(refreshToken);
        setRefreshTokenCookie(response, apiResponse.getData().getRefreshToken());
        apiResponse.getData().setRefreshToken("Securely stored");
        return ResponseEntity.ok(apiResponse);
    }


    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout user", description = "Clears refresh token cookie")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/v1/auth/refresh-token");
        cookie.setMaxAge(0); // Deletes the cookie
        response.addCookie(cookie);
        log.info("Refresh token cookie cleared on logout");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Send password reset link to user's registered email")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest,
            HttpServletRequest request) {

        log.info("POST /forgot-password called for email: {}", forgotPasswordRequest.getEmail());
        return ResponseEntity.ok(authService.forgotPassword(forgotPasswordRequest, getApplicationUrl(request)));
    }



    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset user password using the token from password reset email")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            @RequestParam("token") String token) {

        log.info("POST /reset-password called");
        return ResponseEntity.ok(authService.resetPassword(request, token));
    }


    @PostMapping("/resend-token")
    @Operation(summary = "Resend verification token", description = "Resend email verification token to user's email")
    public ResponseEntity<ApiResponse<String>> resendVerificationEmail(
            @Valid @RequestBody ResendTokenRequest request,
            HttpServletRequest servletRequest) {

        log.info("POST /resend-token called for email: {}", request.getEmail());
        String appUrl = getApplicationUrl(servletRequest);
        return ResponseEntity.ok(authService.resendTokenByEmail(request.getEmail(), appUrl));
    }

    // ================================
    // Utility methods
    // ================================

    private String getApplicationUrl(HttpServletRequest request) {
        return request.getRequestURL().toString().replace(request.getServletPath(), "");
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        log.debug("Setting refresh token cookie");
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Set to true in production
        cookie.setPath("/api/v1/auth/refresh-token");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(cookie);
        log.debug("Refresh token cookie set");
    }
}
