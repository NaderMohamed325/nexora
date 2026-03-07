package com.neo.nexora.service.post;

import com.neo.nexora.dto.CloudinaryUploadResponse;
import com.neo.nexora.dto.PostMediaUpdateDto;
import com.neo.nexora.dto.PostRequestDto;
import com.neo.nexora.dto.PostResponseDto;
import com.neo.nexora.entity.Post;
import com.neo.nexora.entity.User;
import com.neo.nexora.exception.ResourceNotFoundException;
import com.neo.nexora.repository.PostRepository;
import com.neo.nexora.repository.UserRepository;
import com.neo.nexora.service.cloudinary.CloudinaryUploadService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CloudinaryUploadService cloudinaryUploadService;


    private @NonNull PostResponseDto mapToResponseDto(@NonNull Post post) {
        PostResponseDto dto = new PostResponseDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setMedia_urls(post.getMedia_urls());
        return dto;
    }

    @Override
    @Transactional
    public PostResponseDto createPost(@NonNull UserDetails userDetails, @NonNull PostRequestDto requestDto, @NonNull List<MultipartFile> fileList) {
        String username = userDetails.getUsername();
        User author = userRepository.findByUsername(username).orElseThrow(() -> {
            log.warn("User with username {} is not found", username);
            return new ResourceNotFoundException("Usr with " + username + " is not found");
        });
        Post post = new Post();
        post.setAuthor(author);
        post.setContent(requestDto.getContent());
        post.setTitle(requestDto.getTitle());

        List<String> urls = new ArrayList<>();
        if (!fileList.isEmpty()) {
            for (MultipartFile file : fileList) {
                CloudinaryUploadResponse cloudinaryUploadResponse = cloudinaryUploadService.upload(file, "Posts");
                urls.add(cloudinaryUploadResponse.getSecureUrl());
            }
        }
        post.setMedia_urls(urls);

        Post saved = postRepository.save(post);

        return mapToResponseDto(saved);
    }

    @Override
    public List<PostResponseDto> getPostsByUserId(Long Id) {
        userRepository.findById(Id).orElseThrow(() -> {
            log.warn("User with id {} is not found", Id);
            return new ResourceNotFoundException("User with id " + Id + " is not found");
        });
        List<Post> posts = postRepository.findAllByAuthorId(Id);
        List<PostResponseDto> responseDtos = new ArrayList<>();
        for (Post post : posts) {
            responseDtos.add(mapToResponseDto(post));
        }
        return responseDtos;
    }

    @Override
    public Page<PostResponseDto> getAllPosts(int page, int size) {
        return postRepository.findAll(PageRequest.of(page, size)).map(this::mapToResponseDto);
    }

    @Override
    public Page<PostResponseDto> searchPosts(String keyword, int page, int size) {
        Page<Post> posts = postRepository.findPostWhereTitleOrContentContains(keyword, PageRequest.of(page, size));
        return posts.map(this::mapToResponseDto);
    }

    @Override
    public PostResponseDto getPostById(Long id) {
        Post postOptional = postRepository.findPostById(id).orElseThrow(() -> {
            log.warn("Post with id {} is not found", id);
            return new ResourceNotFoundException("Post with id " + id + " is not found");
        });
        return mapToResponseDto(postOptional);
    }

    @Override
    public void deletePostById(@NonNull UserDetails userDetails, Long id) {
        Post post = postRepository.findPostById(id).orElseThrow(() -> {
            log.warn("Post with id {} is not found", id);
            return new ResourceNotFoundException("Post with id " + id + " is not found");
        });

        String username = userDetails.getUsername();
        if (!post.getAuthor().getUsername().equals(username)) {
            log.warn("User {} attempted to delete post {} without ownership", username, id);
            throw new AccessDeniedException("You do not have permission to delete this post");
        }

        for (String url : post.getMedia_urls()) {
            String publicId = cloudinaryUploadService.extractPublicId(url);
            cloudinaryUploadService.delete(publicId);
        }

        postRepository.deletePostById(id);

    }

    @Override
    @Transactional
    public PostResponseDto updatePost(UserDetails userDetails, Long id, PostRequestDto requestDto,
                                      PostMediaUpdateDto mediaUpdateDto,
                                      List<MultipartFile> fileList) {

        Post post = postRepository.findPostById(id).orElseThrow(() -> {
            log.warn("Post with id {} not found", id);
            return new ResourceNotFoundException("Post with id " + id + " is not found");
        });

        // 1. Ownership check
        String username = userDetails.getUsername();
        if (!post.getAuthor().getUsername().equals(username)) {
            log.warn("User {} attempted to update post {} without ownership", username, id);
            throw new AccessDeniedException("You do not have permission to update this post");
        }

        // 2. Update basic fields
        post.setTitle(requestDto.getTitle());
        post.setContent(requestDto.getContent());

        // 3. Remove media
        List<String> mediaToRemove = mediaUpdateDto.getMediaToRemove();
        if (mediaToRemove != null && !mediaToRemove.isEmpty()) {
            for (String url : mediaToRemove) {
                post.getMedia_urls().remove(url);
                String publicId = cloudinaryUploadService.extractPublicId(url);
                cloudinaryUploadService.delete(publicId);
            }
        }

        if (fileList != null && !fileList.isEmpty()) {
            List<String> newUrls = new ArrayList<>();
            for (MultipartFile file : fileList) {
                CloudinaryUploadResponse response = cloudinaryUploadService.upload(file, "Posts");
                newUrls.add(response.getSecureUrl());
            }
            post.getMedia_urls().addAll(newUrls);
        }

        return mapToResponseDto(post);
    }


    @Override
    public Page<PostResponseDto> getPostsByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        return postRepository.findPostByCreatedAtBetween(startDate, endDate, PageRequest.of(page, size)).map(this::mapToResponseDto);

    }

    @Override
    public Page<PostResponseDto> getPostsByUserIdAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        userRepository.findById(userId).orElseThrow(() -> {
            log.warn("User with id {} is not found", userId);
            return new ResourceNotFoundException("User with id " + userId + " is not found");
        });

        return postRepository.findPostByAuthor_IdAndCreatedAtBetween(userId, startDate, endDate, PageRequest.of(page, size)).map(this::mapToResponseDto);
    }
}
