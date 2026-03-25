package com.neo.nexora.service.slug;

import com.neo.nexora.dto.SlugRequestDto;
import com.neo.nexora.dto.SlugResponseDto;
import com.neo.nexora.entity.EntityType;
import com.neo.nexora.entity.Slug;
import com.neo.nexora.exception.ResourceNotFoundException;
import com.neo.nexora.repository.SlugRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlugServiceImpl implements SlugService {

    private final SlugRepository slugRepository;

    private SlugResponseDto mapToResponseDto(Slug slug) {
        return new SlugResponseDto(
                slug.getId(),
                slug.getSlug(),
                slug.getEntityType(),
                slug.getClickCount()
        );
    }

    @Override
    @Transactional
    public SlugResponseDto createSlug(SlugRequestDto requestDto) {
        if (slugRepository.existsBySlug(requestDto.getSlug())) {
            throw new IllegalArgumentException("Slug '" + requestDto.getSlug() + "' is already taken");
        }
        Slug slug = new Slug();
        slug.setSlug(requestDto.getSlug());
        slug.setEntityType(requestDto.getEntityType());
        slug.setClickCount(0L);
        Slug saved = slugRepository.save(slug);
        log.info("Created slug '{}' for entity type '{}'", saved.getSlug(), saved.getEntityType());
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public SlugResponseDto resolveSlug(String slug) {
        Slug found = slugRepository.findBySlug(slug).orElseThrow(() -> {
            log.warn("Slug '{}' not found", slug);
            return new ResourceNotFoundException("Slug '" + slug + "' not found");
        });
        // Atomic DB-side increment; clearAutomatically evicts `found` from the
        // persistence context so Hibernate cannot flush a stale clickCount on commit.
        slugRepository.incrementClickCount(slug);
        // Re-read the authoritative count written by the bulk UPDATE.
        long updatedCount = slugRepository.findClickCountBySlug(slug)
                .orElse(found.getClickCount() + 1);
        found.setClickCount(updatedCount);
        log.info("Resolved slug '{}', total clicks: {}", slug, updatedCount);
        return mapToResponseDto(found);
    }

    @Override
    @Transactional(readOnly = true)
    public SlugResponseDto getSlug(String slug) {
        Slug found = slugRepository.findBySlug(slug).orElseThrow(() -> {
            log.warn("Slug '{}' not found", slug);
            return new ResourceNotFoundException("Slug '" + slug + "' not found");
        });
        return mapToResponseDto(found);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SlugResponseDto> getAllSlugs(int page, int size) {
        return slugRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "clickCount")))
                .map(this::mapToResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SlugResponseDto> getSlugsByEntityType(EntityType entityType, int page, int size) {
        return slugRepository.findAllByEntityType(entityType,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "clickCount")))
                .map(this::mapToResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlugResponseDto> getTopSlugs(int limit) {
        return slugRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "clickCount")))
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSlug(String slug) {
        if (!slugRepository.existsBySlug(slug)) {
            log.warn("Attempted to delete non-existent slug '{}'", slug);
            throw new ResourceNotFoundException("Slug '" + slug + "' not found");
        }
        slugRepository.deleteBySlug(slug);
        log.info("Deleted slug '{}'", slug);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean slugExists(String slug) {
        return slugRepository.existsBySlug(slug);
    }
}

