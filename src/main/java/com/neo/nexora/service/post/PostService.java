package com.neo.nexora.service.post;

import com.neo.nexora.dto.PostMediaUpdateDto;
import com.neo.nexora.dto.PostRequestDto;
import com.neo.nexora.dto.PostResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public interface PostService {


    PostResponseDto createPost(UserDetails userDetails, PostRequestDto requestDto, List<MultipartFile> fileList);

    List<PostResponseDto> getPostsByUserId(Long Id);

    Page<PostResponseDto> getAllPosts(int page, int size);

    Page<PostResponseDto> searchPosts(String keyword, int page, int size);

    PostResponseDto getPostById(Long id);

    void deletePostById(UserDetails userDetails, Long id);

    PostResponseDto updatePost(UserDetails userDetails, Long id, PostRequestDto requestDto, PostMediaUpdateDto mediaUpdateDto, List<MultipartFile> fileList);


    Page<PostResponseDto> getPostsByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size);

    Page<PostResponseDto> getPostsByUserIdAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate, int page, int size);

}
