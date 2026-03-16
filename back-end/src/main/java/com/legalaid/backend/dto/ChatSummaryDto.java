package com.legalaid.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatSummaryDto {
    private Integer matchId;
    private String name;          // Lawyer / NGO name
    private String role;          // LAWYER / NGO
    private String email;
    private String caseTitle;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
}
