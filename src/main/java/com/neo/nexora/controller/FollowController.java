package com.neo.nexora.controller;

import com.neo.nexora.dto.ApiResponse;
import com.neo.nexora.service.follow.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
@Tag(name = "Follow", description = "Follow management - follow or unfollow users")
@SecurityRequirement(name = "Bearer Authentication")
public class FollowController {
    private final FollowService followService;

    @Operation(
            summary = "Follow a user",
            description = "Starts following the target user for the authenticated account")
    @PostMapping("/{followeeId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> followUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long followeeId) {
        try {
            followService.followUser(userDetails, followeeId);
            return ResponseEntity.ok(ApiResponse.success("User followed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
            summary = "Unfollow a user",
            description = "Stops following the target user for the authenticated account")
    @DeleteMapping("/{followeeId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long followeeId) {
        try {
            followService.unfollowUser(userDetails, followeeId);
            return ResponseEntity.ok(ApiResponse.success("User unfollowed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

}
