package com.neo.nexora.controller;

import com.neo.nexora.dto.ApiResponse;
import com.neo.nexora.dto.MediaServerPathUploadRequest;
import com.neo.nexora.dto.MediaServerStreamRequest;
import com.neo.nexora.dto.MediaServerStreamResponse;
import com.neo.nexora.dto.MediaServerTusUploadResponse;
import com.neo.nexora.service.cloudinary.CloudinaryUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Media Server integration endpoints for TUS upload and video streaming")
@SecurityRequirement(name = "Bearer Authentication")
public class MediaController {

    private final CloudinaryUploadService cloudinaryUploadService;

    @Operation(
            summary = "Upload video via TUS",
            description = "Triggers Media Server TUS upload flow for a server-side file path")
    @PostMapping("/video/tus")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MediaServerTusUploadResponse>> uploadVideoViaTus(
            @Valid @RequestBody MediaServerPathUploadRequest request) {
        MediaServerTusUploadResponse response = cloudinaryUploadService.uploadVideoViaTus(request.getFilePath());
        return ResponseEntity.ok(ApiResponse.success("Video upload started successfully", response));
    }

    @Operation(
            summary = "Generate HLS stream",
            description = "Requests Media Server to transcode an uploaded video and produce streaming assets")
    @PostMapping("/video/stream")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MediaServerStreamResponse>> generateVideoStream(
            @Valid @RequestBody MediaServerStreamRequest request) {
        MediaServerStreamResponse response = cloudinaryUploadService.generateVideoStream(request.getFileUrl());
        return ResponseEntity.ok(ApiResponse.success("Video stream generated successfully", response));
    }
}

