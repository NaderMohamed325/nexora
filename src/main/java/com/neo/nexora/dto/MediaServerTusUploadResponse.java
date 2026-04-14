package com.neo.nexora.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MediaServerTusUploadResponse {

    @JsonProperty("uploadUrl")
    private String uploadUrl;
}

