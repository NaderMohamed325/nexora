package com.neo.nexora.controller;

import com.neo.nexora.dto.ApiResponse;
import com.neo.nexora.dto.CommentRequestDto;
import com.neo.nexora.dto.CommentResponseDto;
import com.neo.nexora.service.comment.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Comment management — add, view, update, and delete comments on posts")
@SecurityRequirement(name = "Bearer Authentication")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "Add a comment to a post",
            description = "Adds a new comment to the specified post for the authenticated user")
    @PostMapping("/post/{postId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommentResponseDto>> addComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequestDto requestDto) {
        CommentResponseDto comment = commentService.addComment(userDetails, idempotencyKey, postId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added successfully", comment));
    }

    @Operation(summary = "Get comments for a post",
            description = "Retrieves a paginated list of comments for the specified post")
    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponse<Page<CommentResponseDto>>> getCommentsByPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CommentResponseDto> comments = commentService.getCommentsByPostId(postId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Comments retrieved successfully", comments));
    }

    @Operation(summary = "Get comments by user",
            description = "Retrieves all comments made by a specific user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<CommentResponseDto>>> getCommentsByUser(
            @PathVariable Long userId) {
        List<CommentResponseDto> comments = commentService.getCommentsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Comments retrieved successfully", comments));
    }

    @Operation(summary = "Update a comment",
            description = "Updates the content of a comment. Only the author or an admin can update a comment")
    @PutMapping("/{commentId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommentResponseDto>> updateComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto requestDto) {
        CommentResponseDto updated = commentService.updateComment(userDetails, commentId, requestDto);
        return ResponseEntity.ok(ApiResponse.success("Comment updated successfully", updated));
    }

    @Operation(summary = "Delete a comment",
            description = "Deletes a comment. Only the author or an admin can delete a comment")
    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long commentId) {
        commentService.deleteComment(userDetails, commentId);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully"));
    }
}

