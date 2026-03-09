package com.neo.nexora.service.user;

import com.neo.nexora.dto.UserResponseDto;
import com.neo.nexora.dto.UserUpdateDto;
import com.neo.nexora.entity.User;
import com.neo.nexora.entity.UserAccountStatus;
import com.neo.nexora.exception.ResourceNotFoundException;
import com.neo.nexora.repository.UserRepository;
import com.neo.nexora.service.user.lookUp.UserLookUpService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private static final int GRACE_PERIOD_DAYS = 30;
    private final UserRepository userRepository;
    private final UserLookUpService userLookUpService;


    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id the unique identifier of the user
     * @return the user details as {@link UserResponseDto}
     * @throws ResourceNotFoundException if no user is found with the given id
     */
    @Override
    public UserResponseDto getUserById(Long id) {
        User user = extractUserById(id);
        log.info("User found with id: {}", id);
        return UserResponseDto.fromEntity(user);
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
        return UserResponseDto.fromEntity(user);
    }

    /**
     * Retrieves all registered users.
     *
     * @return a list of {@link UserResponseDto} representing all users
     */
    @Override
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
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
                .map(UserResponseDto::fromEntity)
                .toList();
    }

    /**
     * Updates the details of the authenticated user extracted from the JWT token.
     *
     * @param userDetails   User Data
     * @param userUpdateDto the DTO containing the updated user details
     * @return the updated user details as {@link UserResponseDto}
     * @throws ResourceNotFoundException if no user is found matching the token's subject
     */
    @Override
    @Transactional
    public UserResponseDto updateUser(UserDetails userDetails, UserUpdateDto userUpdateDto) {
        String username = userDetails.getUsername();
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
            userLookUpService.addUser(userUpdateDto.getUsername());
        }
        if (userUpdateDto.getEmail() != null && !userUpdateDto.getEmail().isBlank()) {
            user.setEmail(userUpdateDto.getEmail());
        }


        log.info("User updated successfully: {}", userUpdateDto.getUsername());

        return UserResponseDto.builder().id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .enabled(user.isEnabled())
                .lastLoginAt(user.getLastLoginAt())
                .deactivatedAt(user.getDeactivatedAt())
                .scheduledDeletionAt(user.getScheduledDeletionAt())
                .avatarUrl(user.getAvatarUrl())
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

    @Override
    @Transactional
    public void deactivateAccount(UserDetails userDetails) {
        String username = userDetails.getUsername();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> {
                            log.warn("User not found with username: {}", username);
                            return new ResourceNotFoundException("User not found with username: " + username);
                        });
        user.setDeactivatedAt(LocalDateTime.now());
        user.setScheduledDeletionAt(LocalDateTime.now().plusDays(GRACE_PERIOD_DAYS));
        user.setStatus(UserAccountStatus.PENDING_DELETION);
        userRepository.save(user);
    }


    @Override
    @Transactional
    public void reactivateAccount(UserDetails userDetails) {
        User user = userRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(
                        () -> {
                            log.warn("User not found with username: {}", userDetails.getUsername());
                            return new ResourceNotFoundException("User not found with username: " + userDetails.getUsername());
                        });
        user.setStatus(UserAccountStatus.ACTIVE);
        user.setDeactivatedAt(null);
        user.setScheduledDeletionAt(null);
        userRepository.save(user);
    }

    @Override
    public User extractUserById(Long id) {
        return userRepository
                .findUserById(id)
                .orElseThrow(
                        () -> {
                            log.warn("User not found with id: {}", id);
                            return new ResourceNotFoundException("User not found with id: " + id);
                        });
    }
}
