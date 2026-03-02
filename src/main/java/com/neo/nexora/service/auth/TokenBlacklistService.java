package com.neo.nexora.service.auth;

import com.neo.nexora.entity.BlacklistedToken;
import com.neo.nexora.repository.BlacklistedTokenRepository;
import com.neo.nexora.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public void blacklist(String token) {
        if (blacklistedTokenRepository.existsByToken(token)) {
            return;
        }

        BlacklistedToken blacklistedToken = new BlacklistedToken();
        blacklistedToken.setToken(token);
        blacklistedToken.setBlacklistedAt(LocalDateTime.now());
        blacklistedToken.setExpiryDate(
                jwtUtil
                        .extractExpiration(token)
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime());

        blacklistedTokenRepository.save(blacklistedToken);
    }

    public boolean isBlacklisted(String token) {
        return blacklistedTokenRepository.existsByToken(token);
    }

    /**
     * Scheduled cleanup: removes expired blacklisted tokens every hour. Once a JWT has expired,
     * there's no need to keep it in the blacklist.
     */
    @Transactional
    @Scheduled(fixedRate = 3600000) // every hour
    public void purgeExpiredTokens() {
        blacklistedTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}
