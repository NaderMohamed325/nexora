package com.neo.nexora.controller;

import com.neo.nexora.dto.ApiResponse;
import com.neo.nexora.dto.UserResponseDto;
import com.neo.nexora.dto.UserUpdateDto;
import com.neo.nexora.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
@Tag(name = "Users", description = "User management — CRUD operations")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

  private final UserService userService;

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
      description = "Deletes a user by their unique identifier")
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')") // Only admins can delete users
  public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
    userService.deleteUserById(id);
    return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
  }
}
