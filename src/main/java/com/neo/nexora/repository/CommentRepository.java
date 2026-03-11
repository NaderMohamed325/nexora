package com.neo.nexora.repository;

import com.neo.nexora.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostId(Long postId, Pageable pageable);

    List<Comment> findByAuthorId(Long authorId);

    @Transactional
    void deleteByIdAndAuthorId(Long id, Long authorId);
}

