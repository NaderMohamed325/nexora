package com.neo.nexora.service.comment;

import com.neo.nexora.dto.CommentRequestDto;
import com.neo.nexora.dto.CommentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface CommentService {

    CommentResponseDto addComment(UserDetails userDetails, Long postId, CommentRequestDto requestDto);

    Page<CommentResponseDto> getCommentsByPostId(Long postId, int page, int size);

    List<CommentResponseDto> getCommentsByUserId(Long userId);

    CommentResponseDto updateComment(UserDetails userDetails, Long commentId, CommentRequestDto requestDto);

    void deleteComment(UserDetails userDetails, Long commentId);
}

