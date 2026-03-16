package com.legalaid.backend.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUtils {

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private long jwtExpirationMs; // Access token expiry

    @Value("${app.jwtRefreshExpirationMs}")
    private long refreshExpirationMs; // Refresh token expiry

    private Key key() {
        // If the secret is not Base64 encoded or too short, this might fail or produce a weak key.
        // Ensure app.jwtSecret is a strong, Base64-encoded string (at least 32 bytes / 256 bits).
        try {
             return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        } catch (Exception e) {
            // Fallback or re-throw with a clearer message if decoding fails
             throw new IllegalStateException("Invalid JWT Secret configuration", e);
        }
    }

    // ========================= ACCESS TOKEN =========================

    public String generateAccessToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ========================= REFRESH TOKEN =========================

    public String generateRefreshToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ========================= VALIDATION =========================

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT validation failed: Token expired. Message: {}", e.getMessage());
            return false;
        } catch (WeakKeyException e) {
            log.error("JWT validation failed: Weak Key Exception. The key used to sign the token is not secure enough. Message: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation failed: Invalid JWT. Message: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // Used by refresh-token endpoint
    public String generateAccessTokenFromUsername(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }
}
