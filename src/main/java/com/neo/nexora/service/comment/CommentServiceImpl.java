package com.neo.nexora.service.comment;

import com.neo.nexora.dto.CommentRequestDto;
import com.neo.nexora.dto.CommentResponseDto;
import com.neo.nexora.entity.Comment;
import com.neo.nexora.entity.Post;
import com.neo.nexora.entity.User;
import com.neo.nexora.exception.ResourceNotFoundException;
import com.neo.nexora.repository.CommentRepository;
import com.neo.nexora.repository.PostRepository;
import com.neo.nexora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    private CommentResponseDto mapToResponseDto(Comment comment) {
        CommentResponseDto dto = new CommentResponseDto();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setPostId(comment.getPost().getId());
        dto.setAuthorId(comment.getAuthor().getId());
        dto.setAuthorUsername(comment.getAuthor().getUsername());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        return dto;
    }

    @Override
    @Transactional
    public CommentResponseDto addComment(UserDetails userDetails, Long postId, CommentRequestDto requestDto) {
        User author = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userDetails.getUsername()));
        Post post = postRepository.findPostById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        Comment comment = new Comment();
        comment.setContent(requestDto.getContent());
        comment.setAuthor(author);
        comment.setPost(post);

        Comment saved = commentRepository.save(comment);
        log.info("User '{}' added comment to post id={}", author.getUsername(), postId);
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getCommentsByPostId(Long postId, int page, int size) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found with id: " + postId);
        }
        return commentRepository.findByPostId(postId, PageRequest.of(page, size))
                .map(this::mapToResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return commentRepository.findByAuthorId(userId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentResponseDto updateComment(UserDetails userDetails, Long commentId, CommentRequestDto requestDto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!comment.getAuthor().getUsername().equals(userDetails.getUsername()) && !isAdmin) {
            throw new AccessDeniedException("You are not allowed to edit this comment");
        }

        comment.setContent(requestDto.getContent());
        Comment updated = commentRepository.save(comment);
        log.info("Comment id={} updated by '{}'", commentId, userDetails.getUsername());
        return mapToResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteComment(UserDetails userDetails, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!comment.getAuthor().getUsername().equals(userDetails.getUsername()) && !isAdmin) {
            throw new AccessDeniedException("You are not allowed to delete this comment");
        }

        commentRepository.delete(comment);
        log.info("Comment id={} deleted by '{}'", commentId, userDetails.getUsername());
    }
}



