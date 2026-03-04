package com.neo.nexora.controller;

import com.neo.nexora.dto.ApiResponse;
import com.neo.nexora.dto.CloudinaryUploadResponse;
import com.neo.nexora.dto.UserResponseDto;
import com.neo.nexora.dto.UserUpdateDto;
import com.neo.nexora.service.cloudinary.CloudinaryUploadService;
import com.neo.nexora.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management — CRUD operations")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;
    private final CloudinaryUploadService cloudinaryUploadService;

    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a user by their unique identifier")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable Long id) {
        UserResponseDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    @Operation(summary = "Get user by email", description = "Retrieves a user by their email address")
    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserByEmail(@PathVariable String email) {
        UserResponseDto user = userService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    @Operation(summary = "Get all users", description = "Retrieves all registered users")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @Operation(
            summary = "Search users",
            description = "Searches for users whose username matches the given pattern")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> searchUsers(
            @RequestParam String username) {
        List<UserResponseDto> users = userService.getUsersLike(username);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @Operation(
            summary = "Update current user",
            description = "Updates the authenticated user's details using the JWT token")
    @PutMapping
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUser(
            @RequestHeader("Authorization") String authHeader, @RequestBody UserUpdateDto userUpdateDto) {
        String token = authHeader.substring(7);
        UserResponseDto updatedUser = userService.updateUser(token, userUpdateDto);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));
    }

    @Operation(
            summary = "Delete user by ID",
            description = "Permanently deletes a user. Only accessible by admins")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }

    @Operation(
            summary = "Deactivate account",
            description = "Deactivates the user account. The account can be reactivated later")
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(@PathVariable Long id) {
        userService.deactivateAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Account deactivated successfully"));
    }


    @Operation(
            summary = "Reactivate account",
            description = "Reactivates a previously deactivated or deletion-scheduled account")
    @PutMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivateAccount(@PathVariable Long id) {
        userService.reactivateAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Account reactivated successfully"));
    }

    @Operation(
            summary = "Upload avatar",
            description = "Uploads a profile picture for the given user. "
                    + "Accepts JPEG, PNG, WebP or GIF up to 5 MB. "
                    + "Replaces any previously uploaded avatar.")
    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<CloudinaryUploadResponse>> uploadAvatar(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        CloudinaryUploadResponse response = cloudinaryUploadService.uploadAvatar(file, id);
        return ResponseEntity.ok(ApiResponse.success("Avatar uploaded successfully", response));
    }
}
