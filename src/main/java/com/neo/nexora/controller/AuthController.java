package com.neo.nexora.controller;

import com.neo.nexora.dto.*;
import com.neo.nexora.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, logout, and password management")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and returns a JWT token")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse authResponse = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Registration successful", authResponse));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Login", description = "Authenticates a user and returns a JWT token")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse authResponse = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid credentials"));
        }
    }

    @Operation(
            summary = "Logout",
            description = "Blacklists the current JWT token",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authorization header"));
        }

        String token = authHeader.substring(7);
        authService.logout(token);

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @Operation(
            summary = "Request password reset",
            description = "Generates a password reset token for the given email")
    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<String>> requestPasswordReset(
            @RequestBody PasswordResetRequest request) {
        try {
            String resetToken = authService.requestPasswordReset(request);
            return ResponseEntity.ok(
                    ApiResponse.success("Password reset token generated. Check your email.", resetToken));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
            summary = "Confirm password reset",
            description = "Resets the password using the provided reset token")
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @RequestBody PasswordResetConfirm request) {
        try {
            authService.confirmPasswordReset(request);
            return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
            summary = "Change password",
            description = "Changes the password of the authenticated user",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequest request) {
        try {
            authService.changePassword(userDetails.getUsername(), request);
            return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
            summary = "Create admin user",
            description = "Creates a new user with ADMIN role. Only accessible by existing admins",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/create-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDto>> createAdmin(
            @RequestBody RegisterRequest request) {
        try {
            UserResponseDto responseDto = authService.createAdmin(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Admin created successfully", responseDto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
