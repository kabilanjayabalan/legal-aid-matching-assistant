package com.legalaid.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalaid.backend.dto.*;
import com.legalaid.backend.dto.analytics.CaseStatsResponse;
import com.legalaid.backend.model.*;
import com.legalaid.backend.repository.*;
import com.legalaid.backend.dto.system.MaintenanceStatusResponse;
import com.legalaid.backend.service.*;
import com.legalaid.backend.service.analytics.CaseAnalyticsService;
import com.legalaid.backend.service.analytics.MatchAnalyticsService;
import com.legalaid.backend.service.system.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@DisplayName("Admin Controller Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private LawyerProfileRepository lawyerProfileRepository;

    @MockBean
    private NGOProfileRepository ngoProfileRepository;

    @MockBean
    private CaseAnalyticsService caseAnalyticsService;

    @MockBean
    private SystemHealthService systemHealthService;

    @MockBean
    private ActuatorMetricsService actuatorMetricsService;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private AppointmentRepository appointmentRepository;

    @MockBean
    private MatchRepository matchRepository;

    @MockBean
    private MatchAnalyticsService matchAnalyticsService;

    @MockBean
    private LogCleanupService logCleanupService;

    @MockBean
    private SystemSettingsService systemSettingsService;

    private User testUser;
    private User testNgoUser;
    private LawyerProfile testLawyerProfile;
    private NGOProfile testNgoProfile;
    private Appointment testAppointment;
    private Match testMatch;
    private Case testCase;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testlawyer");
        testUser.setEmail("lawyer@example.com");
        testUser.setFullName("Test Lawyer");
        testUser.setRole(Role.LAWYER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setApproved(true);
        testUser.setCreatedAt(LocalDateTime.now());

        testNgoUser = new User();
        testNgoUser.setId(2);
        testNgoUser.setUsername("testngo");
        testNgoUser.setEmail("ngo@example.com");
        testNgoUser.setFullName("Test NGO");
        testNgoUser.setRole(Role.NGO);
        testNgoUser.setStatus(UserStatus.ACTIVE);
        testNgoUser.setApproved(true);
        testNgoUser.setCreatedAt(LocalDateTime.now());

        testLawyerProfile = new LawyerProfile();
        testLawyerProfile.setId(1);
        testLawyerProfile.setUser(testUser);
        testLawyerProfile.setName("John Lawyer");
        testLawyerProfile.setSpecialization("Criminal Law");
        testLawyerProfile.setExpertise("Criminal Defense");
        testLawyerProfile.setLocation("New York");
        testLawyerProfile.setContactInfo("contact@example.com");
        testLawyerProfile.setLanguage("English");
        testLawyerProfile.setVerified(true);

        testNgoProfile = new NGOProfile();
        testNgoProfile.setId(1);
        testNgoProfile.setUser(testNgoUser);
        testNgoProfile.setOrganization("Legal Aid Org");
        testNgoProfile.setNgoName("Legal Aid NGO");
        testNgoProfile.setLocation("New York");
        testNgoProfile.setContactInfo("ngo@example.com");
        testNgoProfile.setLanguage("English");
        testNgoProfile.setVerified(true);

        testCase = new Case();
        testCase.setId(1);
        testCase.setTitle("Test Case");
        testCase.setCaseNumber("CS-2026-001");
        testCase.setCaseType(CaseType.CS);
        testCase.setCategory("CIVIL");
        testCase.setStatus(CaseStatus.OPEN);
        testCase.setCreatedAt(LocalDateTime.now());

        testMatch = new Match();
        testMatch.setId(1);
        testMatch.setCaseObj(testCase);
        testMatch.setProviderType(ProviderType.LAWYER);
        testMatch.setProviderId(1);
        testMatch.setStatus(MatchStatus.PENDING);
        testMatch.setScore(85);
        testMatch.setCreatedAt(LocalDateTime.now());

        testAppointment = new Appointment();
        testAppointment.setId(1L);
        testAppointment.setMatchId(1L);
        testAppointment.setStatus(AppointmentStatus.PENDING);
        testAppointment.setRequesterId(1L);
        testAppointment.setReceiverId(2L);
        testAppointment.setAppointmentDate(LocalDate.now());
        testAppointment.setTimeSlot("10:00-11:00");
        testAppointment.setTimeZone("UTC");
        testAppointment.setDurationMinutes(60);
        testAppointment.setCreatedAt(LocalDateTime.now());

        // Mock maintenance status to be disabled
        when(systemSettingsService.getMaintenanceStatus())
                .thenReturn(new MaintenanceStatusResponse(false, null, null, null));
    }

    @Nested
    @DisplayName("User Approval Tests")
    class UserApprovalTests {

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should approve user successfully")
        void shouldApproveUserSuccessfully() throws Exception {
            // Given
            when(authService.approveUserByUsername("testlawyer")).thenReturn(testUser);

            // When & Then
            mockMvc.perform(post("/admin/approve/testlawyer")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User approved: " + testUser.getEmail()));

            verify(authService, times(1)).approveUserByUsername("testlawyer");
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should reject user successfully")
        void shouldRejectUserSuccessfully() throws Exception {
            // Given
            when(authService.rejectUserByUsername("testlawyer")).thenReturn(testUser);

            // When & Then
            mockMvc.perform(post("/admin/reject/testlawyer")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User rejected: " + testUser.getEmail()));

            verify(authService, times(1)).rejectUserByUsername("testlawyer");
        }
    }

    @Nested
    @DisplayName("Profile Update Tests")
    class ProfileUpdateTests {

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should update lawyer profile successfully")
        void shouldUpdateLawyerProfileSuccessfully() throws Exception {
            // Given
            AdminController.LawyerNgoProfileUpdate updatePayload = new AdminController.LawyerNgoProfileUpdate(
                    "Updated Name", "Updated Expertise", "Updated Location",
                    "Updated Contact", null, "English"
            );

            when(userRepository.findByUsername("testlawyer")).thenReturn(Optional.of(testUser));
            when(lawyerProfileRepository.findByUser(testUser)).thenReturn(Optional.of(testLawyerProfile));
            when(lawyerProfileRepository.save(any(LawyerProfile.class))).thenReturn(testLawyerProfile);

            // When & Then
            mockMvc.perform(put("/admin/lawyers-ngos/testlawyer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatePayload))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Name"));

            verify(lawyerProfileRepository).save(any(LawyerProfile.class));
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should update NGO profile successfully")
        void shouldUpdateNgoProfileSuccessfully() throws Exception {
            // Given
            AdminController.LawyerNgoProfileUpdate updatePayload = new AdminController.LawyerNgoProfileUpdate(
                    null, null, "Updated Location",
                    "Updated Contact", "Updated Org", "English"
            );

            when(userRepository.findByUsername("testngo")).thenReturn(Optional.of(testNgoUser));
            when(ngoProfileRepository.findByUser(testNgoUser)).thenReturn(Optional.of(testNgoProfile));
            when(ngoProfileRepository.save(any(NGOProfile.class))).thenReturn(testNgoProfile);

            // When & Then
            mockMvc.perform(put("/admin/lawyers-ngos/testngo")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatePayload))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.organization").value("Updated Org"));

            verify(ngoProfileRepository).save(any(NGOProfile.class));
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should return error when user not found for profile update")
        void shouldReturnErrorWhenUserNotFoundForProfileUpdate() throws Exception {
            // Given
            AdminController.LawyerNgoProfileUpdate updatePayload = new AdminController.LawyerNgoProfileUpdate(
                    "Name", null, null, null, null, null
            );

            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(put("/admin/lawyers-ngos/unknown")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatePayload))
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("User not found")));
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should return error when user is not lawyer or NGO")
        void shouldReturnErrorWhenUserIsNotLawyerOrNgo() throws Exception {
            // Given
            User citizenUser = new User();
            citizenUser.setRole(Role.CITIZEN);
            AdminController.LawyerNgoProfileUpdate updatePayload = new AdminController.LawyerNgoProfileUpdate(
                    "Name", null, null, null, null, null
            );

            when(userRepository.findByUsername("citizen")).thenReturn(Optional.of(citizenUser));

            // When & Then
            mockMvc.perform(put("/admin/lawyers-ngos/citizen")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updatePayload))
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Only lawyers and NGOs")));
        }
    }

    @Nested
    @DisplayName("Verification Tests")
    class VerificationTests {

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should mark lawyer profile as verified")
        void shouldMarkLawyerProfileAsVerified() throws Exception {
            // Given
            when(userRepository.findByUsername("testlawyer")).thenReturn(Optional.of(testUser));
            when(lawyerProfileRepository.findByUser(testUser)).thenReturn(Optional.of(testLawyerProfile));
            when(lawyerProfileRepository.save(any(LawyerProfile.class))).thenReturn(testLawyerProfile);

            // When & Then
            mockMvc.perform(put("/admin/verify/testlawyer/true")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("verification set to true")));

            verify(lawyerProfileRepository).save(any(LawyerProfile.class));
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should mark NGO profile as unverified")
        void shouldMarkNgoProfileAsUnverified() throws Exception {
            // Given
            when(userRepository.findByUsername("testngo")).thenReturn(Optional.of(testNgoUser));
            when(ngoProfileRepository.findByUser(testNgoUser)).thenReturn(Optional.of(testNgoProfile));
            when(ngoProfileRepository.save(any(NGOProfile.class))).thenReturn(testNgoProfile);

            // When & Then
            mockMvc.perform(put("/admin/verify/testngo/false")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("verification set to false")));

            verify(ngoProfileRepository).save(any(NGOProfile.class));
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should return error when user not found for verification")
        void shouldReturnErrorWhenUserNotFoundForVerification() throws Exception {
            // Given
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(put("/admin/verify/unknown/true")
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("User not found")));
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should return error when lawyer profile not found")
        void shouldReturnErrorWhenLawyerProfileNotFound() throws Exception {
            // Given
            when(userRepository.findByUsername("testlawyer")).thenReturn(Optional.of(testUser));
            when(lawyerProfileRepository.findByUser(testUser)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(put("/admin/verify/testlawyer/true")
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Lawyer profile not found")));
        }
    }

    @Nested
    @DisplayName("User Management Tests")
    class UserManagementTests {

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get all users with pagination")
        void shouldGetAllUsersWithPagination() throws Exception {
            // Given
            AdminManagementResponse response = new AdminManagementResponse(
                    1, testUser.getEmail(), testUser.getFullName(), "LAWYER",
                    UserStatus.ACTIVE, LocalDateTime.now().toString()
            );

            when(adminUserService.getUsers(anyInt(), anyInt(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(response)));

            // When & Then
            mockMvc.perform(get("/admin/users")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].email").value(testUser.getEmail()));

            verify(adminUserService).getUsers(0, 10, null, null, null);
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should activate user")
        void shouldActivateUser() throws Exception {
            // Given
            doNothing().when(adminUserService).changeStatus(1, UserStatus.ACTIVE);

            // When & Then
            mockMvc.perform(put("/admin/users/1/activate")
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(adminUserService).changeStatus(1, UserStatus.ACTIVE);
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should deactivate user")
        void shouldDeactivateUser() throws Exception {
            // Given
            doNothing().when(adminUserService).changeStatus(1, UserStatus.INACTIVE);

            // When & Then
            mockMvc.perform(put("/admin/users/1/deactivate")
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(adminUserService).changeStatus(1, UserStatus.INACTIVE);
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should suspend user")
        void shouldSuspendUser() throws Exception {
            // Given
            doNothing().when(adminUserService).changeStatus(1, UserStatus.SUSPENDED);

            // When & Then
            mockMvc.perform(put("/admin/users/1/suspend")
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(adminUserService).changeStatus(1, UserStatus.SUSPENDED);
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should block user")
        void shouldBlockUser() throws Exception {
            // Given
            doNothing().when(adminUserService).changeStatus(1, UserStatus.BLOCKED);

            // When & Then
            mockMvc.perform(put("/admin/users/1/block")
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(adminUserService).changeStatus(1, UserStatus.BLOCKED);
        }
    }

    @Nested
    @DisplayName("Case Analytics Tests")
    class CaseAnalyticsTests {

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get case stats")
        void shouldGetCaseStats() throws Exception {
            // Given
            CaseStatsResponse stats = new CaseStatsResponse(100L, 50L, 30L, 20L);
            when(caseAnalyticsService.getCaseStats()).thenReturn(stats);

            // When & Then
            mockMvc.perform(get("/admin/cases/stats"))
                    .andExpect(status().isOk());

            verify(caseAnalyticsService).getCaseStats();
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get cases for monitoring")
        void shouldGetCasesForMonitoring() throws Exception {
            // Given
            CaseMonitoringResponse response = CaseMonitoringResponse.builder()
                    .id("CASE-1")
                    .title("Test Case")
                    .category("CIVIL")
                    .status("OPEN")
                    .build();
            when(caseAnalyticsService.getPaginatedCases(isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(response)));

            // When & Then
            mockMvc.perform(get("/admin/cases/monitoring")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());

            verify(caseAnalyticsService).getPaginatedCases(isNull(), isNull(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Lawyer/NGO Listing Tests")
    class LawyerNgoListingTests {

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get lawyers and NGOs with filters")
        void shouldGetLawyersAndNgosWithFilters() throws Exception {
            // Given
            when(userRepository.findLawyersAndNgos(any(), any(), anyBoolean(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(testUser)));

            // When & Then
            mockMvc.perform(get("/admin/users/lawyers-ngos")
                            .param("page", "0")
                            .param("size", "10")
                            .param("role", "LAWYER")
                            .param("status", "APPROVED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get lawyers and NGOs with pending status")
        void shouldGetLawyersAndNgosWithPendingStatus() throws Exception {
            // Given
            when(userRepository.findLawyersAndNgos(any(), any(), eq(true), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(testUser)));

            // When & Then
            mockMvc.perform(get("/admin/users/lawyers-ngos")
                            .param("status", "PENDING"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get lawyer-ngo profiles")
        void shouldGetLawyerNgoProfiles() throws Exception {
            // Given
            when(lawyerProfileRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(testLawyerProfile)));
            when(ngoProfileRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(testNgoProfile)));

            // When & Then
            mockMvc.perform(get("/admin/lawyer-ngo")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").exists());
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get pending lawyer-ngo profiles")
        void shouldGetPendingLawyerNgoProfiles() throws Exception {
            // Given
            when(lawyerProfileRepository.findByVerifiedIsNull(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(testLawyerProfile)));
            when(ngoProfileRepository.findByVerifiedIsNull(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(testNgoProfile)));

            // When & Then
            mockMvc.perform(get("/admin/lawyer-ngo")
                            .param("pendingOnly", "true"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("System Health Tests")
    class SystemHealthTests {

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get system health")
        void shouldGetSystemHealth() throws Exception {
            // Given
            SystemHealthService.SystemHealthDTO health = new SystemHealthService.SystemHealthDTO(
                    50.0, 60.0, 10L, 40.0, 5L, 10L, 100L
            );

            when(systemHealthService.getSystemHealth()).thenReturn(health);

            // When & Then
            mockMvc.perform(get("/admin/system-health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cpuUsage").value(50.0))
                    .andExpect(jsonPath("$.memoryUsage").value(60.0));

            verify(systemHealthService).getSystemHealth();
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get system load over time")
        void shouldGetSystemLoadOverTime() throws Exception {
            // Given
            SystemHealthService.SystemLoadPointDTO point = new SystemHealthService.SystemLoadPointDTO(
                    "2026-01-22", 50L
            );

            when(systemHealthService.getSystemLoadOverTime()).thenReturn(List.of(point));

            // When & Then
            mockMvc.perform(get("/admin/system-load-over-time"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            verify(systemHealthService).getSystemLoadOverTime();
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get service activity breakdown")
        void shouldGetServiceActivityBreakdown() throws Exception {
            // Given
            SystemHealthService.ServiceActivityDTO activity = new SystemHealthService.ServiceActivityDTO(
                    "ServiceName", 100L
            );

            when(systemHealthService.getServiceActivityBreakdown()).thenReturn(List.of(activity));

            // When & Then
            mockMvc.perform(get("/admin/service-activity"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            verify(systemHealthService).getServiceActivityBreakdown();
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get actuator metrics")
        void shouldGetActuatorMetrics() throws Exception {
            // Given
            ActuatorMetricsService.ActuatorSystemMetrics metrics = mock(
                    ActuatorMetricsService.ActuatorSystemMetrics.class);

            when(actuatorMetricsService.getSystemMetrics()).thenReturn(metrics);

            // When & Then
            mockMvc.perform(get("/admin/actuator-metrics"))
                    .andExpect(status().isOk());

            verify(actuatorMetricsService).getSystemMetrics();
        }
    }

    @Nested
    @DisplayName("Appointment Tests")
    class AppointmentTests {

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get appointment stats")
        void shouldGetAppointmentStats() throws Exception {
            // Given
            AppointmentStatsDTO stats = new AppointmentStatsDTO(100L, 20L, 30L, 25L, 15L, 10L);
            when(adminUserService.getAppointmentStats()).thenReturn(stats);

            // When & Then
            mockMvc.perform(get("/admin/appointments/stats"))
                    .andExpect(status().isOk());

            verify(adminUserService).getAppointmentStats();
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get all appointments")
        void shouldGetAllAppointments() throws Exception {
            // Given
            when(appointmentRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(testAppointment)));
            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(2)).thenReturn(Optional.of(testNgoUser));
            when(matchRepository.findById(1)).thenReturn(Optional.of(testMatch));

            // When & Then
            mockMvc.perform(get("/admin/appointments")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get appointments filtered by status")
        void shouldGetAppointmentsFilteredByStatus() throws Exception {
            // Given
            when(appointmentRepository.findByStatus(eq(AppointmentStatus.PENDING), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(testAppointment)));
            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(2)).thenReturn(Optional.of(testNgoUser));
            when(matchRepository.findById(1)).thenReturn(Optional.of(testMatch));

            // When & Then
            mockMvc.perform(get("/admin/appointments")
                            .param("status", "PENDING"))
                    .andExpect(status().isOk());

            verify(appointmentRepository).findByStatus(eq(AppointmentStatus.PENDING), any(Pageable.class));
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get appointments filtered by search")
        void shouldGetAppointmentsFilteredBySearch() throws Exception {
            // Given
            when(appointmentRepository.findByMatchIdContainingIgnoreCase(eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(testAppointment)));
            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(2)).thenReturn(Optional.of(testNgoUser));
            when(matchRepository.findById(1)).thenReturn(Optional.of(testMatch));

            // When & Then
            mockMvc.perform(get("/admin/appointments")
                            .param("search", "1"))
                    .andExpect(status().isOk());

            verify(appointmentRepository).findByMatchIdContainingIgnoreCase(eq(1L), any(Pageable.class));
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should return bad request for invalid appointment status")
        void shouldReturnBadRequestForInvalidAppointmentStatus() throws Exception {
            // When & Then
            mockMvc.perform(get("/admin/appointments")
                            .param("status", "INVALID"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Match Analytics Tests")
    class MatchAnalyticsTests {

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get match stats")
        void shouldGetMatchStats() throws Exception {
            // Given
            MatchStatsDTO stats = new MatchStatsDTO(100L, 30L, 25L, 20L, 15L, 75.5);
            when(matchAnalyticsService.getMatchStats()).thenReturn(stats);

            // When & Then
            mockMvc.perform(get("/admin/matches/stats"))
                    .andExpect(status().isOk());

            verify(matchAnalyticsService).getMatchStats();
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should get matches for monitoring")
        void shouldGetMatchesForMonitoring() throws Exception {
            // Given
            // Use an empty page to avoid serialization issues with mock
            when(matchAnalyticsService.getMatchesForMonitoring(isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(new ArrayList<>()));

            // When & Then
            mockMvc.perform(get("/admin/matches/monitoring")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());

            verify(matchAnalyticsService).getMatchesForMonitoring(isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should delete match successfully")
        void shouldDeleteMatchSuccessfully() throws Exception {
            // Given
            when(matchRepository.findById(1)).thenReturn(Optional.of(testMatch));
            doNothing().when(matchRepository).deleteById(1);

            // When & Then
            mockMvc.perform(delete("/admin/matches/1")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Match deleted successfully"));

            verify(matchRepository).deleteById(1);
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should return not found when match does not exist")
        void shouldReturnNotFoundWhenMatchDoesNotExist() throws Exception {
            // Given
            when(matchRepository.findById(999)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(delete("/admin/matches/999")
                            .with(csrf()))
                    .andExpect(status().isNotFound());

            verify(matchRepository, never()).deleteById(anyInt());
        }
    }

    @Nested
    @DisplayName("Log Cleanup Tests")
    class LogCleanupTests {

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should cleanup logs successfully")
        void shouldCleanupLogsSuccessfully() throws Exception {
            // Given
            doNothing().when(logCleanupService).cleanupLogsManually(30);

            // When & Then
            mockMvc.perform(post("/admin/logs/cleanup")
                            .param("days", "30")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("Logs older than 30 days")));

            verify(logCleanupService).cleanupLogsManually(30);
        }

        @Test
        @WithMockUser(username = "admin@example.com", authorities = {"ROLE_ADMIN"})
        @DisplayName("Should use default days when not provided")
        void shouldUseDefaultDaysWhenNotProvided() throws Exception {
            // Given
            doNothing().when(logCleanupService).cleanupLogsManually(30);

            // When & Then
            mockMvc.perform(post("/admin/logs/cleanup")
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(logCleanupService).cleanupLogsManually(30);
        }
    }
}
