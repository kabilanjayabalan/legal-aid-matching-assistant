package com.legalaid.backend.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Utility class for mocking Spring Security context in tests
 */
public class SecurityTestUtils {

    /**
     * Mock authenticated user in SecurityContext
     */
    public static void mockAuthenticatedUser(String email, String role) {
        UserDetails userDetails = User.builder()
                .username(email)
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(role)))
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * Clear SecurityContext
     */
    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Create UserDetails for testing
     */
    public static UserDetails createUserDetails(String username, String role) {
        return User.builder()
                .username(username)
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(role)))
                .build();
    }
}

