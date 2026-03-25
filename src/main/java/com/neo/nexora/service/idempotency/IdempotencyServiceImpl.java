package com.neo.nexora.service.idempotency;


import com.sun.jdi.request.DuplicateRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServiceImpl implements IdempotencyService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration TTL = Duration.ofMinutes(5);

    public void validate(String key, String userId) {
        String redisKey = "idempotency:" + userId + ":" + key;

        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "pending", TTL);

        if (Boolean.FALSE.equals(isNew)) {
            throw new DuplicateRequestException("Duplicate request detected");
        }
    }
}
