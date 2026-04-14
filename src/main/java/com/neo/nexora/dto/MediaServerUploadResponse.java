package com.neo.nexora.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * DTO returned from Media Server upload endpoint.
 *
 * <p>Media Server returns a simpler response with fileUrl field.
 */
@Data
@Builder
public class MediaServerUploadResponse {

    /**
     * The URL of the uploaded file in MinIO bucket.
     */
    @JsonProperty("fileUrl")
    private String fileUrl;
}

