package com.neo.nexora.service.auth;

import com.neo.nexora.dto.*;
import com.neo.nexora.entity.Role;
import com.neo.nexora.entity.User;
import com.neo.nexora.repository.UserRepository;
import com.neo.nexora.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordResetService passwordResetService;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());

        String token = jwtUtil.generateToken(user);
        return buildAuthResponse(token, user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = (User) authentication.getPrincipal();
        String token = jwtUtil.generateToken(user);
        log.info("User logged in successfully: {}", user.getUsername());

        return buildAuthResponse(token, user);
    }

    @Override
    public void logout(String token) {
        tokenBlacklistService.blacklist(token);
        log.info("Token blacklisted successfully");
    }

    @Override
    public String requestPasswordReset(PasswordResetRequest request) {
        return passwordResetService.createResetToken(request.getEmail());
    }

    @Override
    public void confirmPasswordReset(PasswordResetConfirm request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
    }

    @Override
    public void changePassword(String username, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        passwordResetService.changePassword(
                user, request.getCurrentPassword(), request.getNewPassword());
        log.info("Password changed successfully for user: {}", username);
    }

    @Override
    @Transactional
    public UserResponseDto createAdmin(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ADMIN);

        userRepository.save(user);
        log.info("Admin created successfully: {}", user.getUsername());

        return UserResponseDto.fromEntity(user);
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .expiresIn(jwtExpiration / 1000)
                .build();
    }
}
