package com.legalaid.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.legalaid.backend.service.GroqAiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AIChatController {

    private final GroqAiService groqAiService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("answer", "Question cannot be empty"));
        }
        
        log.info("Received AI chat request: {}", question);
        String answer = groqAiService.getChatResponse(question);
        
        return ResponseEntity.ok(Map.of("answer", answer));
    }
}
