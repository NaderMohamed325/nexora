package com.neo.nexora.service.cloudinary;

import com.neo.nexora.dto.CloudinaryUploadResponse;
import com.neo.nexora.dto.MediaServerUploadResponse;
import com.neo.nexora.entity.User;
import com.neo.nexora.exception.CloudinaryUploadException;
import com.neo.nexora.exception.ResourceNotFoundException;
import com.neo.nexora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of {@link CloudinaryUploadService} using Media Server API.
 *
 * <p>Uses HTTP multipart uploads to the Media Server instead of Cloudinary SDK.
 * Avatar uploads are stored under the {@code avatars/} folder and are restricted to image files only.
 * General uploads support both images and videos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryUploadServiceImpl implements CloudinaryUploadService {

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;

    @Value("${media-server.url:http://localhost:3000}")
    private String mediaServerUrl;

    @Value("${media-server.max-file-size:5242880}")
    private long maxFileSize;

    /**
     * Allowed MIME types for avatar uploads — images only.
     */
    @Value("${media-server.allowed-image-types:image/jpeg,image/png,image/webp,image/gif}")
    private String allowedImageTypes;

    /**
     * Allowed MIME types for general uploads — images and videos.
     */
    @Value("${media-server.allowed-media-types:image/jpeg,image/png,image/webp,image/gif,video/mp4,video/mpeg,video/quicktime,video/webm}")
    private String allowedMediaTypes;

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Validates file size and MIME type (images + videos) before uploading to Media Server.
     */
    @Override
    public CloudinaryUploadResponse upload(MultipartFile file, String folder) {
        validateFile(file, allowedMediaTypes);

        try {
            MediaServerUploadResponse response = uploadToMediaServer(file);
            log.info("Media Server upload succeeded: url={}", response.getFileUrl());
            return mapToCloudinaryResponse(response);

        } catch (RestClientException e) {
            log.error("Media Server upload failed", e);
            throw new CloudinaryUploadException("Failed to upload file to Media Server", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uploads the file to Media Server and saves the URL to the user record.
     */
    @Override
    public CloudinaryUploadResponse uploadAvatar(MultipartFile file, Long userId) {
        validateFile(file, allowedImageTypes);

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("User not found with id: " + userId));

        // Remove old avatar if one exists (Media Server doesn't require deletion, URL will just be orphaned)
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
            log.info("Previous avatar URL will be orphaned: {}", user.getAvatarUrl());
        }

        try {
            MediaServerUploadResponse response = uploadToMediaServer(file);
            CloudinaryUploadResponse cloudinaryResponse = mapToCloudinaryResponse(response);

            user.setAvatarUrl(cloudinaryResponse.getSecureUrl());
            userRepository.save(user);

            log.info("Avatar updated for userId={}, url={}", userId, cloudinaryResponse.getSecureUrl());
            return cloudinaryResponse;

        } catch (RestClientException e) {
            log.error("Avatar upload failed for userId={}", userId, e);
            throw new CloudinaryUploadException("Failed to upload avatar to Media Server", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Media Server doesn't require explicit deletion; URLs are just orphaned.
     * This method logs the deletion request but doesn't perform any action.
     */
    @Override
    public void delete(String publicId) {
        // Media Server/MinIO doesn't require explicit deletion from application side
        // Files can be cleaned up via MinIO's lifecycle policies or manual intervention
        log.info("Delete request for publicId={} (no-op in Media Server)", publicId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Uploads a file to the Media Server API.
     *
     * @param file the file to upload
     * @return the Media Server response with fileUrl
     */
    private MediaServerUploadResponse uploadToMediaServer(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            // Create a wrapper that RestTemplate can serialize as multipart
            body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String uploadUrl = mediaServerUrl + "/api/v1/upload/";

            MediaServerUploadResponse response = restTemplate.postForObject(
                    uploadUrl,
                    requestEntity,
                    MediaServerUploadResponse.class
            );

            if (response == null || response.getFileUrl() == null) {
                throw new CloudinaryUploadException("Invalid response from Media Server");
            }

            return response;
        } catch (IOException e) {
            throw new CloudinaryUploadException("Failed to prepare file for upload", e);
        }
    }

    /**
     * Converts Media Server response to CloudinaryUploadResponse for backward compatibility.
     *
     * @param mediaResponse the Media Server response
     * @return CloudinaryUploadResponse with extracted metadata
     */
    private CloudinaryUploadResponse mapToCloudinaryResponse(MediaServerUploadResponse mediaResponse) {
        String fileUrl = mediaResponse.getFileUrl();
        String filename = extractFilename(fileUrl);

        return CloudinaryUploadResponse.builder()
                .publicId(filename)
                .secureUrl(fileUrl)
                .originalFilename(filename)
                .format(extractFormat(filename))
                .resourceType("auto")
                .build();
    }

    /**
     * Validates file size and MIME type against the provided allowed types.
     *
     * @param file         the file to validate
     * @param allowedTypes comma-separated list of allowed MIME types
     */
    private void validateFile(MultipartFile file, String allowedTypes) {
        if (file == null || file.isEmpty()) {
            throw new CloudinaryUploadException("Upload file must not be empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new CloudinaryUploadException(
                    "File size exceeds the maximum allowed size of " + (maxFileSize / 1024 / 1024) + " MB");
        }

        String contentType = file.getContentType();
        List<String> allowed = Arrays.asList(allowedTypes.split(","));
        if (contentType == null || !allowed.contains(contentType.trim())) {
            throw new CloudinaryUploadException(
                    "Unsupported file type '" + contentType + "'. Allowed: " + allowedTypes);
        }
    }

    /**
     * Extracts the filename from a MinIO URL.
     *
     * <p>MinIO URLs typically follow: {@code http://localhost:9000/bucket/filename.ext}
     */
    private String extractFilename(String url) {
        if (url == null || url.isBlank()) {
            return "unknown";
        }
        String[] parts = url.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "unknown";
    }

    /**
     * Extracts the file format/extension from a filename.
     *
     * @param filename the filename
     * @return the file extension (e.g., "jpg", "png")
     */
    private String extractFormat(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    @Override
    public String extractPublicId(String url) {
        if (url == null || url.isBlank()) {
            log.error("Cannot extract public ID from a null or blank URL");
            throw new CloudinaryUploadException("Media Server URL must not be null or blank");
        }

        // For Media Server/MinIO URLs, the public ID is simply the filename
        // URL format: http://localhost:9000/bucket/filename.ext
        return extractFilename(url);
    }

}