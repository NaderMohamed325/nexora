package com.neo.nexora.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class BloomFilterConfig {
    @Bean
    public BloomFilter<String> userBloomFilter() {
        return BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                1_000_000,    // Expected number of insertions
                0.01        // False positive probability (1%)
        );
    }
}
