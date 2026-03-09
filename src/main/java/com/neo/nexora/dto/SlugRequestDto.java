package com.neo.nexora.dto;

import com.neo.nexora.entity.EntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlugRequestDto {

    @NotBlank(message = "Slug must not be blank")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
             message = "Slug must be lowercase alphanumeric words separated by hyphens")
    private String slug;

    @NotNull(message = "Entity type must not be null")
    private EntityType entityType;
}

