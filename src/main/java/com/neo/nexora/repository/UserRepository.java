package com.neo.nexora.repository;

import com.neo.nexora.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User>findUsers();

    @Query(value = "SELECT * FROM users where username ILIKE %:username%", nativeQuery = true)
    List<User> findByUsernameLike(@Param("username") String username);

    Optional<User> findUserById(Long id);




}
