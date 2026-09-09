package com.kisansarthi.auth;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Farmer OTP Authentication and Mandi Operator Login")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-otp")
    @Operation(summary = "Send Cryptographically Secure OTP to Farmer Mobile")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request.getPhone());
        return ResponseEntity.ok(ApiResponse.ok(null, "OTP sent successfully to registered mobile"));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP and Login Farmer")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyFarmerOtp(request.getPhone(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.ok(response, "Farmer authentication successful"));
    }

    @PostMapping("/admin-login")
    @Operation(summary = "Mandi Operator / Administrator Login")
    public ResponseEntity<ApiResponse<AuthResponse>> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        AuthResponse response = authService.adminLogin(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.ok(response, "Operator login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT Access Token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate session")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }
}
