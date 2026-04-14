package com.neo.nexora.service.cloudinary;

import com.neo.nexora.dto.CloudinaryUploadResponse;
import com.neo.nexora.dto.MediaServerStreamResponse;
import com.neo.nexora.dto.MediaServerTusUploadResponse;
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
     * Triggers a Media Server TUS upload using a server-side file path.
     *
     * @param filePath local path known by the Media Server worker
     * @return upload details including resumable upload URL
     */
    MediaServerTusUploadResponse uploadVideoViaTus(String filePath);

    /**
     * Deletes an asset from Cloudinary by its public ID.
     *
     * @param publicId the Cloudinary public ID of the asset to remove
     */
    void delete(String publicId);

    /**
     * Requests Media Server to generate HLS assets for a video in object storage.
     *
     * @param fileUrl the stored file URL to transcode
     * @return stream generation details, including playlist URL when available
     */
    MediaServerStreamResponse generateVideoStream(String fileUrl);


    String extractPublicId(String cloudinaryUrl);


}



