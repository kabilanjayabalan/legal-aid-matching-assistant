package com.legalaid.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationship identifiers
    @Column(nullable = false)
    private Long matchId;

    @Column(nullable = false)
    private Long requesterId;

    @Column(nullable = false)
    private Long receiverId;

    // UI attributes
    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private String timeZone;

    @Column(nullable = false)
    private String timeSlot;

    @Column(nullable = false)
    private Integer durationMinutes;

    // Reminder options
    private Boolean remind15Min;
    private Boolean remind1Hour;

    // System-managed fields
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Track who last modified the appointment (for determining who should respond)
    @Column(name = "last_modified_by")
    private Long lastModifiedBy;

    /* =====================
       GETTERS & SETTERS
       ===================== */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Boolean getRemind15Min() {
        return remind15Min;
    }

    public void setRemind15Min(Boolean remind15Min) {
        this.remind15Min = remind15Min;
    }

    public Boolean getRemind1Hour() {
        return remind1Hour;
    }

    public void setRemind1Hour(Boolean remind1Hour) {
        this.remind1Hour = remind1Hour;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(Long lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
}
