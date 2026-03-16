package com.legalaid.backend.controller;

import com.legalaid.backend.model.*;
import com.legalaid.backend.repository.*;
import com.legalaid.backend.dto.system.MaintenanceStatusResponse;
import com.legalaid.backend.service.MatchingService;
import com.legalaid.backend.service.NotificationService;
import com.legalaid.backend.service.system.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchController.class)
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchingService matchingService;

    @MockBean
    private MatchRepository matchRepository;

    @MockBean
    private CaseRepository caseRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private LawyerProfileRepository lawyerProfileRepository;

    @MockBean
    private NGOProfileRepository ngoProfileRepository;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private SystemSettingsService systemSettingsService;

    private User citizenUser;
    private User lawyerUser;
    private Case testCase;
    private Match testMatch;

    @BeforeEach
    void setUp() {
        citizenUser = new User();
        citizenUser.setId(1);
        citizenUser.setEmail("citizen@example.com");
        citizenUser.setFullName("Test Citizen");
        citizenUser.setRole(Role.CITIZEN);
        citizenUser.setStatus(UserStatus.ACTIVE);

        lawyerUser = new User();
        lawyerUser.setId(2);
        lawyerUser.setEmail("lawyer@example.com");
        lawyerUser.setFullName("Test Lawyer");
        lawyerUser.setRole(Role.LAWYER);
        lawyerUser.setStatus(UserStatus.ACTIVE);

        testCase = new Case();
        testCase.setId(1);
        testCase.setTitle("Test Case");
        testCase.setCategory("Criminal Law");
        testCase.setStatus(CaseStatus.OPEN);
        testCase.setCreatedBy(citizenUser);
        testCase.setCreatedAt(LocalDateTime.now());

        testMatch = new Match();
        testMatch.setId(1);
        testMatch.setCaseObj(testCase);
        testMatch.setProviderType(ProviderType.LAWYER);
        testMatch.setProviderId(1);
        testMatch.setScore(85);
        testMatch.setStatus(MatchStatus.PENDING);
        testMatch.setCreatedAt(LocalDateTime.now());

        // Mock maintenance status to be disabled
        when(systemSettingsService.getMaintenanceStatus())
                .thenReturn(new MaintenanceStatusResponse(false, null, null, null));
    }

//    @Test
//    @WithMockUser(username = "citizen@example.com", authorities = {"CITIZEN"})
//    void testGenerateMatches_Success() throws Exception {
//        // Arrange
//        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizenUser));
//        when(caseRepository.findById(1)).thenReturn(Optional.of(testCase));
//
//        MatchController.MatchCard card = new MatchController.MatchCard(
//            "REGISTERED", "LAWYER", 1, "Test Lawyer", "City",
//            "Criminal Law", 85, true, true, MatchStatus.PENDING, CaseStatus.OPEN
//        );
//        MatchController.MatchResult result = new MatchController.MatchResult(Arrays.asList(card));
//
//        when(matchingService.generateMatches(any(Case.class),40)).thenReturn(result);
//
//        // Act & Assert
//        mockMvc.perform(post("/matches/generate/1")
//                        .with(csrf()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.results").isArray());
//    }

    @Test
    @WithMockUser(username = "citizen@example.com", authorities = {"CITIZEN"})
    void testGetMyCases_Success() throws Exception {
        // Arrange
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizenUser));
        when(matchRepository.findCitizenMatches(eq(1), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Arrays.asList()));

        // Act & Assert
        mockMvc.perform(get("/matches/my-cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(username = "citizen@example.com", authorities = {"CITIZEN"})
    void testCitizenAcceptMatch_Success() throws Exception {
        // Arrange
        testMatch.setStatus(MatchStatus.PENDING);
        testCase.setCreatedBy(citizenUser);

        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizenUser));
        when(matchRepository.findById(1)).thenReturn(Optional.of(testMatch));
        when(matchRepository.save(any(Match.class))).thenReturn(testMatch);

        // Act & Assert
        mockMvc.perform(put("/matches/1/citizen-accept")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(matchRepository, times(1)).save(any(Match.class));
    }

    @Test
    @WithMockUser(username = "citizen@example.com", authorities = {"CITIZEN"})
    void testCitizenRejectMatch_Success() throws Exception {
        // Arrange
        testMatch.setStatus(MatchStatus.PENDING);
        testCase.setCreatedBy(citizenUser);

        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizenUser));
        when(matchRepository.findById(1)).thenReturn(Optional.of(testMatch));
        when(matchRepository.save(any(Match.class))).thenReturn(testMatch);

        // Act & Assert
        mockMvc.perform(put("/matches/1/citizen-reject")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(matchRepository, times(1)).save(any(Match.class));
    }
}

