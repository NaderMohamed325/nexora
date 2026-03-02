package com.neo.nexora.service.user;

import com.neo.nexora.dto.UserResponseDto;
import com.neo.nexora.dto.UserUpdateDto;
import com.neo.nexora.entity.User;
import com.neo.nexora.exception.ResourceNotFoundException;
import com.neo.nexora.repository.UserRepository;
import com.neo.nexora.security.JwtUtil;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;

  /**
   * Retrieves a user by their unique identifier.
   *
   * @param id the unique identifier of the user
   * @return the user details as {@link UserResponseDto}
   * @throws ResourceNotFoundException if no user is found with the given id
   */
  @Override
  public UserResponseDto getUserById(Long id) {
    User user =
        userRepository
            .findUserById(id)
            .orElseThrow(
                () -> {
                  log.warn("User not found with id: {}", id);
                  return new ResourceNotFoundException("User not found with id: " + id);
                });

    log.info("User found with id: {}", id);
    return UserResponseDto.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .role(user.getRole())
        .build();
  }

  /**
   * Retrieves a user by their email address.
   *
   * @param email the email address of the user
   * @return the user details as {@link UserResponseDto}
   * @throws ResourceNotFoundException if no user is found with the given email
   */
  @Override
  public UserResponseDto getUserByEmail(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () -> {
                  log.warn("User not found with email: {}", email);
                  return new ResourceNotFoundException("User not found with email: " + email);
                });

    log.info("User found with email: {}", email);
    return UserResponseDto.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .role(user.getRole())
        .build();
  }

  /**
   * Retrieves all registered users.
   *
   * @return a list of {@link UserResponseDto} representing all users
   */
  @Override
  public List<UserResponseDto> getAllUsers() {
    return userRepository.findAll().stream()
        .map(
            user ->
                UserResponseDto.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .build())
        .toList();
  }

  /**
   * Searches for users whose username matches the given pattern.
   *
   * @param likeUserName the partial or full username to search for
   * @return a list of {@link UserResponseDto} matching the search criteria
   */
  @Override
  public List<UserResponseDto> getUsersLike(String likeUserName) {
    return userRepository.findByUsernameLike(likeUserName).stream()
        .map(
            user ->
                UserResponseDto.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .build())
        .toList();
  }

  /**
   * Updates the details of the authenticated user extracted from the JWT token.
   *
   * @param token the JWT token used to identify the authenticated user
   * @param userUpdateDto the DTO containing the updated user details
   * @return the updated user details as {@link UserResponseDto}
   * @throws ResourceNotFoundException if no user is found matching the token's subject
   */
  @Override
  @Transactional
  public UserResponseDto updateUser(String token, UserUpdateDto userUpdateDto) {
    String username = jwtUtil.extractUsername(token);

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () -> {
                  log.warn("User not found with username: {}", username);
                  return new ResourceNotFoundException("User not found with username: " + username);
                });

    if (userUpdateDto.getUsername() != null && !userUpdateDto.getUsername().isBlank()) {
      user.setUsername(userUpdateDto.getUsername());
    }
    if (userUpdateDto.getEmail() != null && !userUpdateDto.getEmail().isBlank()) {
      user.setEmail(userUpdateDto.getEmail());
    }

    User savedUser = userRepository.save(user);
    log.info("User updated successfully: {}", savedUser.getUsername());

    return UserResponseDto.builder()
        .id(savedUser.getId())
        .username(savedUser.getUsername())
        .email(savedUser.getEmail())
        .role(savedUser.getRole())
        .build();
  }

  /**
   * Deletes a user by their unique identifier.
   *
   * @param id the unique identifier of the user to delete
   * @throws ResourceNotFoundException if no user is found with the given id
   */
  @Override
  public void deleteUserById(Long id) {

    if (!userRepository.existsById(id)) {
      log.warn("User not found with id: {}", id);
      throw new ResourceNotFoundException("User not found with id: " + id);
    }

    userRepository.deleteById(id);
    log.info("User deleted successfully with id: {}", id);
  }
}
