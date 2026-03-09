package com.neo.nexora.repository;

import com.neo.nexora.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>Provides standard CRUD operations (via {@link JpaRepository}) plus custom query methods
 * for authentication, search, and the <b>RabbitMQ-driven user deletion pipeline</b>.</p>
 *
 * <h3>Queue-related methods</h3>
 * <ul>
 *   <li>{@link #findUsersScheduledForDeletion(LocalDateTime, Pageable)} — used by the
 *       {@link com.neo.nexora.service.queue.producer.UserDeletionProducerImpl producer}
 *       to page through users eligible for deletion.</li>
 *   <li>{@link #deleteBatch(List)} — used by the
 *       {@link com.neo.nexora.service.queue.consumer.UserDeletionConsumerImpl consumer}
 *       to bulk-delete users in sub-batches of 500.</li>
 * </ul>
 *
 * @see com.neo.nexora.entity.User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by exact username match (used during login/authentication).
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by exact email match (used during registration and password reset).
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a username is already taken (used during registration validation).
     */
    boolean existsByUsername(String username);

    /**
     * Checks if an email is already registered (used during registration validation).
     */
    boolean existsByEmail(String email);

    /**
     * Case-insensitive LIKE search on the {@code username} column.
     * <p>Uses PostgreSQL's {@code ILIKE} for case-insensitive matching.</p>
     *
     * @param username the partial username to search for
     * @return list of users whose username contains the given string (case-insensitive)
     */
    @Query(value = "SELECT * FROM users where username ILIKE %:username%", nativeQuery = true)
    List<User> findByUsernameLike(@Param("username") String username);

    /**
     * Finds a single user by primary key (returns {@link Optional#empty()} if not found).
     */
    Optional<User> findUserById(Long id);

    /**
     * Finds users whose status is {@code PENDING_DELETION} and whose
     * {@code scheduledDeletionAt} timestamp is in the past.
     *
     * <p><b>Used by:</b>
     * {@link com.neo.nexora.service.queue.producer.UserDeletionProducerImpl}</p>
     *
     * <p>Returns a {@link Page} so the producer can iterate page-by-page
     * (page size = {@value com.neo.nexora.config.RabbitMQConfig#BATCH_SIZE})
     * without loading all eligible users into memory at once.</p>
     *
     * @param now      the current timestamp — users with {@code scheduledDeletionAt <= now} are eligible
     * @param pageable pagination parameters (page number and page size)
     * @return a page of users ready for permanent deletion
     */
    @Query(
            "SELECT u FROM User u WHERE u.status = 'PENDING_DELETION' AND u.scheduledDeletionAt <= :now")
    Page<User> findUsersScheduledForDeletion(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Bulk-deletes users by a list of IDs in a single SQL {@code DELETE} statement.
     *
     * <p><b>Used by:</b>
     * {@link com.neo.nexora.service.queue.consumer.UserDeletionConsumerImpl}</p>
     *
     * <p>Instead of issuing {@code N} individual {@code DELETE} statements (one per user),
     * this executes a single:</p>
     * <pre>DELETE FROM users WHERE id IN (1, 2, 3, ..., 500)</pre>
     *
     * <p>The consumer calls this method with sub-batches of ~500 IDs to keep each
     * transaction short (~50 ms) and minimize table-level lock contention.</p>
     *
     * @param ids the list of user IDs to delete (typically ≤ 500 per call)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.id IN :ids")
    void deleteBatch(@Param("ids") List<Long> ids);

    @Query(value = "SELECT username FROM users LIMIT :numberOfUsernames", nativeQuery = true)
    List<String> findAllUsernames(@Param("numberOfUsernames") int numberOfUsernames);
}
