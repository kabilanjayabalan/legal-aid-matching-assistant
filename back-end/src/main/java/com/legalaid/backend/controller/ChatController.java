package com.legalaid.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.legalaid.backend.dto.ChatSummaryDto;
import com.legalaid.backend.model.ChatFileAttachment;
import com.legalaid.backend.model.ChatMessage;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.repository.UserRepository;
import com.legalaid.backend.security.JwtUtils;
import com.legalaid.backend.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/my")
    public List<ChatSummaryDto> getMyChats(
            @RequestHeader("Authorization") String auth
    ) {
        String token = auth.substring(7);
        String username = jwtUtils.extractUsername(token);

        log.info("CHAT_AUDIT | action=LIST_CHATS | user={}", username);

        // Get user role to determine which method to call
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Role.CITIZEN) {
            return chatService.getCitizenChats(username);
        } else if (user.getRole() == Role.LAWYER || user.getRole() == Role.NGO) {
            return chatService.getProviderChats(username);
        } else {
            throw new RuntimeException("Chat not supported for role: " + user.getRole());
        }
    }
    
    @GetMapping("/me")
    public Integer getMyUserId(@RequestHeader("Authorization") String auth) {
        String token = auth.substring(7);
        String username = jwtUtils.extractUsername(token);
        log.info("CHAT_AUDIT | action=GET_MY_ID | user={}", username);
        return userRepository.findByEmail(username)
            .orElseThrow()
            .getId();
    }


    @GetMapping("/{matchId}")
    public List<ChatMessage> getChats(
            @PathVariable Integer matchId,
            @RequestHeader("Authorization") String auth
    ) {
        String token = auth.substring(7);
        String username = jwtUtils.extractUsername(token);
        log.info("CHAT_AUDIT | action=LOAD_CHATS | matchId={} | user={}", matchId, username);

        return chatService.loadChats(matchId, username);
    }

    /**
     * REST API endpoint for sending chat messages (for testing with Postman)
     * POST /chats/{matchId}
     * Headers: Authorization: Bearer <token>
     * Body: { "message": "Your message here" }
     * 
     * This endpoint:
     * 1. Saves the message to database
     * 2. Creates a notification for the receiver (similar to appointment notifications)
     * 3. Broadcasts the message via WebSocket to /topic/chat/{matchId}
     */
    @PostMapping("/{matchId}")
    public ChatMessage sendMessage(
            @PathVariable Integer matchId,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String auth
    ) {
        String token = auth.substring(7);
        String username = jwtUtils.extractUsername(token);
        String message = request.get("message");
        
        if (message == null || message.trim().isEmpty()) {
            throw new RuntimeException("Message cannot be empty");
        }
        
        log.info("REST SEND | matchId={} | user={} | message={}", matchId, username, message);

        // Save message (this will also create the notification)
        ChatMessage saved = chatService.saveMessage(matchId, username, message);
        
        // Broadcast to WebSocket subscribers
        messagingTemplate.convertAndSend("/topic/chat/" + matchId, saved);
        log.info("REST SEND | Broadcasted to /topic/chat/{}", matchId);
        
        return saved;
    }

    /**
     * File upload endpoint for chat messages
     * POST /chats/{matchId}/upload
     * Headers: Authorization: Bearer <token>
     * Body: multipart/form-data with file and optional message
     */
    @PostMapping(value = "/{matchId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatMessage uploadFile(
            @PathVariable Integer matchId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "message", required = false, defaultValue = "") String message,
            @RequestHeader("Authorization") String auth
    ) {
        String token = auth.substring(7);
        String username = jwtUtils.extractUsername(token);
        
        // Validate file size (2GB = 2 * 1024 * 1024 * 1024 bytes)
        long maxSize = 2L * 1024 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new RuntimeException("File size exceeds maximum limit of 2GB");
        }
        
        log.info("FILE UPLOAD | matchId={} | user={} | fileName={} | fileSize={}", 
                matchId, username, file.getOriginalFilename(), file.getSize());

        // Save message with file attachment
        ChatMessage saved = chatService.saveMessageWithFile(matchId, username, message, file);
        
        // Broadcast to WebSocket subscribers
        messagingTemplate.convertAndSend("/topic/chat/" + matchId, saved);
        log.info("FILE UPLOAD | Broadcasted to /topic/chat/{}", matchId);
        
        return saved;
    }

    /**
     * File download endpoint
     * GET /chats/files/{fileId}
     * Headers: Authorization: Bearer <token>
     */
    @GetMapping("/files/{fileId}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Long fileId,
            @RequestHeader("Authorization") String auth
    ) {
        String token = auth.substring(7);
        String username = jwtUtils.extractUsername(token);
        
        log.info("FILE DOWNLOAD | fileId={} | user={}", fileId, username);

        ChatFileAttachment fileAttachment = chatService.getFileAttachment(fileId, username);
        
        HttpHeaders headers = new HttpHeaders();
        String contentType = fileAttachment.getFileType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", fileAttachment.getFileName());
        headers.setContentLength(fileAttachment.getFileSize());
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(fileAttachment.getData());
    }
}
