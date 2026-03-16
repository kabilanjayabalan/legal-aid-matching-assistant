package com.legalaid.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type; // MATCH, MESSAGE, APPOINTMENT

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "reference_id")
    private String referenceId; // matchId / appointmentId

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /* =====================
       GETTERS
       ===================== */

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /* =====================
       SETTERS
       ===================== */

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
