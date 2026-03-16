package com.legalaid.backend.security.oauth2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalaid.backend.security.JwtUtils;
import com.legalaid.backend.service.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtils jwtUtils;
    private final AuthService authService;

    @Value("${app.oauth2.redirectUri:http://localhost:3000/oauth2/callback}")
    private String redirectUri;

    public OAuth2AuthenticationSuccessHandler(JwtUtils jwtUtils, AuthService authService) {
        this.jwtUtils = jwtUtils;
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        log.info("OAuth2 authentication success handler invoked");
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        log.debug("OAuth2 principal attributes: {}", principal.getAttributes().keySet());
        
        String email = principal.getAttribute("email");
        log.debug("Extracted email from OAuth2 principal: {}", email);
        
        if (email == null || email.isBlank()) {
            log.error("OAuth2 provider did not return an email for principal");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by OAuth2 provider");
            return;
        }

        log.debug("Loading user details for email: {}", email);
        UserDetails userDetails = authService.loadUserByUsername(email);
        
        String accessToken = jwtUtils.generateAccessToken(userDetails);
        String refreshToken = jwtUtils.generateRefreshToken(userDetails);
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        boolean isNewUser = Boolean.TRUE.equals(principal.getAttribute("isNewUser"));
        
        log.debug("Generated tokens. Role: {}, IsNewUser: {}", role, isNewUser);

        if (redirectUri != null && !redirectUri.isBlank()) {
            log.info("Redirect URI configured: {}", redirectUri);
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .queryParam("email", email)
                    .queryParam("role", role)
                    .queryParam("isNewUser", isNewUser)
                    .build().toUriString();
            log.info("OAuth2 authentication succeeded for user {} with role {}. Redirecting to: {}", email, role, redirectUri);
            response.sendRedirect(targetUrl);
            return;
        }

        log.debug("No redirect URI configured, returning tokens in response body");
        Map<String, String> body = new HashMap<>();
        body.put("accessToken", accessToken);
        body.put("refreshToken", refreshToken);
        body.put("email", email);
        body.put("role", role);
        body.put("isNewUser", Boolean.toString(isNewUser));

        response.setContentType("application/json");
        log.info("OAuth2 authentication succeeded for user {} with role {}. Returning tokens in response body.", email, role);
        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
    }
}

