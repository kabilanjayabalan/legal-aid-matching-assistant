package com.legalaid.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Order(0)
public class WebSocketSecurityConfig {

    @Bean
    public SecurityFilterChain webSocketChain(HttpSecurity http) throws Exception {

        http
            .securityMatcher("/ws-chat/**")
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)

            // 🔥 ALLOW SockJS iframe transport
            .headers(headers ->
                headers.frameOptions(frame -> frame.disable())
            )

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth ->
                auth.anyRequest().permitAll()
            )

            .oauth2Login(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
