
package com.legalaid.backend.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import com.legalaid.backend.model.User;
import com.legalaid.backend.repository.UserRepository;
import com.legalaid.backend.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationWebSocketController {

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    /**
     * Subscribe to personal notifications
     * Client subscribes to: /topic/notifications/{userId}
     */
    @SubscribeMapping("/notifications/{userId}")
    public void subscribeToNotifications(
            @DestinationVariable String userId,
            Principal principal
    ) {
        if (principal == null) {
            log.warn("Unauthenticated subscription attempt for userId={}", userId);
            return;
        }

        String email = principal.getName();
        log.info("User subscribed to notifications | userId={} | email={}", userId, email);
    }

    /**
     * Mark notification as read via WebSocket
     * Message mapping: /app/notifications/mark-read
     */
    @MessageMapping("/notifications/mark-read")
    public void markNotificationAsRead(
            @Payload MarkReadRequest request,
            Principal principal
    ) {
        if (principal == null) {
            log.warn("Unauthorized mark-read attempt");
            return;
        }

        String email = principal.getName();
        log.info("Mark read | notificationId={} | user={}", request.getNotificationId(), email);

        notificationService.markAsRead(request.getNotificationId());

        // Broadcast updated unread count to user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long unreadCount = notificationService.getUnreadCount(user.getId().longValue());

        messagingTemplate.convertAndSendToUser(
                user.getId().toString(),
                "/queue/unread-count",
                new UnreadCountResponse(unreadCount)
        );
    }

    /**
     * Acknowledge notification receipt
     */
    @MessageMapping("/notifications/ack")
    public void acknowledgeNotification(
            @Payload AckRequest request,
            Principal principal
    ) {
        if (principal == null) {
            log.warn("Unauthorized ack attempt");
            return;
        }

        String email = principal.getName();
        log.info("Notification acknowledged | notificationId={} | user={}", request.getNotificationId(), email);
        // You can use this for analytics/tracking if needed
    }

    // ==================== DTOs ====================

    public static class MarkReadRequest {
        private Long notificationId;

        public Long getNotificationId() {
            return notificationId;
        }

        public void setNotificationId(Long notificationId) {
            this.notificationId = notificationId;
        }
    }

    public static class AckRequest {
        private Long notificationId;

        public Long getNotificationId() {
            return notificationId;
        }

        public void setNotificationId(Long notificationId) {
            this.notificationId = notificationId;
        }
    }

    public static class UnreadCountResponse {
        private long count;

        public UnreadCountResponse(long count) {
            this.count = count;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }
}
