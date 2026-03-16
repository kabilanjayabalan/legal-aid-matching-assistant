package com.legalaid.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.legalaid.backend.dto.MatchResultDTO;
import com.legalaid.backend.dto.ProviderDashboardStatsDTO;
import com.legalaid.backend.model.Case;
import com.legalaid.backend.model.CaseStatus;
import com.legalaid.backend.model.CaseType;
import com.legalaid.backend.model.DirectoryLawyer;
import com.legalaid.backend.model.DirectoryNgo;
import com.legalaid.backend.model.LawyerProfile;
import com.legalaid.backend.model.Match;
import com.legalaid.backend.model.MatchStatus;
import com.legalaid.backend.model.NGOProfile;
import com.legalaid.backend.model.ProviderType;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.repository.DirectoryLawyerRepository;
import com.legalaid.backend.repository.DirectoryNgoRepository;
import com.legalaid.backend.repository.LawyerProfileRepository;
import com.legalaid.backend.repository.MatchRepository;
import com.legalaid.backend.repository.NGOProfileRepository;
import com.legalaid.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Matching Service Tests")
class MatchingServiceTest {

    @Mock
    private LawyerProfileRepository lawyerProfileRepository;

    @Mock
    private NGOProfileRepository ngoProfileRepository;

    @Mock
    private DirectoryLawyerRepository directoryLawyerRepository;

    @Mock
    private DirectoryNgoRepository directoryNgoRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MatchingService matchingService;

    private Case testCase;
    private User testUser;
    private LawyerProfile testLawyerProfile;
    private NGOProfile testNgoProfile;
    private DirectoryLawyer testDirectoryLawyer;
    private DirectoryNgo testDirectoryNgo;
    private Match testMatch;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("lawyer@example.com");
        testUser.setFullName("Test Lawyer");
        testUser.setRole(Role.LAWYER);

        testCase = new Case();
        testCase.setId(1);
        testCase.setTitle("Test Case");
        testCase.setDescription("Criminal case description");
        testCase.setCategory("CRIMINAL");
        testCase.setLocation("New York");
        testCase.setCity("New York");
        testCase.setLatitude(40.7128);
        testCase.setLongitude(-74.0060);
        testCase.setPreferredLanguage("English");
        testCase.setStatus(CaseStatus.OPEN);
        testCase.setCaseNumber("CS-2026-001");
        testCase.setCaseType(CaseType.CR);
        testCase.setCreatedAt(LocalDateTime.now());

        testLawyerProfile = new LawyerProfile();
        testLawyerProfile.setId(1);
        testLawyerProfile.setName("John Lawyer");
        testLawyerProfile.setSpecialization("Criminal Law");
        testLawyerProfile.setExpertise("Criminal Defense");
        testLawyerProfile.setCity("New York");
        testLawyerProfile.setLatitude(40.7128);
        testLawyerProfile.setLongitude(-74.0060);
        testLawyerProfile.setLanguage("English");
        testLawyerProfile.setBio("Experienced criminal lawyer");
        testLawyerProfile.setVerified(true);
        testLawyerProfile.setIsAvailable(true);
        testLawyerProfile.setUser(testUser);

        testNgoProfile = new NGOProfile();
        testNgoProfile.setId(1);
        testNgoProfile.setNgoName("Legal Aid NGO");
        testNgoProfile.setDescription("Criminal legal assistance");
        testNgoProfile.setCity("New York");
        testNgoProfile.setLatitude(40.7128);
        testNgoProfile.setLongitude(-74.0060);
        testNgoProfile.setLanguage("English");
        testNgoProfile.setVerified(true);
        testNgoProfile.setIsAvailable(true);
        testNgoProfile.setUser(testUser);

        testDirectoryLawyer = new DirectoryLawyer();
        testDirectoryLawyer.setId(1);
        testDirectoryLawyer.setFullName("Directory Lawyer");
        testDirectoryLawyer.setSpecialization("Criminal Law");
        testDirectoryLawyer.setCity("New York");
        testDirectoryLawyer.setLatitude(40.7128);
        testDirectoryLawyer.setLongitude(-74.0060);
        testDirectoryLawyer.setLanguage("English");
        testDirectoryLawyer.setVerified(true);

