package com.legalaid.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.legalaid.backend.websocket.JwtHandshakeInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Chat endpoint
        registry.addEndpoint("/ws-chat")
                .setAllowedOrigins("http://localhost:3000");

        // Notifications endpoint
        registry.addEndpoint("/ws-notifications")
                .setAllowedOrigins("http://localhost:3000");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple broker for subscriptions
        registry.enableSimpleBroker("/topic", "/queue");

        // Application destination prefix for client-to-server messages
        registry.setApplicationDestinationPrefixes("/app");

        // User destination prefix for private messages (user-specific queues)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtHandshakeInterceptor);
    }
}