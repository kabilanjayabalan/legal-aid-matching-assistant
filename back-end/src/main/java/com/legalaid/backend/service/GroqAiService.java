package com.legalaid.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GroqAiService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final RestTemplate restTemplate;

    public GroqAiService() {
        this.restTemplate = new RestTemplate();
    }

    public String getChatResponse(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a legal aid assistant. You must ONLY reply to questions and topics related to legal aid, justice, law, general news related to law & justice, and rules and regulations of law. If a user asks about anything else, you must politely decline to answer and state that you can only assist with legal and justice-related matters.");

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("model", "llama-3.1-8b-instant"); // Using a popular Groq model
            requestBodyMap.put("messages", List.of(systemMessage, userMessage));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBodyMap, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_API_URL, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> msg = (Map<String, Object>) firstChoice.get("message");
                    if (msg != null && msg.containsKey("content")) {
                        return (String) msg.get("content");
                    }
                }
            }

            return "I'm sorry, I couldn't generate a response at this time.";
        } catch (Exception e) {
            log.error("Error calling Groq API", e);
            return "An error occurred while communicating with the AI service.";
        }
    }
}
