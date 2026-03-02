package com.neo.nexora.service.user;

import com.neo.nexora.dto.UserResponseDto;
import com.neo.nexora.dto.UserUpdateDto;
import com.neo.nexora.entity.User;

import java.util.List;

/**
 * Service interface for managing user-related operations.
 */
public interface UserService {

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id the unique identifier of the user
     * @return the user details as {@link UserResponseDto}
     * @throws com.neo.nexora.exception.ResourceNotFoundException if no user is found with the given
     *                                                            id
     */
    UserResponseDto getUserById(Long id);

    /**
     * Retrieves a user by their email address.
     *
     * @param email the email address of the user
     * @return the user details as {@link UserResponseDto}
     * @throws com.neo.nexora.exception.ResourceNotFoundException if no user is found with the given
     *                                                            email
     */
    UserResponseDto getUserByEmail(String email);

    /**
     * Retrieves all registered users.
     *
     * @return a list of {@link UserResponseDto} representing all users
     */
    List<UserResponseDto> getAllUsers();

    /**
     * Searches for users whose username matches the given pattern.
     *
     * @param likeUserName the partial or full username to search for
     * @return a list of {@link UserResponseDto} matching the search criteria
     */
    List<UserResponseDto> getUsersLike(String likeUserName);

    /**
     * Updates the details of the authenticated user extracted from the JWT token.
     *
     * @param token         the JWT token used to identify the authenticated user
     * @param userUpdateDto the DTO containing the updated user details
     * @return the updated user details as {@link UserResponseDto}
     * @throws com.neo.nexora.exception.ResourceNotFoundException if no user is found matching the
     *                                                            token's subject
     */
    UserResponseDto updateUser(String token, UserUpdateDto userUpdateDto);

    /**
     * Deletes a user by their unique identifier.
     *
     * @param id the unique identifier of the user to delete
     * @throws com.neo.nexora.exception.ResourceNotFoundException if no user is found with the given
     *                                                            id
     */
    void deleteUserById(Long id);

    /**
     * Deactivates the account of the user with the specified unique identifier. This typically
     * prevents the user from logging in or performing authenticated actions without permanently
     * removing the user from the system.
     *
     * @param id the unique identifier of the user whose account should be deactivated
     * @throws com.neo.nexora.exception.ResourceNotFoundException if no user is found with the given
     *                                                            id
     */
    void deactivateAccount(Long id);

    /**
     * Schedules the account of the user with the specified unique identifier for deletion. The actual
     * deletion may be performed at a later time according to business or system policies.
     *
     * @param id the unique identifier of the user whose account should be scheduled for deletion
     * @throws com.neo.nexora.exception.ResourceNotFoundException if no user is found with the given
     *                                                            id
     */
    void scheduleAccountForDeletion(Long id);

    /**
     * Reactivates the account of the user with the specified unique identifier. This reverses a
     * previous deactivation or scheduled deletion, allowing the user to access their account again
     * according to system rules.
     *
     * @param id the unique identifier of the user whose account should be reactivated
     * @throws com.neo.nexora.exception.ResourceNotFoundException if no user is found with the given
     *                                                            id
     */
    void reactivateAccount(Long id);

    /**
     * Retrieves the {@link User} entity by its unique identifier. Unlike {@link #getUserById(Long)},
     * this method returns the underlying entity instead of a DTO representation.
     *
     * @param id the unique identifier of the user
     * @return the {@link User} entity associated with the given identifier
     * @throws com.neo.nexora.exception.ResourceNotFoundException if no user is found with the given
     *                                                            id
     */
    User extractUserById(Long id);
}
