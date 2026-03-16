package com.legalaid.backend.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.legalaid.backend.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;
    private final Cache<String, Bucket> bucketCache;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitConfig rateLimitConfig, Cache<String, Bucket> bucketCache) {
        this.rateLimitConfig = rateLimitConfig;
        this.bucketCache = bucketCache;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Skip rate limiting if disabled
        if (!rateLimitConfig.isRateLimitEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get client identifier (IP address or authenticated username)
        String clientId = getClientIdentifier(request);
        String requestPath = request.getRequestURI();

        // Get rate limit for this endpoint
        int requestsPerMinute = rateLimitConfig.getRateLimitForPath(requestPath);
        // Use a normalized bucket key to avoid bypass via dynamic path segments (e.g. /cases/123 vs /cases/124)
        String bucketKey = rateLimitConfig.getBucketKeyForPath(requestPath);
        String cacheKey = clientId + ":" + bucketKey;

        // Get or create bucket for this client and bucket group
        Bucket bucket = bucketCache.get(cacheKey, key -> {
            log.debug("Creating new rate limit bucket for client: {} on bucket: {} (path: {})", clientId, bucketKey, requestPath);
            return rateLimitConfig.createBucket(requestsPerMinute);
        });

        // Try to consume a token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        // Add rate limit headers to response
        addRateLimitHeaders(response, requestsPerMinute, probe);

        if (probe.isConsumed()) {
            // Request is allowed
            log.debug("Rate limit check passed for client: {} on path: {}, remaining: {}", 
                    clientId, requestPath, probe.getRemainingTokens());
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            log.warn("Rate limit exceeded for client: {} on path: {}. Retry after {} seconds", 
                    clientId, requestPath, retryAfterSeconds);
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Too Many Requests");
            errorResponse.put("message", "Rate limit exceeded. Please try again later.");
            errorResponse.put("retryAfter", retryAfterSeconds);
            
            objectMapper.writeValue(response.getWriter(), errorResponse);
        }
    }

    /**
     * Extracts client identifier from request.
     * Uses authenticated username if available, otherwise uses IP address.
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get authenticated username first
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getName())) {
            log.debug("Using authenticated username as client identifier: {}", authentication.getName());
            return "user:" + authentication.getName();
        }

        // Fall back to IP address
        String ipAddress = extractIpAddress(request);
        log.debug("Using IP address as client identifier: {}", ipAddress);
        return "ip:" + ipAddress;
    }

    /**
     * Extracts IP address from request, handling proxy headers.
     */
    private String extractIpAddress(HttpServletRequest request) {
        // Check X-Forwarded-For header (for proxies/load balancers)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            String[] ips = xForwardedFor.split(",");
            String ip = ips[0].trim();
            log.debug("Extracted IP from X-Forwarded-For: {}", ip);
            return ip;
        }

        // Check X-Real-IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            log.debug("Extracted IP from X-Real-IP: {}", xRealIp);
            return xRealIp;
        }

        // Fall back to remote address
        String remoteAddr = request.getRemoteAddr();
        log.debug("Using remote address: {}", remoteAddr);
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    /**
     * Adds rate limit headers to the response.
     */
    private void addRateLimitHeaders(HttpServletResponse response, int limit, ConsumptionProbe probe) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
        
        // Calculate reset time (current time + time until refill)
        long resetTime = Instant.now().getEpochSecond() + (probe.getNanosToWaitForRefill() / 1_000_000_000);
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));
    }
}

