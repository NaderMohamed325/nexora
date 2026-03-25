package com.neo.nexora.service.like;

import com.neo.nexora.dto.LikeResponseDto;
import com.neo.nexora.entity.Comment;
import com.neo.nexora.entity.Like;
import com.neo.nexora.entity.Post;
import com.neo.nexora.entity.User;
import com.neo.nexora.exception.ResourceNotFoundException;
import com.neo.nexora.repository.CommentRepository;
import com.neo.nexora.repository.LikeRepository;
import com.neo.nexora.repository.PostRepository;
import com.neo.nexora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    // ── helpers ──────────────────────────────────────────────────────────────

    private User resolveUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + userDetails.getUsername()));
    }

    private LikeResponseDto buildPostResponse(Like like, long total) {
        LikeResponseDto dto = new LikeResponseDto();
        dto.setLikeId(like.getId());
        dto.setUserId(like.getUser().getId());
        dto.setUsername(like.getUser().getUsername());
        dto.setPostId(like.getPost().getId());
        dto.setTotalLikes(total);
        return dto;
    }

    private LikeResponseDto buildCommentResponse(Like like, long total) {
        LikeResponseDto dto = new LikeResponseDto();
        dto.setLikeId(like.getId());
        dto.setUserId(like.getUser().getId());
        dto.setUsername(like.getUser().getUsername());
        dto.setCommentId(like.getComment().getId());
        dto.setTotalLikes(total);
        return dto;
    }

    // ── post likes ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = "postLikeCounts", key = "#postId")
    public LikeResponseDto togglePostLike(UserDetails userDetails, Long postId) {
        User user = resolveUser(userDetails);
        Post post = postRepository.findPostById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (likeRepository.existsByUserIdAndPostId(user.getId(), postId)) {
            likeRepository.deleteByUserIdAndPostId(user.getId(), postId);
            log.info("User '{}' unliked post id={}", user.getUsername(), postId);
            long total = likeRepository.countByPostId(postId);
            // Return a lightweight DTO without a likeId (already deleted)
            LikeResponseDto dto = new LikeResponseDto();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setPostId(postId);
            dto.setTotalLikes(total);
            return dto;
        }

        Like like = new Like();
        like.setUser(user);
        like.setPost(post);
        Like saved = likeRepository.save(like);
        log.info("User '{}' liked post id={}", user.getUsername(), postId);
        return buildPostResponse(saved, likeRepository.countByPostId(postId));
    }

    // ── comment likes ────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = "commentLikeCounts", key = "#commentId")
    public LikeResponseDto toggleCommentLike(UserDetails userDetails, Long commentId) {
        User user = resolveUser(userDetails);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        if (likeRepository.existsByUserIdAndCommentId(user.getId(), commentId)) {
            likeRepository.deleteByUserIdAndCommentId(user.getId(), commentId);
            log.info("User '{}' unliked comment id={}", user.getUsername(), commentId);
            long total = likeRepository.countByCommentId(commentId);
            LikeResponseDto dto = new LikeResponseDto();
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setCommentId(commentId);
            dto.setTotalLikes(total);
            return dto;
        }

        Like like = new Like();
        like.setUser(user);
        like.setComment(comment);
        Like saved = likeRepository.save(like);
        log.info("User '{}' liked comment id={}", user.getUsername(), commentId);
        return buildCommentResponse(saved, likeRepository.countByCommentId(commentId));
    }

    // ── counts ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "postLikeCounts", key = "#postId")
    public long getPostLikeCount(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found with id: " + postId);
        }
        return likeRepository.countByPostId(postId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "commentLikeCounts", key = "#commentId")
    public long getCommentLikeCount(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new ResourceNotFoundException("Comment not found with id: " + commentId);
        }
        return likeRepository.countByCommentId(commentId);
    }

    // ── status checks ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserLikedPost(UserDetails userDetails, Long postId) {
        User user = resolveUser(userDetails);
        return likeRepository.existsByUserIdAndPostId(user.getId(), postId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserLikedComment(UserDetails userDetails, Long commentId) {
        User user = resolveUser(userDetails);
        return likeRepository.existsByUserIdAndCommentId(user.getId(), commentId);
    }
}

