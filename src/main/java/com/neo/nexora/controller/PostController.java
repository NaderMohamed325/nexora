package com.neo.nexora.controller;

import com.neo.nexora.dto.ApiResponse;
import com.neo.nexora.dto.PostMediaUpdateDto;
import com.neo.nexora.dto.PostRequestDto;
import com.neo.nexora.dto.PostResponseDto;
import com.neo.nexora.service.post.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Post management — create, read, update, and delete operations")
@SecurityRequirement(name = "Bearer Authentication")
public class PostController {

    private final PostService postService;

    @Operation(
            summary = "Create a new post",
            description = "Creates a new post with optional media files for the authenticated user")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PostResponseDto>> createPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestPart("post") PostRequestDto requestDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        List<MultipartFile> fileList = files != null ? files : List.of();
        PostResponseDto created = postService.createPost(userDetails, idempotencyKey, requestDto, fileList);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post created successfully", created));
    }

    @Operation(
            summary = "Get all posts",
            description = "Retrieves a paginated list of all posts")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostResponseDto>>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PostResponseDto> posts = postService.getAllPosts(page, size);
        return ResponseEntity.ok(ApiResponse.success("Posts retrieved successfully", posts));
    }

    @Operation(
            summary = "Get post by ID",
            description = "Retrieves a single post by its unique identifier")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponseDto>> getPostById(@PathVariable Long id) {
        PostResponseDto post = postService.getPostById(id);
        return ResponseEntity.ok(ApiResponse.success("Post retrieved successfully", post));
    }

    @Operation(
            summary = "Get posts by user ID",
            description = "Retrieves all posts created by a specific user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<PostResponseDto>>> getPostsByUserId(@PathVariable Long userId) {
        List<PostResponseDto> posts = postService.getPostsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Posts retrieved successfully", posts));
    }

    @Operation(
            summary = "Search posts",
            description = "Searches posts by keyword in title or content")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<PostResponseDto>>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PostResponseDto> posts = postService.searchPosts(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", posts));
    }

    @Operation(
            summary = "Get posts by date range",
            description = "Retrieves a paginated list of posts created within the given date range")
    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<Page<PostResponseDto>>> getPostsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PostResponseDto> posts = postService.getPostsByDateRange(startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success("Posts retrieved successfully", posts));
    }

    @Operation(
            summary = "Get posts by user ID and date range",
            description = "Retrieves paginated posts for a specific user within a date range")
    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<ApiResponse<Page<PostResponseDto>>> getPostsByUserIdAndDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PostResponseDto> posts = postService.getPostsByUserIdAndDateRange(userId, startDate, endDate, page, size);
        return ResponseEntity.ok(ApiResponse.success("Posts retrieved successfully", posts));
    }

    @Operation(
            summary = "Update a post",
            description = "Updates the title, content, and media of an existing post. Only the post owner can update it")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PostResponseDto>> updatePost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestPart("post") PostRequestDto requestDto,
            @RequestPart(value = "mediaUpdate", required = false) PostMediaUpdateDto mediaUpdateDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        PostMediaUpdateDto update = mediaUpdateDto != null ? mediaUpdateDto : new PostMediaUpdateDto();
        List<MultipartFile> fileList = files != null ? files : List.of();
        PostResponseDto updated = postService.updatePost(userDetails, id, requestDto, update, fileList);
        return ResponseEntity.ok(ApiResponse.success("Post updated successfully", updated));
    }

    @Operation(
            summary = "Delete a post",
            description = "Deletes a post by ID. Only the post owner or an admin can delete it")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        postService.deletePostById(userDetails, id);
        return ResponseEntity.ok(ApiResponse.success("Post deleted successfully"));
    }
}

