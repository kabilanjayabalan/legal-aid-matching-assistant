package com.legalaid.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private Long userId;
    private String type; // MATCH, MESSAGE, APPOINTMENT, etc.
    private String message;
    private String referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
