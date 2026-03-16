package com.legalaid.backend.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.legalaid.backend.dto.AdminManagementResponse;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.model.UserStatus;
import com.legalaid.backend.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("user@example.com");
        testUser.setFullName("Test User");
        testUser.setRole(Role.CITIZEN);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testChangeStatus_Success() {
        // Arrange
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        adminUserService.changeStatus(1, UserStatus.SUSPENDED);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserStatus.SUSPENDED, userCaptor.getValue().getStatus());
        assertNotNull(userCaptor.getValue().getStatusChangedAt());
    }

    @Test
    void testChangeStatus_UserNotFound() {
        // Arrange
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
            () -> adminUserService.changeStatus(999, UserStatus.SUSPENDED));
    }

    @Test
    void testChangeStatus_CannotModifyAdmin() {
        // Arrange
        testUser.setRole(Role.ADMIN);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalStateException.class,
            () -> adminUserService.changeStatus(1, UserStatus.SUSPENDED));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangeStatus_SameStatusNoOp() {
        // Arrange
        testUser.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        // Act
        adminUserService.changeStatus(1, UserStatus.ACTIVE);

        // Assert
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangeStatus_ActivateSuspendedUser() {
        // Arrange
        testUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        adminUserService.changeStatus(1, UserStatus.ACTIVE);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserStatus.ACTIVE, userCaptor.getValue().getStatus());
    }

    @Test
    void testGetUsers_Success() {
        // Arrange
        User user1 = new User();
        user1.setId(1);
        user1.setEmail("user1@example.com");
        user1.setFullName("User One");
        user1.setRole(Role.CITIZEN);
        user1.setStatus(UserStatus.ACTIVE);
        user1.setCreatedAt(LocalDateTime.now());

        User user2 = new User();
        user2.setId(2);
        user2.setEmail("user2@example.com");
        user2.setFullName("User Two");
        user2.setRole(Role.LAWYER);
        user2.setStatus(UserStatus.PENDING);
        user2.setCreatedAt(LocalDateTime.now());

        Page<User> userPage = new PageImpl<>(Arrays.asList(user1, user2));

        when(userRepository.findUsers(anyString(), any(), any(), any(Pageable.class)))
            .thenReturn(userPage);

        // Act
        Page<AdminManagementResponse> result = adminUserService.getUsers(0, 10, "", null, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals("user1@example.com", result.getContent().get(0).getEmail());
        assertEquals("User One", result.getContent().get(0).getFullName());
        assertEquals("CITIZEN", result.getContent().get(0).getRole());
    }

    @Test
    void testGetUsers_WithSearchFilter() {
        // Arrange
        User user1 = new User();
        user1.setId(1);
        user1.setEmail("john@example.com");
        user1.setFullName("John Doe");
        user1.setRole(Role.CITIZEN);
        user1.setStatus(UserStatus.ACTIVE);
        user1.setCreatedAt(LocalDateTime.now());

        Page<User> userPage = new PageImpl<>(Arrays.asList(user1));

        when(userRepository.findUsers(eq("john"), any(), any(), any(Pageable.class)))
            .thenReturn(userPage);

        // Act
        Page<AdminManagementResponse> result = adminUserService.getUsers(0, 10, "john", null, null);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals("john@example.com", result.getContent().get(0).getEmail());
    }

    @Test
    void testGetUsers_WithRoleFilter() {
        // Arrange
        User lawyer = new User();
        lawyer.setId(1);
        lawyer.setEmail("lawyer@example.com");
        lawyer.setFullName("Test Lawyer");
        lawyer.setRole(Role.LAWYER);
        lawyer.setStatus(UserStatus.ACTIVE);
        lawyer.setCreatedAt(LocalDateTime.now());

        Page<User> userPage = new PageImpl<>(Arrays.asList(lawyer));

        when(userRepository.findUsers(anyString(), eq(Role.LAWYER), any(), any(Pageable.class)))
            .thenReturn(userPage);

        // Act
        Page<AdminManagementResponse> result = adminUserService.getUsers(0, 10, "", "LAWYER", null);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals("LAWYER", result.getContent().get(0).getRole());
    }

    @Test
    void testGetUsers_WithStatusFilter() {
        // Arrange
        User suspendedUser = new User();
        suspendedUser.setId(1);
        suspendedUser.setEmail("suspended@example.com");
        suspendedUser.setFullName("Suspended User");
        suspendedUser.setRole(Role.CITIZEN);
        suspendedUser.setStatus(UserStatus.SUSPENDED);
        suspendedUser.setCreatedAt(LocalDateTime.now());

        Page<User> userPage = new PageImpl<>(Arrays.asList(suspendedUser));

        when(userRepository.findUsers(anyString(), any(), eq(UserStatus.SUSPENDED), any(Pageable.class)))
            .thenReturn(userPage);

        // Act
        Page<AdminManagementResponse> result = adminUserService.getUsers(0, 10, "", null, UserStatus.SUSPENDED);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(UserStatus.SUSPENDED, result.getContent().get(0).getStatus());
    }

    @Test
    void testGetUsers_EmptyResults() {
        // Arrange
        Page<User> emptyPage = new PageImpl<>(Arrays.asList());

        when(userRepository.findUsers(anyString(), any(), any(), any(Pageable.class)))
            .thenReturn(emptyPage);

        // Act
        Page<AdminManagementResponse> result = adminUserService.getUsers(0, 10, "nonexistent", null, null);

        // Assert
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void testGetUsers_Pagination() {
        // Arrange
        User user1 = new User();
        user1.setId(1);
        user1.setEmail("user1@example.com");
        user1.setFullName("User One");
        user1.setRole(Role.CITIZEN);
        user1.setStatus(UserStatus.ACTIVE);
        user1.setCreatedAt(LocalDateTime.now());

        Page<User> userPage = new PageImpl<>(Arrays.asList(user1), PageRequest.of(1, 5), 20);

        when(userRepository.findUsers(anyString(), any(), any(), any(Pageable.class)))
            .thenReturn(userPage);

        // Act
        Page<AdminManagementResponse> result = adminUserService.getUsers(1, 5, "", null, null);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());
        assertEquals(20, result.getTotalElements());
    }
}

