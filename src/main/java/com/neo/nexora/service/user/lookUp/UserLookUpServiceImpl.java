package com.neo.nexora.service.user.lookUp;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.neo.nexora.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@AllArgsConstructor
public class UserLookUpServiceImpl implements UserLookUpService {

    private BloomFilter<String> bloomFilter;
    private final UserRepository userRepository;
    final int MAX_USER_LIMIT = 1_000_000;

    // avg name is 1 byte, so 500k users = 500k bytes = 500 KB, which is reasonable for in-memory storage
    @Scheduled(cron = "0 0 0 * * 0")
    @PostConstruct
    public void init() {
        bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                MAX_USER_LIMIT,
                0.01
        );
        List<String> existingUsernames = userRepository.findAllUsernames(MAX_USER_LIMIT);
        existingUsernames.forEach(bloomFilter::put);

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
