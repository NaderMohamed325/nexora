package com.neo.nexora.repository;

import com.neo.nexora.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findByPostId(Long postId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByAuthorId(Long authorId);

    @Transactional
    void deleteByIdAndAuthorId(Long id, Long authorId);
}

