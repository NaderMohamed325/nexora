package com.neo.nexora.service.user.lookUp;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.neo.nexora.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class UserLookUpServiceImpl implements UserLookUpService {

    private BloomFilter<String> bloomFilter;
    private final UserRepository userRepository;
    final int MAX_USER_LIMIT = 500_000;

    @Scheduled(cron = "0 0 0 * * 0")
    @PostConstruct
    @Transactional(readOnly = true)
    public void init() {
        long userCount = Math.max(userRepository.count(), MAX_USER_LIMIT);
        bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                userCount,
                0.01
        );
        try (Stream<String> usernames = userRepository.streamAllUsernames()) {
            usernames.forEach(bloomFilter::put);
        }
    }

    @Override
    public void addUser(String username) {
        bloomFilter.put(username);
    }

    @Override
    public boolean mightContainUser(String username) {
        return bloomFilter.mightContain(username);
    }

    @Override
    public boolean shouldCheckDatabase(String username) {
        return bloomFilter.mightContain(username);
    }
}
