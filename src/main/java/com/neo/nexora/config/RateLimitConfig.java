package com.neo.nexora.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bandwidth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    /**
     * Configures a Bucket4j bucket with a capacity of 100 tokens that refills at a rate of 100 tokens per minute.
     * Each token represents the capacity to handle one request.
     */
    @Bean
    public Bucket bucket() {
        // Define the bandwidth with a limit of 100 tokens, refilled every minute
        Bandwidth limit = Bandwidth.builder()
                .capacity(100)
                .refillGreedy(100, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}

