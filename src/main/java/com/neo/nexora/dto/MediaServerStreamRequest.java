package com.neo.nexora.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaServerStreamRequest {

    @JsonProperty("fileUrl")
    @NotBlank(message = "fileUrl is required")
    private String fileUrl;
}


