package com.legalaid.backend.websocket;

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.legalaid.backend.security.JwtUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(message);
                
    log.info("JWT INTERCEPTOR HIT | command={}", accessor.getCommand());

        // ================= CONNECT =================
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("STOMP CONNECT received in interceptor");

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {

                String token = authHeader.substring(7);

                if (jwtUtils.validateToken(token)) {
                    String email = jwtUtils.extractUsername(token);

                    Principal principal = () -> email;

                    // 🔥 SET USER
                    accessor.setUser(principal);
                    log.info("User authenticated: {}", email);
                    // 🔥 SAVE IN SESSION
                    accessor.getSessionAttributes().put("user", principal);
                }
            }
        }
        
        // ================= DISCONNECT =================
        // Ensure user is available in accessor for disconnect event
        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
             Principal sessionUser =
                    (Principal) accessor.getSessionAttributes().get("user");
             if (sessionUser != null) {
                 accessor.setUser(sessionUser);
             }
        }

        // ================= SEND =================
        if (StompCommand.SEND.equals(accessor.getCommand())
                && accessor.getUser() == null) {

            Principal sessionUser =
                    (Principal) accessor.getSessionAttributes().get("user");

            if (sessionUser != null) {
                accessor.setUser(sessionUser);
            }
        }

        return MessageBuilder.createMessage(
                message.getPayload(),
                accessor.getMessageHeaders()
        );
    }
}
