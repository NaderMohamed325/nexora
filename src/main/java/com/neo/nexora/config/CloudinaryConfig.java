package com.neo.nexora.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configures the Media Server integration for file uploads.
 *
 * <p>Expected environment variables (loaded through application.yaml):
 *
 * <ul>
 *   <li>{@code media-server.url} - Base URL of the Media Server API
 * </ul>
 */
@Configuration
public class CloudinaryConfig {

    /**
     * Creates and exposes a RestTemplate bean for HTTP calls to Media Server.
     *
     * @return the RestTemplate client
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

