package com.neo.nexora.service.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.neo.nexora.dto.CloudinaryUploadResponse;
import com.neo.nexora.entity.User;
import com.neo.nexora.exception.CloudinaryUploadException;
import com.neo.nexora.exception.ResourceNotFoundException;
import com.neo.nexora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link CloudinaryUploadService}.
 *
 * <p>Uses the Cloudinary Java SDK to upload, manage, and delete assets. Avatar uploads are stored
 * under the {@code avatars/} folder and are restricted to image files only. General uploads
 * support both images and videos. Each upload is given a unique public ID to prevent collisions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryUploadServiceImpl implements CloudinaryUploadService {

    private final Cloudinary cloudinary;
    private final UserRepository userRepository;

    @Value("${cloudinary.max-file-size:5242880}")
    private long maxFileSize;

    /**
     * Allowed MIME types for avatar uploads — images only.
     */
    @Value("${cloudinary.allowed-image-types:image/jpeg,image/png,image/webp,image/gif}")
    private String allowedImageTypes;

    /**
     * Allowed MIME types for general uploads — images and videos.
     */
    @Value("${cloudinary.allowed-media-types:image/jpeg,image/png,image/webp,image/gif,video/mp4,video/mpeg,video/quicktime,video/webm}")
    private String allowedMediaTypes;

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Validates file size and MIME type (images + videos) before uploading.
     * A unique public ID is generated to prevent filename collisions.
     */
    @Override
    public CloudinaryUploadResponse upload(MultipartFile file, String folder) {
        validateFile(file, allowedMediaTypes);

        String publicId = folder + "/" + UUID.randomUUID();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result =
                    cloudinary
                            .uploader()
                            .upload(
                                    file.getBytes(),
                                    ObjectUtils.asMap(
                                            "public_id", publicId,
                                            "folder", folder,
                                            "resource_type", "auto",
                                            "overwrite", true));

            log.info("Cloudinary upload succeeded: publicId={}, url={}", publicId, result.get("secure_url"));
            return mapToResponse(result);

        } catch (IOException e) {
            log.error("Cloudinary upload failed for publicId={}", publicId, e);
            throw new CloudinaryUploadException("Failed to upload file to Cloudinary", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uploads the file to the {@code avatars/} folder — images only (no videos).
     * Persists the resulting secure URL to the user record.
     */
    @Override
    public CloudinaryUploadResponse uploadAvatar(MultipartFile file, Long userId) {
        validateFile(file, allowedImageTypes);

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("User not found with id: " + userId));

        // Remove old avatar from Cloudinary if one exists
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
            deleteByUrl(user.getAvatarUrl());
        }

        String publicId = "avatars/" + UUID.randomUUID();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result =
                    cloudinary
                            .uploader()
                            .upload(
                                    file.getBytes(),
                                    ObjectUtils.asMap(
                                            "public_id", publicId,
                                            "folder", "avatars",
                                            "resource_type", "image",
                                            "overwrite", true));

            CloudinaryUploadResponse response = mapToResponse(result);

            user.setAvatarUrl(response.getSecureUrl());
            userRepository.save(user);

            log.info("Avatar updated for userId={}, url={}", userId, response.getSecureUrl());
            return response;

        } catch (IOException e) {
            log.error("Avatar upload failed for userId={}", userId, e);
            throw new CloudinaryUploadException("Failed to upload avatar to Cloudinary", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary asset deleted: publicId={}", publicId);
        } catch (IOException e) {
            log.warn("Failed to delete Cloudinary asset: publicId={}", publicId, e);
            throw new CloudinaryUploadException("Failed to delete asset from Cloudinary", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

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

    private CloudinaryUploadResponse mapToResponse(Map<String, Object> result) {
        return CloudinaryUploadResponse.builder()
                .publicId((String) result.get("public_id"))
                .secureUrl((String) result.get("secure_url"))
                .originalFilename((String) result.get("original_filename"))
                .format((String) result.get("format"))
                .width(result.get("width") instanceof Number n ? n.intValue() : null)
                .height(result.get("height") instanceof Number n ? n.intValue() : null)
                .bytes(result.get("bytes") instanceof Number n ? n.longValue() : null)
                .resourceType((String) result.get("resource_type"))
                .build();
    }

    /**
     * Extracts the Cloudinary public ID from a secure URL and deletes the asset.
     *
     * <p>Cloudinary secure URLs follow the pattern: {@code
     * https://res.cloudinary.com/<cloud>/image/upload/v<version>/<public_id>.<ext>}
     */
    public void deleteByUrl(String secureUrl) {
        try {
            String[] parts = secureUrl.split("/upload/");
            if (parts.length == 2) {
                String withVersion = parts[1];
                String publicIdWithExt = withVersion.replaceFirst("v\\d+/", "");
                String publicId = publicIdWithExt.contains(".")
                        ? publicIdWithExt.substring(0, publicIdWithExt.lastIndexOf('.'))
                        : publicIdWithExt;
                delete(publicId);
            }
        } catch (Exception e) {
            log.warn("Could not parse or delete old avatar from Cloudinary, url={}", secureUrl, e);
        }
    }


    @Override
    public String extractPublicId(String url) {
        if (url == null || url.isBlank()) {
            log.error("Cannot extract public ID from a null or blank URL");
            throw new CloudinaryUploadException("Cloudinary URL must not be null or blank");
        }

        String[] parts = url.split("/upload/");
        if (parts.length < 2 || parts[1].isBlank()) {
            log.error("URL does not contain expected '/upload/' segment: {}", url);
            throw new CloudinaryUploadException("Invalid Cloudinary URL — missing '/upload/' segment: " + url);
        }

        String afterUpload = parts[1];
        // Strip optional version segment (v1234567890/)
        if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
            afterUpload = afterUpload.substring(afterUpload.indexOf('/') + 1);
        }

        int dotIndex = afterUpload.lastIndexOf('.');
        if (dotIndex <= 0) {
            log.error("URL segment after '/upload/' has no recognisable extension: {}", afterUpload);
            throw new CloudinaryUploadException("Invalid Cloudinary URL — no file extension found: " + url);
        }

        return afterUpload.substring(0, dotIndex);
    }

}