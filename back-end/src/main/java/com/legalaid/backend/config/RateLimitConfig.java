package com.legalaid.backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@Getter
public class RateLimitConfig {

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.default-requests-per-minute:100}")
    private int defaultRequestsPerMinute;

    @Value("${app.rate-limit.auth-requests-per-minute:5}")
    private int authRequestsPerMinute;

    @Value("${app.rate-limit.admin-requests-per-minute:200}")
    private int adminRequestsPerMinute;

    @Value("${app.rate-limit.public-requests-per-minute:50}")
    private int publicRequestsPerMinute;

    @Value("${app.rate-limit.cache-expiration-minutes:60}")
    private int cacheExpirationMinutes;

    @Bean
    public Cache<String, Bucket> bucketCache() {
        log.info("Initializing rate limit bucket cache with expiration: {} minutes", cacheExpirationMinutes);
        return Caffeine.newBuilder()
                .expireAfterWrite(cacheExpirationMinutes, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    /**
     * Creates a bucket with the specified requests per minute limit
     */
    public Bucket createBucket(int requestsPerMinute) {
        log.debug("Creating rate limit bucket with {} requests per minute", requestsPerMinute);
        return Bucket.builder()
                .addLimit(io.github.bucket4j.Bandwidth.builder()
                        .capacity(requestsPerMinute)
                        .refillIntervally(requestsPerMinute, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    /**
     * Gets the appropriate rate limit for a given endpoint path
     */
    public int getRateLimitForPath(String path) {
        if (path == null) {
            return defaultRequestsPerMinute;
        }

        String lowerPath = path.toLowerCase();
        
        if (lowerPath.startsWith("/auth/") || lowerPath.startsWith("/login/")) {
            log.debug("Using auth rate limit for path: {}", path);
            return authRequestsPerMinute;
        } else if (lowerPath.startsWith("/admin/") || lowerPath.startsWith("/analytics/") || lowerPath.startsWith("/actuator/")) {
            log.debug("Using admin rate limit for path: {}", path);
            return adminRequestsPerMinute;
        } else if (lowerPath.startsWith("/api/")) {
            log.debug("Using public rate limit for path: {}", path);
            return publicRequestsPerMinute;
        } else {
            log.debug("Using default rate limit for path: {}", path);
            return defaultRequestsPerMinute;
        }
    }

    /**
     * Normalizes the path into a stable bucket key so clients cannot bypass limits by
     * varying dynamic path segments (e.g. /cases/123 vs /cases/124).
     *
     * This should stay aligned with {@link #getRateLimitForPath(String)}.
     */
    public String getBucketKeyForPath(String path) {
        if (path == null) {
            return "default";
        }

        String lowerPath = path.toLowerCase();

        if (lowerPath.startsWith("/auth/") || lowerPath.startsWith("/login/")) {
            return "auth";
        }
        if (lowerPath.startsWith("/admin/") || lowerPath.startsWith("/analytics/") || lowerPath.startsWith("/actuator/")) {
            return "admin";
        }
        if (lowerPath.startsWith("/api/")) {
            return "public";
        }
        return "default";
    }
}

