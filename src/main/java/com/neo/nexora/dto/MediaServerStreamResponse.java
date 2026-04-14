package com.neo.nexora.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MediaServerStreamResponse {

    @JsonProperty("message")
    private String message;

    @JsonProperty("playlistPath")
    private String playlistPath;

    @JsonProperty("playlistUrl")
    private String playlistUrl;
}

