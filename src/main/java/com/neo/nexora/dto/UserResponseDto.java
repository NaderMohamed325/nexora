package com.neo.nexora.dto;

import com.neo.nexora.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
  private Long id;

  private String username;

  private String email;

  private Role role;
}
