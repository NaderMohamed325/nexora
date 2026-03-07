package com.neo.nexora.repository;

import com.neo.nexora.entity.EntityType;
import com.neo.nexora.entity.Slug;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SlugRepository extends JpaRepository<Slug, Long> {

    Optional<Slug> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Slug> findAllByEntityType(EntityType entityType);

    Page<Slug> findAllByEntityType(EntityType entityType, Pageable pageable);

    @Transactional
    @Modifying
    @Query("UPDATE Slug s SET s.clickCount = s.clickCount + 1 WHERE s.slug = :slug")
    void incrementClickCount(@Param("slug") String slug);

    @Transactional
    void deleteBySlug(String slug);
}

