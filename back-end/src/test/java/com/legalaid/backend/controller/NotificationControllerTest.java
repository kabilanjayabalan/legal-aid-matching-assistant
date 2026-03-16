package com.legalaid.backend.controller;

import com.legalaid.backend.model.Notification;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.model.UserStatus;
import com.legalaid.backend.dto.system.MaintenanceStatusResponse;
import com.legalaid.backend.repository.NotificationRepository;
import com.legalaid.backend.repository.UserRepository;
import com.legalaid.backend.service.system.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private SystemSettingsService systemSettingsService;

    private User testUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setRole(Role.CITIZEN);
        testUser.setStatus(UserStatus.ACTIVE);

        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUserId(1L);
        testNotification.setType("MATCH_FOUND");
        testNotification.setMessage("New match found");
        testNotification.setIsRead(false);
        testNotification.setCreatedAt(LocalDateTime.now());

        // Mock maintenance status to be disabled
        when(systemSettingsService.getMaintenanceStatus())
                .thenReturn(new MaintenanceStatusResponse(false, null, null, null));
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"CITIZEN"})
    void testGetNotifications_Success() throws Exception {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
            .thenReturn(Arrays.asList(testNotification));

        // Act & Assert
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].type").value("MATCH_FOUND"));
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"CITIZEN"})
    void testGetUnreadCount_Success() throws Exception {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(5L);

        // Act & Assert
        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"CITIZEN"})
    void testMarkAsRead_Success() throws Exception {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Act & Assert
        mockMvc.perform(put("/notifications/1/read")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"CITIZEN"})
    void testGetRecentNotifications_Success() throws Exception {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(notificationRepository.findTop5ByUserIdOrderByCreatedAtDesc(1L))
            .thenReturn(Arrays.asList(testNotification));

        // Act & Assert
        mockMvc.perform(get("/notifications/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].type").value("MATCH_FOUND"));
    }

    @Test
    @WithMockUser(username = "test@example.com", authorities = {"CITIZEN"})
    void testGetNotifications_EmptyList() throws Exception {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
            .thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
