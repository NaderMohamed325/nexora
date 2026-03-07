package com.neo.nexora.service.slug;

import com.neo.nexora.dto.SlugRequestDto;
import com.neo.nexora.dto.SlugResponseDto;
import com.neo.nexora.entity.EntityType;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SlugService {

    /**
     * Creates a new slug entry.
     */
    SlugResponseDto createSlug(SlugRequestDto requestDto);

    /**
     * Resolves (looks up) a slug by its value and increments the click counter.
     */
    SlugResponseDto resolveSlug(String slug);

    /**
     * Retrieves a slug by its value without incrementing the counter.
     */
    SlugResponseDto getSlug(String slug);

    /**
     * Retrieves all slugs with pagination.
     */
    Page<SlugResponseDto> getAllSlugs(int page, int size);

    /**
     * Retrieves all slugs for a specific entity type with pagination.
     */
    Page<SlugResponseDto> getSlugsByEntityType(EntityType entityType, int page, int size);

    /**
     * Returns the top N slugs ordered by click count descending.
     */
    List<SlugResponseDto> getTopSlugs(int limit);

    /**
     * Deletes a slug by its value.
     */
    void deleteSlug(String slug);

    /**
     * Checks whether a slug value is already taken.
     */
    boolean slugExists(String slug);
}

