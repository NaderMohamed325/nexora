package com.neo.nexora.repository;

import com.neo.nexora.entity.BlacklistedToken;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
  boolean existsByToken(String token);

  void deleteByExpiryDateBefore(LocalDateTime dateTime);
}
