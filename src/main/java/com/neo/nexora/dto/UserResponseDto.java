package com.neo.nexora.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.neo.nexora.entity.Role;
import com.neo.nexora.entity.User;
import com.neo.nexora.entity.UserAccountStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDto {
  private Long id;

  private String username;

  private String email;

  private Role role;

  private UserAccountStatus status;

  private boolean enabled;

  private LocalDateTime lastLoginAt;

  private LocalDateTime deactivatedAt;

  private LocalDateTime scheduledDeletionAt;

  /**
   * Converts a {@link User} entity to a {@link UserResponseDto}.
   *
   * @param user the user entity
   * @return the DTO representation
   */
  public static UserResponseDto fromEntity(User user) {
    return UserResponseDto.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .role(user.getRole())
        .status(user.getStatus())
        .enabled(user.isEnabled())
        .lastLoginAt(user.getLastLoginAt())
        .deactivatedAt(user.getDeactivatedAt())
        .scheduledDeletionAt(user.getScheduledDeletionAt())
        .build();
  }
}
