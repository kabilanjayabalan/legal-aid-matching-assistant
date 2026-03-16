package com.legalaid.backend.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.legalaid.backend.model.ChatMessage;
import com.legalaid.backend.service.ChatService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat.send/{matchId}")
    @SendTo("/topic/chat/{matchId}")
    public ChatMessage sendMessage(
            @DestinationVariable Integer matchId,
            @Payload ChatPayload payload,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Principal principal = headerAccessor.getUser();

        if (principal == null) {
            throw new IllegalStateException("Unauthenticated WebSocket message");
        }

        String email = principal.getName();

        log.info("WS SEND | matchId={} | user={}", matchId, email);

        return chatService.saveMessage(
            matchId,
            email,
            payload.getMessage()
        );
    }
}

@Getter
@Setter
class ChatPayload {
    private String message;
}
