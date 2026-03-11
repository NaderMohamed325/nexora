package com.neo.nexora.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponseDto {
    private Long likeId;
    private Long userId;
    private String username;
    private Long postId;
    private Long commentId;
    private long totalLikes;
}

