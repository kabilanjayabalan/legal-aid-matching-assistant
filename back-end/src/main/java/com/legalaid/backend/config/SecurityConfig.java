    package com.legalaid.backend.config;

    import java.io.IOException;
    import java.util.List;

    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.authentication.AuthenticationManager;
    import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
    import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
    import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
    import org.springframework.security.config.annotation.web.builders.HttpSecurity;
    import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
    import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
    import org.springframework.security.config.http.SessionCreationPolicy;
    import org.springframework.security.core.userdetails.UserDetailsService;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
    import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
    import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
    import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
    import org.springframework.security.oauth2.core.oidc.user.OidcUser;
    import org.springframework.security.oauth2.core.user.OAuth2User;
    import org.springframework.security.web.SecurityFilterChain;
    import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
    import org.springframework.lang.NonNull;
    import org.springframework.web.cors.CorsConfiguration;
    import org.springframework.web.filter.OncePerRequestFilter;

    import com.github.benmanes.caffeine.cache.Cache;
    import com.legalaid.backend.filter.RateLimitFilter;
    import com.legalaid.backend.security.JwtUtils;
    import com.legalaid.backend.security.oauth2.OAuth2AuthenticationSuccessHandler;

    import io.github.bucket4j.Bucket;
    import jakarta.servlet.FilterChain;
    import jakarta.servlet.ServletException;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;
    import lombok.extern.slf4j.Slf4j;

    @Slf4j
    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true)
    public class SecurityConfig {

        private final UserDetailsService userDetailsService;
        private final JwtUtils jwtUtils;
        private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
        private final OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService;
        private final PasswordEncoder passwordEncoder;
        private final RateLimitConfig rateLimitConfig;
        private final Cache<String, Bucket> bucketCache;

        public SecurityConfig(UserDetailsService userDetailsService,
                              JwtUtils jwtUtils,
                              OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                              OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService,
                              PasswordEncoder passwordEncoder,
                              RateLimitConfig rateLimitConfig,
                              Cache<String, Bucket> bucketCache) {
            this.userDetailsService = userDetailsService;
            this.jwtUtils = jwtUtils;
            this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
            this.customOAuth2UserService = customOAuth2UserService;
            this.passwordEncoder = passwordEncoder;
            this.rateLimitConfig = rateLimitConfig;
            this.bucketCache = bucketCache;
        }

        @Bean
        public RateLimitFilter rateLimitFilter() {
            log.info("Initializing rate limit filter");
            return new RateLimitFilter(rateLimitConfig, bucketCache);
        }

        @Bean
        public AuthTokenFilter authenticationJwtTokenFilter() {
            log.info("Initializing JWT authentication token filter");
            return new AuthTokenFilter(jwtUtils, userDetailsService);
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
            log.info("Configuring DAO authentication provider");
            DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
            authProvider.setUserDetailsService(userDetailsService);
            authProvider.setPasswordEncoder(passwordEncoder);
            log.debug("DAO authentication provider configured successfully");
            return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
            log.info("Initializing authentication manager");
            return authConfig.getAuthenticationManager();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            log.info("Configuring security filter chain");
            http.securityMatcher(
                "/auth/**",
                "/oauth2/**",
                "/login/**",
                "/admin/**",
                "/profile/**",
                "/users/**",
                "/cases/**",
                "/matches/**",
                "/appointments/**",
                            "/api/**",
                            "/presence/**",
                            "/chats/**" ,
                            "/ws-chat/**",
                            "/api/location/**",
                            "/notifications/**",
                            "/analytics/**",
                            "/actuator/**",
                            "/system/maintenance",
                            "/admin/system/**"
            )
                    .csrf(AbstractHttpConfigurer::disable)
                    .cors(cors -> cors.configurationSource(request -> {
                        log.debug("Configuring CORS settings");
                        CorsConfiguration config = new CorsConfiguration();
                        config.setAllowedOriginPatterns(List.of("http://localhost:3000"));
                        config.setAllowCredentials(true);
                        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                        config.setAllowedHeaders(List.of("*"));
                        return config;
                    }))
                    .sessionManagement(session -> {
                        log.debug("Configuring session management to STATELESS");
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                    })// VERY IMPORTANT FOR SOCKJS
                    .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                    )
                    .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("Unauthorized request to {}: {}", request.getRequestURI(), authException.getMessage());
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error: Unauthorized");
                        })
                    )
                    .authorizeHttpRequests(auth -> {
                        log.debug("Configuring authorization rules");
                        auth
                            .requestMatchers("/auth/**").permitAll()
                            .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                            .requestMatchers("/ws-chat", "/ws-chat/**").permitAll()
                            .requestMatchers("/presence/**").permitAll()
                            .requestMatchers("/admin/**").hasAuthority("ADMIN")
                            .requestMatchers("/analytics/**").hasAuthority("ADMIN")
                            .requestMatchers("/actuator/**").hasAuthority("ADMIN")
                            .requestMatchers("/system/maintenance").permitAll()
                            .requestMatchers("/admin/system/**").hasRole("ADMIN")
                                .requestMatchers("/appointments/**").permitAll()
                                .requestMatchers("/api/**").permitAll()
                                .requestMatchers("/profile/**").authenticated()
                            .requestMatchers("/users/**").authenticated()
                            .requestMatchers("/cases/**").authenticated()
                            .requestMatchers("/chats/**").authenticated()
                            .requestMatchers(
                                    "/matches/generate/**",
                                                "/matches/my-cases/**",
                                                "/matches/*/citizen-accept",
                                                "/matches/*/citizen-reject",
                                                "/matches/*/save",
                                                "/matches/*/unsave",
                                                "/matches/my/saved"
                                            ).hasAuthority("CITIZEN")

                            .requestMatchers(
                                    "/matches/my/**",
                                                "/matches/*/confirm"
                                            ).hasAnyAuthority("LAWYER", "NGO")

                                .requestMatchers("/api/location/**").permitAll()
                                .requestMatchers("/notifications/**").authenticated()
                            // Everything else secured
                                .anyRequest().authenticated();
                    })
                    .oauth2Login(oauth -> {
                        log.debug("Configuring OAuth2 login");
                        oauth
                            .userInfoEndpoint(userInfo -> userInfo
                                    .userService(customOAuth2UserService)
                                    .oidcUserService(customOidcUserService()))
                            .successHandler(oAuth2AuthenticationSuccessHandler);
                    });

            http.authenticationProvider(authenticationProvider());
            // Run JWT auth first so the rate limiter can key by authenticated user when available.
            // Unauthenticated requests will still be rate limited by IP.
            http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
            http.addFilterAfter(rateLimitFilter(), AuthTokenFilter.class);

            log.info("Security filter chain configured successfully");
            return http.build();
        }

        /**
         * Wrap the existing OAuth2 user service so OIDC logins (Google) also pass through
         * our custom provisioning logic before building an OidcUser.
         */
        @Bean
        public OAuth2UserService<OidcUserRequest, OidcUser> customOidcUserService() {
            log.info("Initializing custom OIDC user service");
            return (oidcRequest) -> {
                log.debug("Processing OIDC user request");
                OAuth2User oAuth2User = customOAuth2UserService.loadUser(
                        new OAuth2UserRequest(
                                oidcRequest.getClientRegistration(),
                                oidcRequest.getAccessToken(),
                                oidcRequest.getAdditionalParameters()
                        )
                );
                log.debug("OIDC user processed successfully");
                return new DefaultOidcUser(
                        oAuth2User.getAuthorities(),
                        oidcRequest.getIdToken(),
                        "email"
                );
            };
        }

        // Inner Class for Filter to keep files concise
        public static class AuthTokenFilter extends OncePerRequestFilter {
            private final JwtUtils jwtUtils;
            private final UserDetailsService userDetailsService;
            private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthTokenFilter.class);

            public AuthTokenFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
                this.jwtUtils = jwtUtils;
                this.userDetailsService = userDetailsService;
            }

            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
                    throws ServletException, IOException {
                try {
                    String jwt = parseJwt(request);
                    if (jwt != null) {
                        log.debug("JWT token found in request");
                        if (jwtUtils.validateToken(jwt)) {
                            String username = jwtUtils.extractUsername(jwt);
                            log.debug("JWT token validated for username: {}", username);
                            var userDetails = userDetailsService.loadUserByUsername(username);
                            var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.debug("User authentication set in security context for: {}", username);
                        } else {
                            log.warn("JWT token validation failed");
                        }
                    } else {
                        log.debug("No JWT token found in request");
                    }
                } catch (Exception e) {
                    log.error("Cannot set user authentication: {}", e.getMessage(), e);
                }
                filterChain.doFilter(request, response);
            }

            private String parseJwt(HttpServletRequest request) {
                String headerAuth = request.getHeader("Authorization");
                if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
                    log.debug("Authorization header found, extracting JWT token");
                    return headerAuth.substring(7);
                }
                return null;
            }
        }
    }
