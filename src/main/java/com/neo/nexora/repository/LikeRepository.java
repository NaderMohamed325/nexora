package com.neo.nexora.repository;

import com.neo.nexora.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndPostId(Long userId, Long postId);

    Optional<Like> findByUserIdAndCommentId(Long userId, Long commentId);

    long countByPostId(Long postId);

    long countByCommentId(Long commentId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    @Transactional
    void deleteByUserIdAndPostId(Long userId, Long postId);

    @Transactional
    void deleteByUserIdAndCommentId(Long userId, Long commentId);
}