        testDirectoryNgo = new DirectoryNgo();
        testDirectoryNgo.setId(1);
        testDirectoryNgo.setOrgName("Directory NGO");
        testDirectoryNgo.setFocusArea("Criminal");
        testDirectoryNgo.setCity("New York");
        testDirectoryNgo.setLatitude(40.7128);
        testDirectoryNgo.setLongitude(-74.0060);
        testDirectoryNgo.setLanguage("English");
        testDirectoryNgo.setVerified(true);

        testMatch = new Match();
        testMatch.setId(1);
        testMatch.setCaseObj(testCase);
        testMatch.setProviderType(ProviderType.LAWYER);
        testMatch.setProviderId(1);
        testMatch.setScore(85);
        testMatch.setStatus(MatchStatus.PENDING);
        testMatch.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("generateMatches Method Tests")
    class GenerateMatchesTests {

        @Test
        @DisplayName("Should throw exception when case category is null")
        void shouldThrowExceptionWhenCaseCategoryIsNull() {
            // Given
            Case invalidCase = new Case();
            invalidCase.setCategory(null);

            // When & Then
            assertThatThrownBy(() -> matchingService.generateMatches(invalidCase, 40))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Case category must be set");
        }

        @Test
        @DisplayName("Should throw exception when case category is blank")
        void shouldThrowExceptionWhenCaseCategoryIsBlank() {
            // Given
            Case invalidCase = new Case();
            invalidCase.setCategory("   ");

            // When & Then
            assertThatThrownBy(() -> matchingService.generateMatches(invalidCase, 40))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Case category must be set");
        }

