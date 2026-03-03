package com.trimly.controller;

import com.trimly.dto.*;
import com.trimly.entity.User;
import com.trimly.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Step 1 — customer enters phone, OTP sent via WhatsApp */
    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @Valid @RequestBody SendOtpRequest req) {
        authService.sendCustomerOtp(req.getPhone());
        return ResponseEntity.ok(ApiResponse.ok("OTP sent to your WhatsApp", null));
    }

    /**
     * Step 2 — customer verifies OTP → receives access + refresh tokens.
     * Both stored client-side. Refresh token keeps session alive
     * until explicit logout — no re-OTP needed.
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest req,
            HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful",
            authService.verifyCustomerOtp(req, deviceInfo(http))));
    }

    /** Barber / Admin email + password login */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful",
            authService.login(req, deviceInfo(http))));
    }

    /** New barber registration — shop starts PENDING until admin approves */
    @PostMapping("/register/barber")
    public ResponseEntity<ApiResponse<AuthResponse>> registerBarber(
            @Valid @RequestBody BarberRegisterRequest req,
            HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Shop registered! Awaiting admin approval.",
            authService.registerBarber(req, deviceInfo(http))));
    }

    /**
     * Silent refresh — called by Angular interceptor when access token nears expiry.
     * Returns a new access token without requiring OTP or password.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshRequest req,
            HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed",
            authService.refresh(req.getRefreshToken(), deviceInfo(http))));
    }

    /**
     * Logout — deletes refresh token from DB.
     * Pass allDevices=true to invalidate all sessions for this user.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal User user,
            @RequestBody(required = false) LogoutRequest req) {
        authService.logout(user.getId(), req);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    /** Get current authenticated user info */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(UserInfo.builder()
            .id(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .role(user.getRole())
            .build()));
    }

    /** Barber forgot password — sends reset link via WhatsApp */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(ApiResponse.ok(
            "Password reset link sent to your WhatsApp.", null));
    }

    /** Reset password using token from WhatsApp link */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok(
            "Password updated. Please login again.", null));
    }

    private String deviceInfo(HttpServletRequest r) {
        String ua = r.getHeader("User-Agent");
        return ua != null ? ua.substring(0, Math.min(ua.length(), 200)) : "Unknown";
    }
}
