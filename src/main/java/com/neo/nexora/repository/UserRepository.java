package com.neo.nexora.repository;

import com.neo.nexora.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query(value = "SELECT u FROM users where u.username ILIKE %:username%", nativeQuery = true)
    List<User> findByUsernameLike(@Param("username") String username);

    Optional<User> findUserById(Long id);


    @Query("SELECT u FROM User u WHERE u.status = 'PENDING_DELETION' AND u.scheduledDeletionAt <= :now")
    List<User> findUsersScheduledForDeletion(@Param("now") LocalDateTime now);
}
