package com.legalaid.backend.websocket;

import java.security.Principal;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPresenceListener {

    private final PresenceService presenceService;

    @EventListener
    public void handleConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor =
            StompHeaderAccessor.wrap(event.getMessage());

        Principal user = accessor.getUser();

        // Fallback: Try to get user from session attributes
        if (user == null && accessor.getSessionAttributes() != null) {
            user = (Principal) accessor.getSessionAttributes().get("user");
        }

        if (user == null) {
            log.warn("WS CONNECTED without authenticated user");
            return;
        }

        String email = user.getName();
        log.info("PRESENCE | CONNECT | user={}", email);

        presenceService.userConnected(email);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor =
            StompHeaderAccessor.wrap(event.getMessage());

        Principal user = accessor.getUser();
        
        // Fallback: Try to get user from session attributes
        if (user == null && accessor.getSessionAttributes() != null) {
            user = (Principal) accessor.getSessionAttributes().get("user");
        }

        if (user == null) {
            log.warn("PRESENCE | DISCONNECT | User not found in session attributes. SessionId={}", event.getSessionId());
            return;
        }

        String email = user.getName();
        presenceService.userDisconnected(email);
        log.info("PRESENCE | DISCONNECT | user={}", email);
    }
}
