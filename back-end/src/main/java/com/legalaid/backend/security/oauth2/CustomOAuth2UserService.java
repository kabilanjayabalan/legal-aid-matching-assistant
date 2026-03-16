package com.legalaid.backend.security.oauth2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.model.UserStatus;
import com.legalaid.backend.repository.UserRepository;

@Service
@Slf4j
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("OAuth2 loadUser invoked for client: {}", userRequest.getClientRegistration().getRegistrationId());
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();
        log.debug("OAuth2 attributes retrieved: {}", attributes.keySet());

        AtomicReference<String> emailRef = new AtomicReference<>((String) attributes.get("email"));
        if (emailRef.get() == null || emailRef.get().isBlank()) {
            String placeholderEmail = "unknown_" + UUID.randomUUID() + "@example.com";
            emailRef.set(placeholderEmail);
            log.warn("Email not provided by OAuth2 provider, generating placeholder email: {}", emailRef.get());
        }
        log.info("Attempting to load user with email: {}", emailRef.get());

        AtomicBoolean isNewUser = new AtomicBoolean(false);
        User user = userRepository.findByEmail(emailRef.get()).orElseGet(() -> {
            isNewUser.set(true);
            log.debug("User not found, creating new OAuth2 user");
            String name = (String) attributes.getOrDefault("name", emailRef.get());
            String baseUsername = emailRef.get().contains("@") ? emailRef.get().substring(0, emailRef.get().indexOf('@')) : emailRef.get();
            String username = generateUniqueUsername(baseUsername);
            Role selectedRole = resolveRequestedRole(Role.CITIZEN);
            Boolean approved = (selectedRole == Role.LAWYER || selectedRole == Role.NGO) ? null : Boolean.TRUE;
            UserStatus status = (selectedRole == Role.LAWYER || selectedRole == Role.NGO) ? UserStatus.PENDING : UserStatus.ACTIVE;
            log.debug("Generated username: {}, selected role: {}, approved: {}", username, selectedRole, approved);
            
            User newUser = new User(
                    username,
                    emailRef.get(),
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    name,
                    selectedRole,
                    approved,
                    status
            );

            User saved = userRepository.saveAndFlush(newUser);
            log.info("Created new OAuth2 user id={} email={} username={} role={} approved={}", saved.getId(), saved.getEmail(), saved.getUsername(), saved.getRole(), saved.getApproved());
            return saved;
        });

        if (!isNewUser.get()) {
            log.debug("Existing user found with email: {}", emailRef.get());
        }

        Map<String, Object> enrichedAttributes = new HashMap<>(attributes);
        enrichedAttributes.put("email", emailRef.get());
        enrichedAttributes.put("isNewUser", isNewUser.get());
        enrichedAttributes.put("appRole", user.getRole().name());
        log.debug("Enriched OAuth2 attributes with email, isNewUser flag, and app role");

        log.info("OAuth2 user loaded successfully: email={}, role={}, isNewUser={}", emailRef.get(), user.getRole().name(), isNewUser.get());
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(user.getRole().name())),
                enrichedAttributes,
                "email"
        );
    }

    private Role resolveRequestedRole(Role defaultRole) {
        log.debug("Attempting to resolve requested OAuth role from cookies, default role: {}", defaultRole);
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                log.debug("No request attributes found, using default role: {}", defaultRole);
                return defaultRole;
            }
            var request = attrs.getRequest();
            if (request.getCookies() == null) {
                log.debug("No cookies found in request, using default role: {}", defaultRole);
                return defaultRole;
            }
            for (var cookie : request.getCookies()) {
                if ("oauth_role".equals(cookie.getName())) {
                    String value = cookie.getValue();
                    log.debug("Found oauth_role cookie with value: {}", value);
                    if (value != null && !value.isBlank()) {
                        try {
                            Role parsed = Role.valueOf(value.toUpperCase());
                            log.info("OAuth role cookie found, using role={}", parsed);
                            return parsed;
                        } catch (Exception ignored) {
                            log.warn("Invalid oauth_role cookie value: {}", value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve requested OAuth role, using default {}. Error: {}", defaultRole, e.getMessage(), e);
        }
        log.debug("No valid oauth_role cookie found, using default role: {}", defaultRole);
        return defaultRole;
    }

    private String generateUniqueUsername(String baseUsername) {
        log.debug("Generating unique username from base: {}", baseUsername);
        String candidate = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            log.debug("Username {} already exists, trying next variant", candidate);
            candidate = baseUsername + suffix;
            suffix++;
        }
        log.debug("Generated unique username: {}", candidate);
        return candidate;
    }
}

