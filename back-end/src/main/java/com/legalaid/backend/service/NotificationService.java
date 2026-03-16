package com.legalaid.backend.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.legalaid.backend.dto.NotificationDTO;
import com.legalaid.backend.model.Notification;
import com.legalaid.backend.model.User;
import com.legalaid.backend.repository.NotificationRepository;
import com.legalaid.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    /**
     * 🔔 Create notification and broadcast via WebSocket
     */
    public Notification notifyUser(
            Long userId,
            String type,
            String message,
            String referenceId) {

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        // Save to database
        Notification savedNotification = notificationRepository.save(notification);

        // 🔥 Broadcast via WebSocket
        broadcastNotification(savedNotification);

        return savedNotification;
    }

    /**
     * 📡 Broadcast notification to user via WebSocket
     */
    public void broadcastNotification(Notification notification) {
        try {
            NotificationDTO dto = convertToDTO(notification);

            // Send to user-specific topic: /topic/notifications/{userId}
            messagingTemplate.convertAndSend(
                    "/topic/notifications/" + notification.getUserId(),
                    dto
            );

            log.info("Notification broadcasted | userId={} | type={}", 
                    notification.getUserId(), notification.getType());

            // Also broadcast updated unread count
            broadcastUnreadCount(notification.getUserId());

        } catch (Exception e) {
            log.error("Failed to broadcast notification: {}", e.getMessage());
        }
    }

    /**
     * 📊 Broadcast updated unread count
     */
    public void broadcastUnreadCount(Long userId) {
        try {
            long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
            String username = userRepository
                .findById(userId.intValue())
                .map(User::getEmail)   // or getUsername()
                .orElseThrow(() -> new RuntimeException("User not found"));

            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/unread-count",
                    new UnreadCountDTO(unreadCount)
            );

            log.info("Unread count broadcasted | userId={} | count={}", userId, unreadCount);

        } catch (Exception e) {
            log.error("Failed to broadcast unread count: {}", e.getMessage());
        }
    }

    /**
     * ✔ Mark notification as read and broadcast updated count
     */
    public void markAsRead(Long notificationId) {
        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);

        if (optionalNotification.isEmpty()) {
            log.warn("Notification not found: {}", notificationId);
            return;
        }

        Notification notification = optionalNotification.get();
        notification.setIsRead(true);
        notificationRepository.save(notification);

        // Broadcast updated unread count
        broadcastUnreadCount(notification.getUserId());

        log.info("Notification marked as read | id={} | userId={}", 
                notificationId, notification.getUserId());
    }

    /**
     * Get unread count for user
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Convert Notification entity to DTO for WebSocket
     */
    private NotificationDTO convertToDTO(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }

    /**
     * DTO for unread count
     */
    public static class UnreadCountDTO {
        private long count;

        public UnreadCountDTO(long count) {
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