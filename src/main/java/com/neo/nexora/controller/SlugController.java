package com.neo.nexora.controller;

import com.neo.nexora.dto.ApiResponse;
import com.neo.nexora.dto.SlugRequestDto;
import com.neo.nexora.dto.SlugResponseDto;
import com.neo.nexora.entity.EntityType;
import com.neo.nexora.service.slug.SlugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slugs")
@RequiredArgsConstructor
@Tag(name = "Slugs", description = "Slug management — create, resolve, and track click counts for entity slugs")
@SecurityRequirement(name = "Bearer Authentication")
public class SlugController {

    private final SlugService slugService;

    @Operation(
            summary = "Create a slug",
            description = "Creates a new unique slug for a given entity type. Requires ADMIN role")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SlugResponseDto>> createSlug(
            @Valid @RequestBody SlugRequestDto requestDto) {
        SlugResponseDto created = slugService.createSlug(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Slug created successfully", created));
    }

    @Operation(
            summary = "Resolve a slug",
            description = "Looks up a slug by value and increments its click counter. Returns the slug details")
    @GetMapping("/{slug}/resolve")
    public ResponseEntity<ApiResponse<SlugResponseDto>> resolveSlug(@PathVariable String slug) {
        SlugResponseDto resolved = slugService.resolveSlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Slug resolved successfully", resolved));
    }

    @Operation(
            summary = "Get a slug",
            description = "Retrieves a slug by its value without incrementing the click counter")
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<SlugResponseDto>> getSlug(@PathVariable String slug) {
        SlugResponseDto found = slugService.getSlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Slug retrieved successfully", found));
    }

    @Operation(
            summary = "Check slug availability",
            description = "Returns whether the given slug value is already in use")
    @GetMapping("/{slug}/exists")
    public ResponseEntity<ApiResponse<Boolean>> slugExists(@PathVariable String slug) {
        boolean exists = slugService.slugExists(slug);
        return ResponseEntity.ok(ApiResponse.success(
                exists ? "Slug is already taken" : "Slug is available", exists));
    }

    @Operation(
            summary = "Get all slugs",
            description = "Retrieves all slugs with pagination, ordered by click count descending")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<SlugResponseDto>>> getAllSlugs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SlugResponseDto> slugs = slugService.getAllSlugs(page, size);
        return ResponseEntity.ok(ApiResponse.success("Slugs retrieved successfully", slugs));
    }

    @Operation(
            summary = "Get slugs by entity type",
            description = "Retrieves a paginated list of slugs filtered by entity type")
    @GetMapping("/type/{entityType}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<SlugResponseDto>>> getSlugsByEntityType(
            @PathVariable EntityType entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SlugResponseDto> slugs = slugService.getSlugsByEntityType(entityType, page, size);
        return ResponseEntity.ok(ApiResponse.success("Slugs retrieved successfully", slugs));
    }

    @Operation(
            summary = "Get top slugs",
            description = "Returns the most-clicked slugs up to the specified limit")
    @GetMapping("/top")
    public ResponseEntity<ApiResponse<List<SlugResponseDto>>> getTopSlugs(
            @RequestParam(defaultValue = "10") int limit) {
        List<SlugResponseDto> topSlugs = slugService.getTopSlugs(limit);
        return ResponseEntity.ok(ApiResponse.success("Top slugs retrieved successfully", topSlugs));
    }

    @Operation(
            summary = "Delete a slug",
            description = "Deletes a slug by its value. Requires ADMIN role")
    @DeleteMapping("/{slug}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSlug(@PathVariable String slug) {
        slugService.deleteSlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Slug deleted successfully"));
    }
}

