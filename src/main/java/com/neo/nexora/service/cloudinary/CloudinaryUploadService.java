package com.neo.nexora.service.cloudinary;

import com.neo.nexora.dto.CloudinaryUploadResponse;
import com.neo.nexora.exception.CloudinaryUploadException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for uploading and managing assets on Cloudinary.
 */
public interface CloudinaryUploadService {

    /**
     * Uploads a file to Cloudinary under the specified folder.
     *
     * @param file   the multipart file to upload
     * @param folder the target folder in Cloudinary (e.g. {@code "avatars"})
     * @return a {@link CloudinaryUploadResponse} containing the asset metadata
     * @throws CloudinaryUploadException if the upload fails
     */
    CloudinaryUploadResponse upload(MultipartFile file, String folder);

    /**
     * Uploads a user avatar and links it to the given user account.
     *
     * <p>Saves the resulting secure URL to {@code User.avatarUrl} in the database.
     *
     * @param file   the avatar image file
     * @param userId the ID of the user whose avatar should be updated
     * @return a {@link CloudinaryUploadResponse} with the uploaded avatar metadata
     */
    CloudinaryUploadResponse uploadAvatar(MultipartFile file, Long userId);

    /**
     * Deletes an asset from Cloudinary by its public ID.
     *
     * @param publicId the Cloudinary public ID of the asset to remove
     */
    void delete(String publicId);
}



