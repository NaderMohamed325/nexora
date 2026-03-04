package com.neo.nexora.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * DTO returned after a successful Cloudinary upload.
 *
 * <p>Contains all information needed by the client to reference or display the uploaded asset.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudinaryUploadResponse {

    /** Cloudinary public ID — use this to transform or delete the asset later. */
    private String publicId;

    /** Secure HTTPS URL of the uploaded asset. */
    private String secureUrl;

    /** Original filename as reported by Cloudinary. */
    private String originalFilename;

    /** File format (e.g. {@code jpg}, {@code png}, {@code webp}). */
    private String format;

    /** Width in pixels (images only). */
    private Integer width;

    /** Height in pixels (images only). */
    private Integer height;

    /** File size in bytes. */
    private Long bytes;

    /** Resource type: {@code image}, {@code video}, or {@code raw}. */
    private String resourceType;
}

