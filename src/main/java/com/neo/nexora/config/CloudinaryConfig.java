package com.neo.nexora.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configures the Cloudinary SDK bean using credentials supplied via environment variables.
 *
 * <p>Expected environment variables (loaded through application.yaml):
 *
 * <ul>
 *   <li>{@code CLOUDINARY_CLOUD_NAME}
 *   <li>{@code CLOUDINARY_API_KEY}
 *   <li>{@code CLOUDINARY_API_SECRET}
 * </ul>
 */
@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    /**
     * Creates and exposes a fully configured {@link Cloudinary} instance as a Spring bean.
     *
     * @return the Cloudinary client
     */
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(
                Map.of(
                        "cloud_name", cloudName,
                        "api_key", apiKey,
                        "api_secret", apiSecret,
                        "secure", true));
    }
}

