package com.legalaid.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.model.UserStatus;
import com.legalaid.backend.dto.system.MaintenanceStatusResponse;
import com.legalaid.backend.security.JwtUtils;
import com.legalaid.backend.service.AuthService;
import com.legalaid.backend.service.system.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private AuthService authService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private SystemSettingsService systemSettingsService;

    private User testUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.CITIZEN);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setApproved(true);

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("encodedPassword")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("CITIZEN")))
                .build();

        // Mock maintenance status to be disabled
        when(systemSettingsService.getMaintenanceStatus())
                .thenReturn(new MaintenanceStatusResponse(false, null, null, null));
    }

    @Test
    @WithMockUser
    void testLogin_Success() throws Exception {
        // Arrange
        String loginJson = """
                {
                    "email": "test@example.com",
                    "password": "password123"
                }
                """;

        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(authService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(jwtUtils.generateAccessToken(any())).thenReturn("accessToken");
        when(jwtUtils.generateRefreshToken(any())).thenReturn("refreshToken");

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.refreshToken").value("refreshToken"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("CITIZEN"));
    }

    @Test
    @WithMockUser
    void testLogin_InvalidCredentials() throws Exception {
        // Arrange
        String loginJson = """
                {
                    "email": "test@example.com",
                    "password": "wrongpassword"
                }
                """;

        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    void testLogin_LawyerPendingApproval() throws Exception {
        // Arrange
        testUser.setRole(Role.LAWYER);
        testUser.setApproved(null); // Pending approval

        String loginJson = """
                {
                    "email": "test@example.com",
                    "password": "password123"
                }
                """;

        UserDetails lawyerDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("encodedPassword")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("LAWYER")))
                .build();

        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(lawyerDetails, null, lawyerDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(authService.getUserByEmail("test@example.com")).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Admin hasn't approved your profile. Please wait for approval"));
    }

    @Test
    @WithMockUser
    void testRegister_Success() throws Exception {
        // Arrange - NOTE: endpoint is /auth/register not /auth/signup
        String registerJson = """
                {
                    "username": "testuser",
                    "email": "newuser@example.com",
                    "password": "Password123!",
                    "fullName": "New User",
                    "role": "CITIZEN"
                }
                """;

        when(authService.existsByEmail("newuser@example.com")).thenReturn(false);
        when(authService.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));
    }

    @Test
    @WithMockUser
    void testRegister_EmailAlreadyExists() throws Exception {
        // Arrange
        String registerJson = """
                {
                    "username": "testuser",
                    "email": "existing@example.com",
                    "password": "Password123!",
                    "fullName": "New User",
                    "role": "CITIZEN"
                }
                """;

        when(authService.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Email is already in use!"));
    }

    @Test
    @WithMockUser
    void testRegister_UsernameAlreadyExists() throws Exception {
        // Arrange
        String registerJson = """
                {
                    "username": "existinguser",
                    "email": "newuser@example.com",
                    "password": "Password123!",
                    "fullName": "New User",
                    "role": "CITIZEN"
                }
                """;

        when(authService.existsByEmail("newuser@example.com")).thenReturn(false);
        when(authService.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Username is already in use!"));
    }
}
