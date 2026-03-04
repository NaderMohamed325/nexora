package com.neo.nexora.service.auth;

import com.neo.nexora.dto.*;

/**
 * Service interface for authentication-related operations including registration, login, logout,
 * password management, and admin creation.
 */
public interface AuthService {

    /**
     * Registers a new user and returns an authentication response with a JWT token.
     *
     * @param request the registration request containing username, email, and password
     * @return the authentication response with token and user details
     * @throws IllegalArgumentException if the username or email already exists
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request the login request containing username and password
     * @return the authentication response with token and user details
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are
     *                                                                             invalid
     */
    AuthResponse login(LoginRequest request);

    /**
     * Blacklists the given JWT token to effectively log the user out.
     *
     * @param token the raw JWT token (without "Bearer " prefix)
     */
    void logout(String token);

    /**
     * Generates a password reset token for the given email address.
     *
     * @param request the password reset request containing the user's email
     * @return the generated reset token string
     * @throws RuntimeException if no account is found with the provided email
     */
    String requestPasswordReset(PasswordResetRequest request);

    /**
     * Resets the user's password using the provided reset token.
     *
     * @param request the password reset confirmation containing token, new password, and confirmation
     * @throws IllegalArgumentException if the passwords do not match
     * @throws RuntimeException         if the token is invalid, expired, or already used
     */
    void confirmPasswordReset(PasswordResetConfirm request);

    /**
     * Changes the password of the given authenticated user.
     *
     * @param username the username of the authenticated user
     * @param request  the change password request containing current and new passwords
     * @throws IllegalArgumentException if the new password and confirmation do not match
     * @throws RuntimeException         if the current password is incorrect
     */
    void changePassword(String username, ChangePasswordRequest request);

    /**
     * Creates a new user with ADMIN role. Only callable by existing admins.
     *
     * @param request the registration request containing admin details
     * @return the created admin's details as {@link UserResponseDto}
     * @throws IllegalArgumentException if the username or email already exists
     */
    UserResponseDto createAdmin(RegisterRequest request);
}
