package com.neo.nexora.controller;

import com.neo.nexora.dto.ApiResponse;
import com.neo.nexora.dto.LikeResponseDto;
import com.neo.nexora.service.like.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
@Tag(name = "Likes", description = "Like management — toggle and query likes on posts and comments")
@SecurityRequirement(name = "Bearer Authentication")
public class LikeController {

    private final LikeService likeService;

    // ── Posts ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Toggle like on a post",
            description = "Likes the post if not already liked, otherwise removes the like")
    @PostMapping("/post/{postId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LikeResponseDto>> togglePostLike(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId) {
        LikeResponseDto response = likeService.togglePostLike(userDetails, postId);
        String message = response.getLikeId() != null ? "Post liked successfully" : "Post unliked successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "Get like count for a post",
            description = "Returns the total number of likes on the specified post")
    @GetMapping("/post/{postId}/count")
    public ResponseEntity<ApiResponse<Long>> getPostLikeCount(@PathVariable Long postId) {
        long count = likeService.getPostLikeCount(postId);
        return ResponseEntity.ok(ApiResponse.success("Post like count retrieved", count));
    }

    @Operation(summary = "Check if current user liked a post",
            description = "Returns true if the authenticated user has liked the specified post")
    @GetMapping("/post/{postId}/status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> hasUserLikedPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long postId) {
        boolean liked = likeService.hasUserLikedPost(userDetails, postId);
        return ResponseEntity.ok(ApiResponse.success("Like status retrieved", liked));
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    @Operation(summary = "Toggle like on a comment",
            description = "Likes the comment if not already liked, otherwise removes the like")
    @PostMapping("/comment/{commentId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LikeResponseDto>> toggleCommentLike(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId) {
        LikeResponseDto response = likeService.toggleCommentLike(userDetails, commentId);
        String message = response.getLikeId() != null ? "Comment liked successfully" : "Comment unliked successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "Get like count for a comment",
            description = "Returns the total number of likes on the specified comment")
    @GetMapping("/comment/{commentId}/count")
    public ResponseEntity<ApiResponse<Long>> getCommentLikeCount(@PathVariable Long commentId) {
        long count = likeService.getCommentLikeCount(commentId);
        return ResponseEntity.ok(ApiResponse.success("Comment like count retrieved", count));
    }

    @Operation(summary = "Check if current user liked a comment",
            description = "Returns true if the authenticated user has liked the specified comment")
    @GetMapping("/comment/{commentId}/status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> hasUserLikedComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId) {
        boolean liked = likeService.hasUserLikedComment(userDetails, commentId);
        return ResponseEntity.ok(ApiResponse.success("Like status retrieved", liked));
    }
}

