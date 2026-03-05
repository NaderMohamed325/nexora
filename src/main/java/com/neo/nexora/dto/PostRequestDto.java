package com.neo.nexora.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRequestDto {
    @NonNull
    @Size(min = 3, max = 255)
    private String title;
    @Size(min = 3, max = 255)
    private String content;
}
