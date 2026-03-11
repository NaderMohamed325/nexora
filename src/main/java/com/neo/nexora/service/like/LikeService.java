package com.neo.nexora.service.like;

import com.neo.nexora.dto.LikeResponseDto;
import org.springframework.security.core.userdetails.UserDetails;

public interface LikeService {

    /** Toggle like on a post — likes if not liked, unlikes if already liked. */
    LikeResponseDto togglePostLike(UserDetails userDetails, Long postId);

    /** Toggle like on a comment — likes if not liked, unlikes if already liked. */
    LikeResponseDto toggleCommentLike(UserDetails userDetails, Long commentId);

    /** Get total likes for a post. */
    long getPostLikeCount(Long postId);

    /** Get total likes for a comment. */
    long getCommentLikeCount(Long commentId);

    /** Check if the current user has liked a post. */
    boolean hasUserLikedPost(UserDetails userDetails, Long postId);

    /** Check if the current user has liked a comment. */
    boolean hasUserLikedComment(UserDetails userDetails, Long commentId);
}

