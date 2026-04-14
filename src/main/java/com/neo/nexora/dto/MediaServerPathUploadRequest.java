package com.neo.nexora.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaServerPathUploadRequest {

    @JsonProperty("filepath")
    @NotBlank(message = "filepath is required")
    private String filePath;
}


