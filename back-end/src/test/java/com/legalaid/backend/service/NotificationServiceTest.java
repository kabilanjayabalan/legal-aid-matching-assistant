package com.legalaid.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import com.legalaid.backend.model.Notification;
import com.legalaid.backend.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void testNotifyUser_Success() {
        // Arrange
        Long userId = 123L;
        String type = "MATCH_FOUND";
        String message = "New match found for your case";
        String referenceId = "CASE456";

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        // Act
        notificationService.notifyUser(userId, type, message, referenceId);

        // Assert
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertEquals(userId, savedNotification.getUserId());
        assertEquals(type, savedNotification.getType());
        assertEquals(message, savedNotification.getMessage());
        assertEquals(referenceId, savedNotification.getReferenceId());
        assertFalse(savedNotification.getIsRead());
        assertNotNull(savedNotification.getCreatedAt());
    }

    @Test
    void testNotifyUser_WithNullReferenceId() {
        // Arrange
        Long userId = 123L;
        String type = "SYSTEM_ALERT";
        String message = "System maintenance scheduled";
        String referenceId = null;

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        // Act
        notificationService.notifyUser(userId, type, message, referenceId);

        // Assert
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());

        Notification savedNotification = notificationCaptor.getValue();
        assertNull(savedNotification.getReferenceId());
        assertEquals(userId, savedNotification.getUserId());
    }

    @Test
    void testNotifyUser_VerifyDefaultIsReadFalse() {
        // Arrange
        Long userId = 999L;
        String type = "TEST_TYPE";
        String message = "Test message";
        String referenceId = "REF001";

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        // Act
        notificationService.notifyUser(userId, type, message, referenceId);

        // Assert
        verify(notificationRepository).save(notificationCaptor.capture());
        assertFalse(notificationCaptor.getValue().getIsRead(), "Notification should be unread by default");
    }
}

