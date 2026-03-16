package com.legalaid.backend.service;

import com.legalaid.backend.model.*;
import com.legalaid.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DirectoryLawyerRepository directoryLawyerRepository;

    @Mock
    private DirectoryNgoRepository directoryNgoRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private LawyerProfileRepository lawyerProfileRepository;

    @Mock
    private NGOProfileRepository ngoProfileRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.CITIZEN);
        testUser.setStatus(UserStatus.ACTIVE);
    }

    @Test
    void testLoadUserByUsername_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = authService.loadUserByUsername("test@example.com");

        // Assert
        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("CITIZEN")));
    }

    @Test
    void testLoadUserByUsername_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
            () -> authService.loadUserByUsername("nonexistent@example.com"));
    }

    @Test
    void testExistsByEmail_True() {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act
        boolean exists = authService.existsByEmail("test@example.com");

        // Assert
        assertTrue(exists);
    }

    @Test
    void testExistsByEmail_False() {
        // Arrange
        when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);

        // Act
        boolean exists = authService.existsByEmail("nonexistent@example.com");

        // Assert
        assertFalse(exists);
    }

    @Test
    void testExistsByUsername_True() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act
        boolean exists = authService.existsByUsername("testuser");

        // Assert
        assertTrue(exists);
    }

    @Test
    void testSaveUser() {
        // Arrange
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        authService.saveUser(testUser);

        // Assert
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testApproveUser_Success() {
        // Arrange
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User approvedUser = authService.approveUser(1);

        // Assert
        assertNotNull(approvedUser);
        assertTrue(approvedUser.getApproved());
        verify(userRepository).save(testUser);
    }

    @Test
    void testApproveUser_UserNotFound() {
        // Arrange
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> authService.approveUser(999));
    }

    @Test
    void testApproveUserByUsername_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User approvedUser = authService.approveUserByUsername("testuser");

        // Assert
        assertNotNull(approvedUser);
        assertTrue(approvedUser.getApproved());
    }

    @Test
    void testRejectUserByUsername_Success() {
        // Arrange
        testUser.setApproved(true);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User rejectedUser = authService.rejectUserByUsername("testuser");

        // Assert
        assertNotNull(rejectedUser);
        assertFalse(rejectedUser.getApproved());
    }

    @Test
    void testGetAllUsers() {
        // Arrange
        List<User> users = Arrays.asList(testUser, new User());
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = authService.getAllUsers();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void testGetUserByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        User user = authService.getUserByEmail("test@example.com");

        // Assert
        assertNotNull(user);
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    void testGetUserByEmail_NotFound() {
        // Arrange
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
            () -> authService.getUserByEmail("notfound@example.com"));
    }

    @Test
    void testRegisterUserWithClaim_CitizenAutoActivated() {
        // Arrange
        User citizenUser = new User();
        citizenUser.setRole(Role.CITIZEN);
        citizenUser.setEmail("citizen@example.com");

        when(userRepository.save(any(User.class))).thenReturn(citizenUser);

        // Act
        authService.registerUserWithClaim(citizenUser, null,null);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserStatus.ACTIVE, userCaptor.getValue().getStatus());
        assertNotNull(userCaptor.getValue().getStatusChangedAt());
    }

    @Test
    void testRegisterUserWithClaim_LawyerPendingStatus() {
        // Arrange
        User lawyerUser = new User();
        lawyerUser.setRole(Role.LAWYER);
        lawyerUser.setEmail("lawyer@example.com");

        when(userRepository.save(any(User.class))).thenReturn(lawyerUser);

        // Act
        authService.registerUserWithClaim(lawyerUser, null,null);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserStatus.PENDING, userCaptor.getValue().getStatus());
    }

    @Test
    void testRegisterUserWithClaim_LawyerWithValidBarId() {
        // Arrange
        User lawyerUser = new User();
        lawyerUser.setId(1);
        lawyerUser.setRole(Role.LAWYER);
        lawyerUser.setEmail("lawyer@example.com");

        DirectoryLawyer directoryLawyer = new DirectoryLawyer();
        directoryLawyer.setBarRegistrationId("BAR123");
        directoryLawyer.setFullName("John Lawyer");
        directoryLawyer.setSpecialization("Criminal Law");
        directoryLawyer.setCity("Test City");

        when(userRepository.save(any(User.class))).thenReturn(lawyerUser);
        when(lawyerProfileRepository.findByBarRegistrationNo("BAR123")).thenReturn(Optional.empty());
        when(directoryLawyerRepository.findByBarRegistrationId("BAR123")).thenReturn(directoryLawyer);

        // Act
        authService.registerUserWithClaim(lawyerUser, "BAR123",null);

        // Assert
        verify(userRepository, times(2)).save(any(User.class)); // Once initially, once for approval
        verify(lawyerProfileRepository).save(any(LawyerProfile.class));
    }

    @Test
    void testRegisterUserWithClaim_LawyerWithClaimedBarId() {
        // Arrange
        User lawyerUser = new User();
        lawyerUser.setRole(Role.LAWYER);
        lawyerUser.setEmail("lawyer@example.com");

        LawyerProfile existingProfile = new LawyerProfile();
        existingProfile.setUser(new User());

        when(userRepository.save(any(User.class))).thenReturn(lawyerUser);
        when(lawyerProfileRepository.findByBarRegistrationNo("BAR123")).thenReturn(Optional.of(existingProfile));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> authService.registerUserWithClaim(lawyerUser, "BAR123",null));
    }

    @Test
    void testRegisterUserWithClaim_NgoWithValidRegNumber() {
        // Arrange
        User ngoUser = new User();
        ngoUser.setId(1);
        ngoUser.setRole(Role.NGO);
        ngoUser.setEmail("ngo@example.com");

        DirectoryNgo directoryNgo = new DirectoryNgo();
        directoryNgo.setRegistrationNumber("REG456");
        directoryNgo.setOrgName("Test NGO");
        directoryNgo.setFocusArea("Legal Aid");
        directoryNgo.setCity("Test City");

        when(userRepository.save(any(User.class))).thenReturn(ngoUser);
        when(ngoProfileRepository.findByRegistrationNo("REG456")).thenReturn(Optional.empty());
        when(directoryNgoRepository.findByRegistrationNumber("REG456")).thenReturn(directoryNgo);

        // Act
        authService.registerUserWithClaim(ngoUser, "REG456",null);

        // Assert
        verify(userRepository, times(2)).save(any(User.class));
        verify(ngoProfileRepository).save(any(NGOProfile.class));
    }

    @Test
    void testRegisterUserWithClaim_NgoWithClaimedRegNumber() {
        // Arrange
        User ngoUser = new User();
        ngoUser.setRole(Role.NGO);
        ngoUser.setEmail("ngo@example.com");

        NGOProfile existingProfile = new NGOProfile();
        existingProfile.setUser(new User());

        when(userRepository.save(any(User.class))).thenReturn(ngoUser);
        when(ngoProfileRepository.findByRegistrationNo("REG456")).thenReturn(Optional.of(existingProfile));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> authService.registerUserWithClaim(ngoUser, "REG456",null));
    }
}

