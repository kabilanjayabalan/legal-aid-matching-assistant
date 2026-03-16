package com.legalaid.backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RateLimitConfig.class)
@TestPropertySource(properties = {
    "app.rate-limit.enabled=true",
    "app.rate-limit.default-requests-per-minute=100",
    "app.rate-limit.auth-requests-per-minute=5",
    "app.rate-limit.admin-requests-per-minute=200",
    "app.rate-limit.public-requests-per-minute=50"
})
class RateLimitConfigTest {

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @Autowired
    private Cache<String, Bucket> bucketCache;

    @Test
    void testRateLimitConfig_LoadsProperties() {
        assertNotNull(rateLimitConfig);
        assertTrue(rateLimitConfig.isRateLimitEnabled());
        assertEquals(100, rateLimitConfig.getDefaultRequestsPerMinute());
        assertEquals(5, rateLimitConfig.getAuthRequestsPerMinute());
        assertEquals(200, rateLimitConfig.getAdminRequestsPerMinute());
        assertEquals(50, rateLimitConfig.getPublicRequestsPerMinute());
    }

    @Test
    void testBucketCache_IsCreated() {
        assertNotNull(bucketCache);
    }

    @Test
    void testBucketCache_CanStoreAndRetrieve() {
        // Create a test bucket
        Bucket testBucket = Bucket.builder()
                .addLimit(io.github.bucket4j.Bandwidth.builder()
                        .capacity(100)
                        .refillIntervally(100, java.time.Duration.ofMinutes(1))
                        .build())
                .build();

        // Store in cache
        bucketCache.put("test-key", testBucket);

        // Retrieve from cache
        Bucket retrieved = bucketCache.getIfPresent("test-key");
        assertNotNull(retrieved);
        assertEquals(testBucket, retrieved);
    }
}

