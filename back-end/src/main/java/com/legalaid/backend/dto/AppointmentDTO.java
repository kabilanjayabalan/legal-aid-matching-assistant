package com.legalaid.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.legalaid.backend.model.AppointmentStatus;

public class AppointmentDTO {

    private Long id;
    private Long matchId;
    private AppointmentStatus status;
    private LocalDate appointmentDate;
    private String timeSlot;
    private String timeZone;
    private Integer durationMinutes;
    private String caseTitle;
    private String caseNumber;
    private Long requesterId;
    private Long receiverId;
    private String requesterName;
    private String receiverName;
    private Long lastModifiedBy;
    private LocalDateTime createdAt;

    public AppointmentDTO(
            Long id,
            Long matchId,
            AppointmentStatus status,
            LocalDate appointmentDate,
            String timeSlot,
            String timeZone,
            Integer durationMinutes,
            String caseTitle,
            String caseNumber,
            Long requesterId,
            Long receiverId,
            String requesterName,
            String receiverName,
            Long lastModifiedBy,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.matchId = matchId;
        this.status = status;
        this.appointmentDate = appointmentDate;
        this.timeSlot = timeSlot;
        this.timeZone = timeZone;
        this.durationMinutes = durationMinutes;
        this.caseTitle = caseTitle;
        this.caseNumber = caseNumber;
        this.requesterId = requesterId;
        this.receiverId = receiverId;
        this.requesterName = requesterName;
        this.receiverName = receiverName;
        this.lastModifiedBy = lastModifiedBy;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getMatchId() { return matchId; }
    public AppointmentStatus getStatus() { return status; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public String getTimeSlot() { return timeSlot; }
    public String getTimeZone() { return timeZone; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public String getCaseTitle() { return caseTitle; }
    public String getCaseNumber() { return caseNumber; }
    public Long getRequesterId() { return requesterId; }
    public Long getReceiverId() { return receiverId; }
    public String getRequesterName() { return requesterName; }
    public String getReceiverName() { return receiverName; }
    public Long getLastModifiedBy() { return lastModifiedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
