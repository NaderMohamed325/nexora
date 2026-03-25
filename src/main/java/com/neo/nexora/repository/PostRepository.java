package com.neo.nexora.repository;

import com.neo.nexora.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByAuthorId(Long authorId);

    @Query("SELECT p FROM Post p WHERE " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Post> findPostWhereTitleOrContentContains(@Param("keyword") String keyword,
                                                   Pageable pageable);

    Optional<Post> findPostById(Long id);

    @Transactional
    void deletePostById(Long id);

    Page<Post> findPostByCreatedAtBetween(LocalDateTime createdAtAfter,
                                          LocalDateTime createdAtBefore,
                                          Pageable pageable);

    Page<Post> findPostByAuthor_IdAndCreatedAtBetween(Long authorId,
                                                      LocalDateTime createdAtAfter,
                                                      LocalDateTime createdAtBefore, Pageable pageable);
}