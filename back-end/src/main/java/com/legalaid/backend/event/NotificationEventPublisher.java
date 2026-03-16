package com.legalaid.backend.event;

import org.springframework.stereotype.Component;

import com.legalaid.backend.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final NotificationService notificationService;

    /**
     * Publish match notification
     */
    public void publishMatchNotification(Long userId, String message, String matchId) {
        notificationService.notifyUser(
                userId,
                "MATCH",
                message,
                matchId
        );
    }

    /**
     * Publish appointment notification
     */
    public void publishAppointmentNotification(Long userId, String message, String appointmentId) {
        notificationService.notifyUser(
                userId,
                "APPOINTMENT",
                message,
                appointmentId
        );
    }

    /**
     * Publish message notification
     */
    public void publishMessageNotification(Long userId, String message, String matchId) {
        notificationService.notifyUser(
                userId,
                "MESSAGE",
                message,
                matchId
        );
    }

    /**
     * Publish case update notification
     */
    public void publishCaseNotification(Long userId, String message, String caseId) {
        notificationService.notifyUser(
                userId,
                "CASE",
                message,
                caseId
        );
    }

    /**
     * Publish general notification
     */
    public void publishNotification(Long userId, String type, String message, String referenceId) {
        notificationService.notifyUser(userId, type, message, referenceId);
    }
}
