package com.legalaid.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalaid.backend.dto.CitizenProfileUpdateDTO;
import com.legalaid.backend.dto.system.MaintenanceStatusResponse;
import com.legalaid.backend.model.*;
import com.legalaid.backend.repository.*;
import com.legalaid.backend.service.system.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ProfileRepository profileRepository;

    @MockBean
    private LawyerProfileRepository lawyerProfileRepository;

    @MockBean
    private NGOProfileRepository ngoProfileRepository;

    @MockBean
    private SystemSettingsService systemSettingsService;

    private User citizenUser;
    private User lawyerUser;
    private User ngoUser;
    private Profile citizenProfile;
    private LawyerProfile lawyerProfile;
    private NGOProfile ngoProfile;

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

        ngoUser = new User();
        ngoUser.setId(3);
        ngoUser.setEmail("ngo@example.com");
        ngoUser.setFullName("Test NGO");
        ngoUser.setRole(Role.NGO);
        ngoUser.setStatus(UserStatus.ACTIVE);
        
        citizenProfile = new Profile();
        citizenProfile.setId(1);
        citizenProfile.setUser(citizenUser);
        citizenProfile.setLocation("Test Location");

        lawyerProfile = new LawyerProfile();
        lawyerProfile.setId(1);
        lawyerProfile.setUser(lawyerUser);
        lawyerProfile.setSpecialization("Criminal Law");

        ngoProfile = new NGOProfile();
        ngoProfile.setId(1);
        ngoProfile.setUser(ngoUser);
        ngoProfile.setNgoName("Test NGO");

        // Mock maintenance status to be disabled
        when(systemSettingsService.getMaintenanceStatus())
                .thenReturn(new MaintenanceStatusResponse(false, null, null, null));
    }

    @Test
    @WithMockUser(username = "citizen@example.com", authorities = {"CITIZEN"})
    void testGetMyProfile_Success() throws Exception {
        // Arrange
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizenUser));

        // Act & Assert
        mockMvc.perform(get("/profile/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("citizen@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test Citizen"))
                .andExpect(jsonPath("$.role").value("CITIZEN"));
    }

    @Test
    @WithMockUser(username = "citizen@example.com", authorities = {"CITIZEN"})
    void testUpdateCitizenProfile_Success() throws Exception {
        // Arrange
        CitizenProfileUpdateDTO updateDTO = new CitizenProfileUpdateDTO();
        updateDTO.setFullName("Updated Citizen Name");
        updateDTO.setContactInfo("updated@contact.com");
        updateDTO.setLocation("Updated Location");

        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizenUser));
        when(profileRepository.findByUser(citizenUser)).thenReturn(Optional.of(citizenProfile));
        when(profileRepository.save(any(Profile.class))).thenReturn(citizenProfile);

        // Act & Assert - endpoint returns the profile object
        mockMvc.perform(put("/profile/update/citizen")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());

        verify(profileRepository, times(1)).save(any(Profile.class));
    }

    @Test
    @WithMockUser(username = "lawyer@example.com", authorities = {"LAWYER"})
    void testUpdateLawyerProfile_Success() throws Exception {
        // Arrange
        String updateJson = """
                {
                    "specialization": "Updated Specialization",
                    "experienceYears": 10,
                    "bio": "Updated bio",
                    "city": "Updated City"
                }
                """;

        when(userRepository.findByEmail("lawyer@example.com")).thenReturn(Optional.of(lawyerUser));
        when(lawyerProfileRepository.findByUser(lawyerUser)).thenReturn(Optional.of(lawyerProfile));
        when(lawyerProfileRepository.save(any(LawyerProfile.class))).thenReturn(lawyerProfile);

        // Act & Assert - endpoint returns the profile object
        mockMvc.perform(put("/profile/update/lawyer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk());

        verify(lawyerProfileRepository, times(1)).save(any(LawyerProfile.class));
    }

    @Test
    @WithMockUser(username = "ngo@example.com", authorities = {"NGO"})
    void testUpdateNGOProfile_Success() throws Exception {
        // Arrange
        String updateJson = """
                {
                    "ngoName": "Updated NGO Name",
                    "description": "Updated description",
                    "city": "Updated City"
                }
                """;

        when(userRepository.findByEmail("ngo@example.com")).thenReturn(Optional.of(ngoUser));
        when(ngoProfileRepository.findByUser(ngoUser)).thenReturn(Optional.of(ngoProfile));
        when(ngoProfileRepository.save(any(NGOProfile.class))).thenReturn(ngoProfile);

        // Act & Assert - endpoint returns the profile object
        mockMvc.perform(put("/profile/update/ngo")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk());

        verify(ngoProfileRepository, times(1)).save(any(NGOProfile.class));
    }

    @Test
    @WithMockUser(username = "lawyer@example.com", authorities = {"LAWYER"})
    void testGetMyProfile_LawyerSuccess() throws Exception {
        // Arrange
        when(userRepository.findByEmail("lawyer@example.com")).thenReturn(Optional.of(lawyerUser));

        // Act & Assert
        mockMvc.perform(get("/profile/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("lawyer@example.com"))
                .andExpect(jsonPath("$.role").value("LAWYER"));
    }
}
