package com.neo.nexora.controller;

import com.neo.nexora.dto.*;
import com.neo.nexora.entity.Role;
import com.neo.nexora.entity.User;
import com.neo.nexora.repository.UserRepository;
import com.neo.nexora.security.JwtUtil;
import com.neo.nexora.service.auth.PasswordResetService;
import com.neo.nexora.service.auth.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, logout, and password management")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final TokenBlacklistService tokenBlacklistService;
  private final PasswordResetService passwordResetService;

  @Value("${jwt.expiration}")
  private Long jwtExpiration;

  @Operation(
      summary = "Register a new user",
      description = "Creates a new user account and returns a JWT token")
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Username already exists"));
    }

    if (userRepository.existsByEmail(request.getEmail())) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Email already exists"));
    }

    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setRole(Role.USER);

    userRepository.save(user);

    String token = jwtUtil.generateToken(user);
    AuthResponse authResponse = buildAuthResponse(token, user);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Registration successful", authResponse));
  }

  @Operation(summary = "Login", description = "Authenticates a user and returns a JWT token")
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
    try {
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  request.getUsername(), request.getPassword()));

      User user = (User) authentication.getPrincipal();
      String token = jwtUtil.generateToken(user);
      AuthResponse authResponse = buildAuthResponse(token, user);

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
    tokenBlacklistService.blacklist(token);

    return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
  }

  @Operation(
      summary = "Request password reset",
      description = "Generates a password reset token for the given email")
  @PostMapping("/password-reset/request")
  public ResponseEntity<ApiResponse<String>> requestPasswordReset(
      @RequestBody PasswordResetRequest request) {
    try {
      String resetToken = passwordResetService.createResetToken(request.getEmail());
      // In production, send the token via email instead of returning it
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
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Passwords do not match"));
    }

    try {
      passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
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
      @AuthenticationPrincipal User user, @RequestBody ChangePasswordRequest request) {

    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Passwords do not match"));
    }

    try {
      passwordResetService.changePassword(
          user, request.getCurrentPassword(), request.getNewPassword());
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
    if (userRepository.existsByUsername(request.getUsername())) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Username already exists"));
    }

    if (userRepository.existsByEmail(request.getEmail())) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Email already exists"));
    }

    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setRole(Role.ADMIN);

    userRepository.save(user);

    UserResponseDto responseDto =
        UserResponseDto.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole())
            .build();

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Admin created successfully", responseDto));
  }

  private AuthResponse buildAuthResponse(String token, User user) {
    return AuthResponse.builder()
        .token(token)
        .tokenType("Bearer")
        .username(user.getUsername())
        .email(user.getEmail())
        .role(user.getRole().name())
        .expiresIn(jwtExpiration / 1000) // convert to seconds
        .build();
  }
}
