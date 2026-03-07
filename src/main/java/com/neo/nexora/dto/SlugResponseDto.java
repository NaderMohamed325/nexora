package com.neo.nexora.dto;

import com.neo.nexora.entity.EntityType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlugResponseDto {
    private Long id;
    private String slug;
    private EntityType entityType;
    private Long clickCount;
}

