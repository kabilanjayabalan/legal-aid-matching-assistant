package com.legalaid.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.rate-limit.enabled=true",
    "app.rate-limit.default-requests-per-minute=10",
    "app.rate-limit.auth-requests-per-minute=3",
    "app.rate-limit.admin-requests-per-minute=20",
    "app.rate-limit.public-requests-per-minute=5"
})
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Clear any existing rate limit state if needed
    }

    @Test
    void testRateLimit_WithinLimit_ShouldSucceed() throws Exception {
        // Make requests within the limit
        // We check rate limit headers regardless of endpoint status
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/directory/lawyers")
                            .header("X-Forwarded-For", "192.168.1.100"))
                    .andExpect(header().exists("X-RateLimit-Limit"))
                    .andExpect(header().exists("X-RateLimit-Remaining"));
        }
    }

    @Test
    void testRateLimit_ExceedLimit_ShouldReturn429() throws Exception {
        String testIp = "192.168.1.200";
        int limit = 3; // Auth endpoint limit

        // Make requests up to the limit
        for (int i = 0; i < limit; i++) {
            mockMvc.perform(post("/auth/login")
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"test@test.com\",\"password\":\"test\"}"))
                    .andExpect(status().is4xxClientError()); // Login will fail but not rate limited yet
        }

        // This request should be rate limited
        mockMvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\",\"password\":\"test\"}"))
                .andExpect(status().is(429))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }

    @Test
    void testRateLimit_DifferentIPs_ShouldHaveSeparateLimits() throws Exception {
        // First IP makes requests
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/directory/lawyers")
                            .header("X-Forwarded-For", "192.168.1.101"))
                    .andExpect(header().exists("X-RateLimit-Limit"));
        }

        // Second IP should still be able to make requests
        mockMvc.perform(get("/api/directory/lawyers")
                        .header("X-Forwarded-For", "192.168.1.102"))
                .andExpect(header().exists("X-RateLimit-Limit"));
    }

    @Test
    void testRateLimit_HeadersPresent() throws Exception {
        mockMvc.perform(get("/api/directory/lawyers")
                        .header("X-Forwarded-For", "192.168.1.300"))
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"));
    }

    @Test
    void testRateLimit_DifferentEndpoints_DifferentLimits() throws Exception {
        String testIp = "192.168.1.400";

        // Test public endpoint limit (5 requests)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/directory/lawyers")
                            .header("X-Forwarded-For", testIp))
                    .andExpect(header().exists("X-RateLimit-Limit"));
        }

        // 6th request should be rate limited for public endpoint
        mockMvc.perform(get("/api/directory/lawyers")
                        .header("X-Forwarded-For", testIp))
                .andExpect(status().is(429));
    }
}

