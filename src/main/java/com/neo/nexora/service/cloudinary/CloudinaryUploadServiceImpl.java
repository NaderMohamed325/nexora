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
 * under the {@code avatars/} folder. Each upload is given a unique public ID to prevent collisions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryUploadServiceImpl implements CloudinaryUploadService {

    private final Cloudinary cloudinary;
    private final UserRepository userRepository;

    @Value("${cloudinary.max-file-size:5242880}")
    private long maxFileSize;

    @Value("${cloudinary.allowed-types:image/jpeg,image/png,image/webp,image/gif}")
    private String allowedTypes;

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Validates file size and MIME type before uploading. A unique public ID is generated to
     * prevent filename collisions.
     */
    @Override
    public CloudinaryUploadResponse upload(MultipartFile file, String folder) {
        validateFile(file);

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
     * <p>Uploads the file to the {@code avatars/} folder and persists the resulting secure URL to
     * the user record.
     */
    @Override
    public CloudinaryUploadResponse uploadAvatar(MultipartFile file, Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("User not found with id: " + userId));

        // Remove old avatar from Cloudinary if one exists
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
            deleteByUrl(user.getAvatarUrl());
        }

        CloudinaryUploadResponse response = upload(file, "avatars");

        user.setAvatarUrl(response.getSecureUrl());
        userRepository.save(user);

        log.info("Avatar updated for userId={}, url={}", userId, response.getSecureUrl());
        return response;
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

    private void validateFile(MultipartFile file) {
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
    private void deleteByUrl(String secureUrl) {
        try {
            // Strip extension and extract public_id (everything after /upload/v<version>/)
            String[] parts = secureUrl.split("/upload/");
            if (parts.length == 2) {
                String withVersion = parts[1]; // e.g. "v1234567/avatars/uuid.jpg"
                // Remove version prefix if present
                String publicIdWithExt = withVersion.replaceFirst("v\\d+/", "");
                // Remove extension
                String publicId = publicIdWithExt.contains(".")
                        ? publicIdWithExt.substring(0, publicIdWithExt.lastIndexOf('.'))
                        : publicIdWithExt;
                delete(publicId);
            }
        } catch (Exception e) {
            log.warn("Could not parse or delete old avatar from Cloudinary, url={}", secureUrl, e);
        }
    }
}