        @Test
        @DisplayName("Should generate matches for registered lawyers")
        void shouldGenerateMatchesForRegisteredLawyers() {
            // Given
            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(List.of(testLawyerProfile));
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());
            when(matchRepository.findByCaseObjIdAndProviderTypeAndProviderId(
                    eq(testCase.getId()), eq(ProviderType.LAWYER), eq(1)))
                    .thenReturn(Optional.empty());
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
                Match match = invocation.getArgument(0);
                match.setId(1);
                return match;
            });

            // When
            MatchResultDTO result = matchingService.generateMatches(testCase, 40);

            // Then
            assertThat(result.results()).isNotEmpty();
            assertThat(result.results().get(0).source()).isEqualTo("REGISTERED");
            assertThat(result.results().get(0).providerType()).isEqualTo("LAWYER");
            assertThat(result.results().get(0).score()).isGreaterThan(0);
            verify(notificationService).notifyUser(anyLong(), eq("MATCH_REQUEST"), anyString(), anyString());
        }

        @Test
        @DisplayName("Should generate matches for registered NGOs")
        void shouldGenerateMatchesForRegisteredNgos() {
            // Given
            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(List.of(testNgoProfile));
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());
            when(matchRepository.findByCaseObjIdAndProviderTypeAndProviderId(
                    eq(testCase.getId()), eq(ProviderType.NGO), eq(1)))
                    .thenReturn(Optional.empty());
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
                Match match = invocation.getArgument(0);
                match.setId(1);
                return match;
            });

            // When
            MatchResultDTO result = matchingService.generateMatches(testCase, 40);

            // Then
            assertThat(result.results()).isNotEmpty();
            assertThat(result.results().get(0).source()).isEqualTo("REGISTERED");
            assertThat(result.results().get(0).providerType()).isEqualTo("NGO");
            verify(notificationService).notifyUser(anyLong(), eq("MATCH_REQUEST"), anyString(), anyString());
        }

        @Test
        @DisplayName("Should generate matches for directory lawyers")
        void shouldGenerateMatchesForDirectoryLawyers() {
            // Given
            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(List.of(testDirectoryLawyer));
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());

            // When
            MatchResultDTO result = matchingService.generateMatches(testCase, 40);

            // Then
            assertThat(result.results()).isNotEmpty();
            assertThat(result.results().get(0).source()).isEqualTo("DIRECTORY");
            assertThat(result.results().get(0).providerType()).isEqualTo("LAWYER");
            assertThat(result.results().get(0).matchId()).isNull();
            assertThat(result.results().get(0).canInteract()).isFalse();
        }

        @Test
        @DisplayName("Should generate matches for directory NGOs")
        void shouldGenerateMatchesForDirectoryNgos() {
            // Given
            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(List.of(testDirectoryNgo));

            // When
            MatchResultDTO result = matchingService.generateMatches(testCase, 40);

            // Then
            assertThat(result.results()).isNotEmpty();
            assertThat(result.results().get(0).source()).isEqualTo("DIRECTORY");
            assertThat(result.results().get(0).providerType()).isEqualTo("NGO");
        }

        @Test
        @DisplayName("Should not create duplicate matches for existing matches")
        void shouldNotCreateDuplicateMatchesForExistingMatches() {
            // Given
            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(List.of(testLawyerProfile));
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());
            when(matchRepository.findByCaseObjIdAndProviderTypeAndProviderId(
                    eq(testCase.getId()), eq(ProviderType.LAWYER), eq(1)))
                    .thenReturn(Optional.of(testMatch));

            // When
            MatchResultDTO result = matchingService.generateMatches(testCase, 40);

            // Then
            assertThat(result.results()).isNotEmpty();
            verify(matchRepository, never()).save(any(Match.class));
            verify(notificationService, never()).notifyUser(anyLong(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should skip providers with terminal status")
        void shouldSkipProvidersWithTerminalStatus() {
            // Given
            testMatch.setStatus(MatchStatus.REJECTED);
            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(List.of(testLawyerProfile));
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());
            when(matchRepository.findByCaseObjIdAndProviderTypeAndProviderId(
                    eq(testCase.getId()), eq(ProviderType.LAWYER), eq(1)))
                    .thenReturn(Optional.of(testMatch));

            // When
            MatchResultDTO result = matchingService.generateMatches(testCase, 40);

            // Then
            assertThat(result.results()).isEmpty();
        }

        @Test
        @DisplayName("Should filter results by sensitivity threshold")
        void shouldFilterResultsBySensitivityThreshold() {
            // Given
            LawyerProfile lowScoreLawyer = new LawyerProfile();
            lowScoreLawyer.setId(2);
            lowScoreLawyer.setName("Low Score Lawyer");
            lowScoreLawyer.setSpecialization("Family Law"); // Different from case category
            lowScoreLawyer.setCity("Boston"); // Different location
            lowScoreLawyer.setVerified(true);
            lowScoreLawyer.setIsAvailable(true);
            lowScoreLawyer.setUser(testUser);

            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(List.of(testLawyerProfile, lowScoreLawyer));
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());
            when(matchRepository.findByCaseObjIdAndProviderTypeAndProviderId(anyInt(), any(ProviderType.class), anyInt()))
                    .thenReturn(Optional.empty());
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
                Match match = invocation.getArgument(0);
                match.setId(1);
                return match;
            });

            // When - High sensitivity threshold
            MatchResultDTO result = matchingService.generateMatches(testCase, 80);

            // Then - Only high-scoring matches should pass
            assertThat(result.results()).allMatch(r -> r.score() >= 80);
        }

        @Test
        @DisplayName("Should use default sensitivity of 40 when null")
        void shouldUseDefaultSensitivityWhenNull() {
            // Given
            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(List.of(testLawyerProfile));
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());
            when(matchRepository.findByCaseObjIdAndProviderTypeAndProviderId(anyInt(), any(ProviderType.class), anyInt()))
                    .thenReturn(Optional.empty());
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
                Match match = invocation.getArgument(0);
                match.setId(1);
                return match;
            });

            // When
            MatchResultDTO result = matchingService.generateMatches(testCase, null);

            // Then
            assertThat(result.results()).allMatch(r -> r.score() >= 40);
        }

        @Test
        @DisplayName("Should limit results to MAX_MATCH_RESULTS (25)")
        void shouldLimitResultsToMaxMatchResults() {
            // Given
            List<LawyerProfile> manyLawyers = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                LawyerProfile lawyer = new LawyerProfile();
                lawyer.setId(i);
                lawyer.setName("Lawyer " + i);
                lawyer.setSpecialization("Criminal Law");
                lawyer.setCity("New York");
                lawyer.setVerified(true);
                lawyer.setIsAvailable(true);
                lawyer.setUser(testUser);
                manyLawyers.add(lawyer);
            }

            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(manyLawyers);
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());
            when(matchRepository.findByCaseObjIdAndProviderTypeAndProviderId(anyInt(), any(ProviderType.class), anyInt()))
                    .thenReturn(Optional.empty());
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
                Match match = invocation.getArgument(0);
                match.setId(1);
                return match;
            });

            // When
            MatchResultDTO result = matchingService.generateMatches(testCase, 0);

            // Then
            assertThat(result.results().size()).isLessThanOrEqualTo(25);
        }

        @Test
        @DisplayName("Should sort results by score descending")
        void shouldSortResultsByScoreDescending() {
            // Given
            LawyerProfile highScoreLawyer = new LawyerProfile();
            highScoreLawyer.setId(2);
            highScoreLawyer.setName("High Score Lawyer");
            highScoreLawyer.setSpecialization("Criminal Law");
            highScoreLawyer.setCity("New York");
            highScoreLawyer.setLatitude(40.7128);
            highScoreLawyer.setLongitude(-74.0060);
            highScoreLawyer.setVerified(true);
            highScoreLawyer.setIsAvailable(true);
            highScoreLawyer.setUser(testUser);

            LawyerProfile lowScoreLawyer = new LawyerProfile();
            lowScoreLawyer.setId(3);
            lowScoreLawyer.setName("Low Score Lawyer");
            lowScoreLawyer.setSpecialization("Family Law");
            lowScoreLawyer.setCity("Boston");
            lowScoreLawyer.setVerified(true);
            lowScoreLawyer.setIsAvailable(true);
            lowScoreLawyer.setUser(testUser);

            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(List.of(lowScoreLawyer, highScoreLawyer));
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());
            when(matchRepository.findByCaseObjIdAndProviderTypeAndProviderId(anyInt(), any(ProviderType.class), anyInt()))
                    .thenReturn(Optional.empty());
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
                Match match = invocation.getArgument(0);
                match.setId(1);
                return match;
            });

            // When
            MatchResultDTO result = matchingService.generateMatches(testCase, 0);

            // Then
            assertThat(result.results().size()).isGreaterThanOrEqualTo(2);
            // Verify scores are in descending order
            for (int i = 0; i < result.results().size() - 1; i++) {
                assertThat(result.results().get(i).score())
                        .isGreaterThanOrEqualTo(result.results().get(i + 1).score());
            }
        }

        @Test
        @DisplayName("Should skip providers with score <= 0")
        void shouldSkipProvidersWithScoreLessThanOrEqualToZero() {
            // Given
            LawyerProfile noMatchLawyer = new LawyerProfile();
            noMatchLawyer.setId(2);
            noMatchLawyer.setName("No Match Lawyer");
            noMatchLawyer.setSpecialization("Tax Law"); // Completely different
            noMatchLawyer.setCity("Los Angeles"); // Far away
            noMatchLawyer.setVerified(false);
            noMatchLawyer.setIsAvailable(true);
            noMatchLawyer.setUser(testUser);

            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(List.of(noMatchLawyer));
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());

            // When
            MatchResultDTO result = matchingService.generateMatches(testCase, 0);

            // Then
            assertThat(result.results()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getProviderDashboardStats Method Tests")
    class GetProviderDashboardStatsTests {

        @Test
        @DisplayName("Should return dashboard stats for lawyer")
        void shouldReturnDashboardStatsForLawyer() {
            // Given
            when(userRepository.findByEmail("lawyer@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(lawyerProfileRepository.findByUser(testUser))
                    .thenReturn(Optional.of(testLawyerProfile));
            when(matchRepository.countByProviderTypeAndProviderIdAndStatus(
                    eq(ProviderType.LAWYER), eq(1), eq(MatchStatus.CITIZEN_ACCEPTED)))
                    .thenReturn(5L);
            when(matchRepository.countByProviderTypeAndProviderIdAndStatus(
                    eq(ProviderType.LAWYER), eq(1), eq(MatchStatus.PROVIDER_CONFIRMED)))
                    .thenReturn(3L);
            when(matchRepository.findTop5ByProviderTypeAndProviderIdAndStatusOrderByCreatedAtDesc(
                    eq(ProviderType.LAWYER), eq(1), eq(MatchStatus.CITIZEN_ACCEPTED), any(PageRequest.class)))
                    .thenReturn(List.of(testMatch));
            when(matchRepository.findTop5ByProviderTypeAndProviderIdAndStatusOrderByCreatedAtDesc(
                    eq(ProviderType.LAWYER), eq(1), eq(MatchStatus.PROVIDER_CONFIRMED), any(PageRequest.class)))
                    .thenReturn(List.of(testMatch));

            // When
            ProviderDashboardStatsDTO result = matchingService.getProviderDashboardStats("lawyer@example.com");

            // Then
            assertThat(result.getMatchRequestsCount()).isEqualTo(5L);
            assertThat(result.getAssignedCasesCount()).isEqualTo(3L);
            assertThat(result.getRecentMatchRequests()).isNotEmpty();
            assertThat(result.getRecentAssignedCases()).isNotEmpty();
        }

        @Test
        @DisplayName("Should return dashboard stats for NGO")
        void shouldReturnDashboardStatsForNgo() {
            // Given
            User ngoUser = new User();
            ngoUser.setId(2);
            ngoUser.setEmail("ngo@example.com");
            ngoUser.setRole(Role.NGO);

            when(userRepository.findByEmail("ngo@example.com"))
                    .thenReturn(Optional.of(ngoUser));
            when(ngoProfileRepository.findByUser(ngoUser))
                    .thenReturn(Optional.of(testNgoProfile));
            when(matchRepository.countByProviderTypeAndProviderIdAndStatus(
                    eq(ProviderType.NGO), eq(1), eq(MatchStatus.CITIZEN_ACCEPTED)))
                    .thenReturn(7L);
            when(matchRepository.countByProviderTypeAndProviderIdAndStatus(
                    eq(ProviderType.NGO), eq(1), eq(MatchStatus.PROVIDER_CONFIRMED)))
                    .thenReturn(4L);
            when(matchRepository.findTop5ByProviderTypeAndProviderIdAndStatusOrderByCreatedAtDesc(
                    eq(ProviderType.NGO), eq(1), eq(MatchStatus.CITIZEN_ACCEPTED), any(PageRequest.class)))
                    .thenReturn(List.of(testMatch));
            when(matchRepository.findTop5ByProviderTypeAndProviderIdAndStatusOrderByCreatedAtDesc(
                    eq(ProviderType.NGO), eq(1), eq(MatchStatus.PROVIDER_CONFIRMED), any(PageRequest.class)))
                    .thenReturn(List.of(testMatch));

            // When
            ProviderDashboardStatsDTO result = matchingService.getProviderDashboardStats("ngo@example.com");

            // Then
            assertThat(result.getMatchRequestsCount()).isEqualTo(7L);
            assertThat(result.getAssignedCasesCount()).isEqualTo(4L);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findByEmail("unknown@example.com"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> matchingService.getProviderDashboardStats("unknown@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should throw exception when lawyer profile not found")
        void shouldThrowExceptionWhenLawyerProfileNotFound() {
            // Given
            when(userRepository.findByEmail("lawyer@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(lawyerProfileRepository.findByUser(testUser))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> matchingService.getProviderDashboardStats("lawyer@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Lawyer profile not found");
        }

        @Test
        @DisplayName("Should throw exception when NGO profile not found")
        void shouldThrowExceptionWhenNgoProfileNotFound() {
            // Given
            User ngoUser = new User();
            ngoUser.setRole(Role.NGO);
            when(userRepository.findByEmail("ngo@example.com"))
                    .thenReturn(Optional.of(ngoUser));
            when(ngoProfileRepository.findByUser(ngoUser))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> matchingService.getProviderDashboardStats("ngo@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("NGO profile not found");
        }

        @Test
        @DisplayName("Should throw exception when user is not a provider")
        void shouldThrowExceptionWhenUserIsNotAProvider() {
            // Given
            User citizenUser = new User();
            citizenUser.setRole(Role.CITIZEN);
            when(userRepository.findByEmail("citizen@example.com"))
                    .thenReturn(Optional.of(citizenUser));

            // When & Then
            assertThatThrownBy(() -> matchingService.getProviderDashboardStats("citizen@example.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User is not a provider");
        }
    }

    @Nested
    @DisplayName("Scoring Method Tests")
    class ScoringTests {

        @Test
        @DisplayName("Should calculate high score for perfect match")
        void shouldCalculateHighScoreForPerfectMatch() {
            // Given
            Case perfectCase = new Case();
            perfectCase.setId(1);
            perfectCase.setCategory("CRIMINAL");
            perfectCase.setDescription("Criminal case");
            perfectCase.setLocation("New York");
            perfectCase.setCity("New York");
            perfectCase.setLatitude(40.7128);
            perfectCase.setLongitude(-74.0060);
            perfectCase.setPreferredLanguage("English");

            LawyerProfile perfectLawyer = new LawyerProfile();
            perfectLawyer.setId(1);
            perfectLawyer.setName("Perfect Lawyer");
            perfectLawyer.setSpecialization("Criminal Law");
            perfectLawyer.setExpertise("Criminal Defense");
            perfectLawyer.setBio("Criminal case expert");
            perfectLawyer.setCity("New York");
            perfectLawyer.setLatitude(40.7128);
            perfectLawyer.setLongitude(-74.0060);
            perfectLawyer.setLanguage("English");
            perfectLawyer.setVerified(true);
            perfectLawyer.setIsAvailable(true);
            perfectLawyer.setUser(testUser);

            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(List.of(perfectLawyer));
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());
            when(matchRepository.findByCaseObjIdAndProviderTypeAndProviderId(anyInt(), any(ProviderType.class), anyInt()))
                    .thenReturn(Optional.empty());
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
                Match match = invocation.getArgument(0);
                match.setId(1);
                return match;
            });

            // When
            MatchResultDTO result = matchingService.generateMatches(perfectCase, 0);

            // Then
            assertThat(result.results()).isNotEmpty();
            assertThat(result.results().get(0).score()).isGreaterThan(80);
        }

        @Test
        @DisplayName("Should handle null location coordinates")
        void shouldHandleNullLocationCoordinates() {
            // Given
            Case caseWithoutCoords = new Case();
            caseWithoutCoords.setId(1);
            caseWithoutCoords.setCategory("CRIMINAL");
            caseWithoutCoords.setLocation("New York");
            caseWithoutCoords.setCity("New York");
            caseWithoutCoords.setLatitude(null);
            caseWithoutCoords.setLongitude(null);

            LawyerProfile lawyerWithoutCoords = new LawyerProfile();
            lawyerWithoutCoords.setId(1);
            lawyerWithoutCoords.setName("Lawyer Without Coords");
            lawyerWithoutCoords.setSpecialization("Criminal Law");
            lawyerWithoutCoords.setCity("New York");
            lawyerWithoutCoords.setLatitude(null);
            lawyerWithoutCoords.setLongitude(null);
            lawyerWithoutCoords.setVerified(true);
            lawyerWithoutCoords.setIsAvailable(true);
            lawyerWithoutCoords.setUser(testUser);

            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(List.of(lawyerWithoutCoords));
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());
            when(matchRepository.findByCaseObjIdAndProviderTypeAndProviderId(anyInt(), any(ProviderType.class), anyInt()))
                    .thenReturn(Optional.empty());
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
                Match match = invocation.getArgument(0);
                match.setId(1);
                return match;
            });

            // When
            MatchResultDTO result = matchingService.generateMatches(caseWithoutCoords, 0);

            // Then
            assertThat(result.results()).isNotEmpty();
            // Should still score based on city match
        }

        @Test
        @DisplayName("Should handle null language")
        void shouldHandleNullLanguage() {
            // Given
            Case caseWithoutLang = new Case();
            caseWithoutLang.setId(1);
            caseWithoutLang.setCategory("CRIMINAL");
            caseWithoutLang.setPreferredLanguage(null);

            LawyerProfile lawyerWithoutLang = new LawyerProfile();
            lawyerWithoutLang.setId(1);
            lawyerWithoutLang.setName("Lawyer Without Lang");
            lawyerWithoutLang.setSpecialization("Criminal Law");
            lawyerWithoutLang.setLanguage(null);
            lawyerWithoutLang.setVerified(true);
            lawyerWithoutLang.setIsAvailable(true);
            lawyerWithoutLang.setUser(testUser);

            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(List.of(lawyerWithoutLang));
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());
            when(matchRepository.findByCaseObjIdAndProviderTypeAndProviderId(anyInt(), any(ProviderType.class), anyInt()))
                    .thenReturn(Optional.empty());
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
                Match match = invocation.getArgument(0);
                match.setId(1);
                return match;
            });

            // When
            MatchResultDTO result = matchingService.generateMatches(caseWithoutLang, 0);

            // Then
            assertThat(result.results()).isNotEmpty();
            // Should still score based on other factors
        }

        @Test
        @DisplayName("Should calculate distance-based location score")
        void shouldCalculateDistanceBasedLocationScore() {
            // Given
            Case caseInNY = new Case();
            caseInNY.setId(1);
            caseInNY.setCategory("CRIMINAL");
            caseInNY.setLatitude(40.7128); // New York
            caseInNY.setLongitude(-74.0060);

            // Close lawyer (within 50km)
            LawyerProfile closeLawyer = new LawyerProfile();
            closeLawyer.setId(1);
            closeLawyer.setName("Close Lawyer");
            closeLawyer.setSpecialization("Criminal Law");
            closeLawyer.setLatitude(40.7580); // Close to NY
            closeLawyer.setLongitude(-73.9855);
            closeLawyer.setVerified(true);
            closeLawyer.setIsAvailable(true);
            closeLawyer.setUser(testUser);

            // Far lawyer (beyond 1000km)
            LawyerProfile farLawyer = new LawyerProfile();
            farLawyer.setId(2);
            farLawyer.setName("Far Lawyer");
            farLawyer.setSpecialization("Criminal Law");
            farLawyer.setLatitude(34.0522); // Los Angeles
            farLawyer.setLongitude(-118.2437);
            farLawyer.setVerified(true);
            farLawyer.setIsAvailable(true);
            farLawyer.setUser(testUser);

            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(List.of(closeLawyer, farLawyer));
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());
            when(matchRepository.findByCaseObjIdAndProviderTypeAndProviderId(anyInt(), any(ProviderType.class), anyInt()))
                    .thenReturn(Optional.empty());
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
                Match match = invocation.getArgument(0);
                match.setId(1);
                return match;
            });

            // When
            MatchResultDTO result = matchingService.generateMatches(caseInNY, 0);

            // Then
            assertThat(result.results()).isNotEmpty();
            // Close lawyer should have higher score than far lawyer
            assertThat(result.results().get(0).name()).isEqualTo("Close Lawyer");
        }

        @Test
        @DisplayName("Should handle multiple languages")
        void shouldHandleMultipleLanguages() {
            // Given
            Case caseWithLang = new Case();
            caseWithLang.setId(1);
            caseWithLang.setCategory("CRIMINAL");
            caseWithLang.setPreferredLanguage("English, Spanish");

            LawyerProfile lawyerWithLang = new LawyerProfile();
            lawyerWithLang.setId(1);
            lawyerWithLang.setName("Lawyer With Lang");
            lawyerWithLang.setSpecialization("Criminal Law");
            lawyerWithLang.setLanguage("English, French, Spanish");
            lawyerWithLang.setVerified(true);
            lawyerWithLang.setIsAvailable(true);
            lawyerWithLang.setUser(testUser);

            when(lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(List.of(lawyerWithLang));
            when(ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue())
                    .thenReturn(new ArrayList<>());
            when(directoryLawyerRepository.findAll()).thenReturn(new ArrayList<>());
            when(directoryNgoRepository.findAll()).thenReturn(new ArrayList<>());
            when(matchRepository.findByCaseObjIdAndProviderTypeAndProviderId(anyInt(), any(ProviderType.class), anyInt()))
                    .thenReturn(Optional.empty());
            when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
                Match match = invocation.getArgument(0);
                match.setId(1);
                return match;
            });

            // When
            MatchResultDTO result = matchingService.generateMatches(caseWithLang, 0);

            // Then
            assertThat(result.results()).isNotEmpty();
            // Should match on language
        }
    }
}
