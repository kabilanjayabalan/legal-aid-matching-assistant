package com.legalaid.backend.service;

import com.legalaid.backend.dto.DirectorySearchRequest;
import com.legalaid.backend.dto.ImportSummary;
import com.legalaid.backend.dto.ProviderDetailsDTO;
import com.legalaid.backend.model.*;
import com.legalaid.backend.repository.DirectoryLawyerRepository;
import com.legalaid.backend.repository.DirectoryNgoRepository;
import com.legalaid.backend.repository.LawyerProfileRepository;
import com.legalaid.backend.repository.NGOProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Directory Service Tests")
class DirectoryServiceTest {

    @Mock
    private DirectoryLawyerRepository directoryLawyerRepository;

    @Mock
    private DirectoryNgoRepository directoryNgoRepository;

    @Mock
    private LawyerProfileRepository lawyerProfileRepository;

    @Mock
    private NGOProfileRepository ngoProfileRepository;

    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private DirectoryService directoryService;

    private DirectoryLawyer testLawyer;
    private DirectoryNgo testNgo;
    private LawyerProfile testLawyerProfile;
    private NGOProfile testNgoProfile;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("lawyer@example.com");
        testUser.setFullName("Test Lawyer");

        testLawyer = new DirectoryLawyer();
        testLawyer.setId(1);
        testLawyer.setFullName("John Doe");
        testLawyer.setBarRegistrationId("BAR123");
        testLawyer.setSpecialization("Criminal Law");
        testLawyer.setCity("New York");
        testLawyer.setEmail("john@example.com");
        testLawyer.setContactNumber("1234567890");
        testLawyer.setLatitude(40.7128);
        testLawyer.setLongitude(-74.0060);
        testLawyer.setLanguage("English");
        testLawyer.setVerified(true);
        testLawyer.setCreatedAt(LocalDateTime.now());

        testNgo = new DirectoryNgo();
        testNgo.setId(1);
        testNgo.setOrgName("Legal Aid NGO");
        testNgo.setRegistrationNumber("NGO123");
        testNgo.setFocusArea("Human Rights");
        testNgo.setCity("New York");
        testNgo.setEmail("ngo@example.com");
        testNgo.setContactNumber("9876543210");
        testNgo.setLatitude(40.7128);
        testNgo.setLongitude(-74.0060);
        testNgo.setLanguage("English");
        testNgo.setVerified(true);
        testNgo.setCreatedAt(LocalDateTime.now());

        testLawyerProfile = new LawyerProfile();
        testLawyerProfile.setId(1);
        testLawyerProfile.setName("Jane Smith");
        testLawyerProfile.setBarRegistrationNo("BAR456");
        testLawyerProfile.setSpecialization("Family Law");
        testLawyerProfile.setCity("Boston");
        testLawyerProfile.setLocation("Boston, MA");
        testLawyerProfile.setContactInfo("Contact: 555-1234");
        testLawyerProfile.setUser(testUser);
        testLawyerProfile.setVerified(true);
        testLawyerProfile.setLanguage("English, Spanish");
        testLawyerProfile.setLatitude(42.3601);
        testLawyerProfile.setLongitude(-71.0589);
        testLawyerProfile.setCreatedAt(LocalDateTime.now());

        testNgoProfile = new NGOProfile();
        testNgoProfile.setId(1);
        testNgoProfile.setNgoName("Community Legal Services");
        testNgoProfile.setRegistrationNo("NGO456");
        testNgoProfile.setDescription("Legal assistance for low-income families");
        testNgoProfile.setCity("Boston");
        testNgoProfile.setLocation("Boston, MA");
        testNgoProfile.setContactInfo("Contact: 555-5678");
        testNgoProfile.setUser(testUser);
        testNgoProfile.setVerified(true);
        testNgoProfile.setLanguage("English");
        testNgoProfile.setLatitude(42.3601);
        testNgoProfile.setLongitude(-71.0589);
        testNgoProfile.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("importLawyers Method Tests")
    class ImportLawyersTests {

        @Test
        @DisplayName("Should return empty summary when lawyers list is null")
        void shouldReturnEmptySummaryWhenLawyersListIsNull() {
            // When
            ImportSummary result = directoryService.importLawyers(null, ImportMode.SKIP);

            // Then
            assertThat(result.getTotalProcessed()).isEqualTo(0);
            assertThat(result.getImportedCount()).isEqualTo(0);
            assertThat(result.getUpdatedCount()).isEqualTo(0);
            assertThat(result.getSkippedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return empty summary when lawyers list is empty")
        void shouldReturnEmptySummaryWhenLawyersListIsEmpty() {
            // When
            ImportSummary result = directoryService.importLawyers(new ArrayList<>(), ImportMode.SKIP);

            // Then
            assertThat(result.getTotalProcessed()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should skip lawyer with null barRegistrationId")
        void shouldSkipLawyerWithNullBarRegistrationId() {
            // Given
            DirectoryLawyer lawyer = new DirectoryLawyer();
            lawyer.setBarRegistrationId(null);
            lawyer.setFullName("Test Lawyer");
            List<DirectoryLawyer> lawyers = List.of(lawyer);

            // When
            ImportSummary result = directoryService.importLawyers(lawyers, ImportMode.SKIP);

            // Then
            assertThat(result.getTotalProcessed()).isEqualTo(1);
            assertThat(result.getSkippedCount()).isEqualTo(1);
            assertThat(result.getErrors()).isNotEmpty();
            assertThat(result.getErrors().get(0)).contains("null barRegistrationId");
        }

        @Test
        @DisplayName("Should import new lawyer when not exists and mode is SKIP")
        void shouldImportNewLawyerWhenNotExistsAndModeIsSkip() {
            // Given
            DirectoryLawyer lawyer = new DirectoryLawyer();
            lawyer.setBarRegistrationId("BAR999");
            lawyer.setFullName("New Lawyer");
            lawyer.setCity("Chicago");
            lawyer.setLatitude(41.8781);
            lawyer.setLongitude(-87.6298);
            List<DirectoryLawyer> lawyers = List.of(lawyer);

            when(directoryLawyerRepository.findByBarRegistrationId("BAR999")).thenReturn(null);
            when(directoryLawyerRepository.save(any(DirectoryLawyer.class))).thenReturn(lawyer);

            // When
            ImportSummary result = directoryService.importLawyers(lawyers, ImportMode.SKIP);

            // Then
            assertThat(result.getTotalProcessed()).isEqualTo(1);
            assertThat(result.getImportedCount()).isEqualTo(1);
            assertThat(result.getSkippedCount()).isEqualTo(0);
            verify(directoryLawyerRepository).save(any(DirectoryLawyer.class));
        }

        @Test
        @DisplayName("Should update existing lawyer when mode is UPDATE")
        void shouldUpdateExistingLawyerWhenModeIsUpdate() {
            // Given
            DirectoryLawyer newLawyer = new DirectoryLawyer();
            newLawyer.setBarRegistrationId("BAR123");
            newLawyer.setFullName("Updated Name");
            newLawyer.setSpecialization("New Specialization");
            newLawyer.setCity("New City");
            newLawyer.setLatitude(40.7128);
            newLawyer.setLongitude(-74.0060);
            List<DirectoryLawyer> lawyers = List.of(newLawyer);

            DirectoryLawyer existingLawyer = new DirectoryLawyer();
            existingLawyer.setId(1);
            existingLawyer.setBarRegistrationId("BAR123");
            existingLawyer.setFullName("Old Name");
            existingLawyer.setSource("EXTERNAL_IMPORT");

            when(directoryLawyerRepository.findByBarRegistrationId("BAR123")).thenReturn(existingLawyer);
            when(directoryLawyerRepository.save(any(DirectoryLawyer.class))).thenReturn(existingLawyer);

            // When
            ImportSummary result = directoryService.importLawyers(lawyers, ImportMode.UPDATE);

            // Then
            assertThat(result.getTotalProcessed()).isEqualTo(1);
            assertThat(result.getUpdatedCount()).isEqualTo(1);
            assertThat(result.getSkippedCount()).isEqualTo(0);
            verify(directoryLawyerRepository).save(existingLawyer);
        }

        @Test
        @DisplayName("Should skip duplicate when mode is CREATE")
        void shouldSkipDuplicateWhenModeIsCreate() {
            // Given
            DirectoryLawyer lawyer = new DirectoryLawyer();
            lawyer.setBarRegistrationId("BAR123");
            lawyer.setFullName("Test Lawyer");
            lawyer.setLatitude(40.7128);
            lawyer.setLongitude(-74.0060);
            List<DirectoryLawyer> lawyers = List.of(lawyer);

            DirectoryLawyer existingLawyer = new DirectoryLawyer();
            existingLawyer.setBarRegistrationId("BAR123");

            when(directoryLawyerRepository.findByBarRegistrationId("BAR123")).thenReturn(existingLawyer);

            // When
            ImportSummary result = directoryService.importLawyers(lawyers, ImportMode.CREATE);

            // Then
            assertThat(result.getTotalProcessed()).isEqualTo(1);
            assertThat(result.getSkippedCount()).isEqualTo(1);
            assertThat(result.getErrors()).isNotEmpty();
            verify(directoryLawyerRepository, never()).save(any(DirectoryLawyer.class));
        }

        @Test
        @DisplayName("Should fetch coordinates when missing")
        void shouldFetchCoordinatesWhenMissing() {
            // Given
            DirectoryLawyer lawyer = new DirectoryLawyer();
            lawyer.setBarRegistrationId("BAR999");
            lawyer.setFullName("New Lawyer");
            lawyer.setCity("Chicago");
            lawyer.setLatitude(null);
            lawyer.setLongitude(null);
            List<DirectoryLawyer> lawyers = List.of(lawyer);

            when(geocodingService.getCoordinates("Chicago")).thenReturn(Optional.of(new double[]{41.8781, -87.6298}));
            when(directoryLawyerRepository.findByBarRegistrationId("BAR999")).thenReturn(null);
            when(directoryLawyerRepository.save(any(DirectoryLawyer.class))).thenAnswer(invocation -> {
                DirectoryLawyer saved = invocation.getArgument(0);
                saved.setId(1);
                return saved;
            });

            // When
            ImportSummary result = directoryService.importLawyers(lawyers, ImportMode.SKIP);

            // Then
            assertThat(result.getImportedCount()).isEqualTo(1);
            verify(geocodingService).getCoordinates("Chicago");
        }

        @Test
        @DisplayName("Should use default SKIP mode when importMode is null")
        void shouldUseDefaultSkipModeWhenImportModeIsNull() {
            // Given
            DirectoryLawyer lawyer = new DirectoryLawyer();
            lawyer.setBarRegistrationId("BAR999");
            lawyer.setFullName("New Lawyer");
            lawyer.setLatitude(40.7128);
            lawyer.setLongitude(-74.0060);
            List<DirectoryLawyer> lawyers = List.of(lawyer);

            when(directoryLawyerRepository.findByBarRegistrationId("BAR999")).thenReturn(null);
            when(directoryLawyerRepository.save(any(DirectoryLawyer.class))).thenReturn(lawyer);

            // When
            ImportSummary result = directoryService.importLawyers(lawyers, null);

            // Then
            assertThat(result.getImportedCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("importNgos Method Tests")
    class ImportNgosTests {

        @Test
        @DisplayName("Should return empty summary when ngos list is null")
        void shouldReturnEmptySummaryWhenNgosListIsNull() {
            // When
            ImportSummary result = directoryService.importNgos(null, ImportMode.SKIP);

            // Then
            assertThat(result.getTotalProcessed()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should skip NGO with null registrationNumber")
        void shouldSkipNgoWithNullRegistrationNumber() {
            // Given
            DirectoryNgo ngo = new DirectoryNgo();
            ngo.setRegistrationNumber(null);
            ngo.setOrgName("Test NGO");
            List<DirectoryNgo> ngos = List.of(ngo);

            // When
            ImportSummary result = directoryService.importNgos(ngos, ImportMode.SKIP);

            // Then
            assertThat(result.getTotalProcessed()).isEqualTo(1);
            assertThat(result.getSkippedCount()).isEqualTo(1);
            assertThat(result.getErrors()).isNotEmpty();
        }

        @Test
        @DisplayName("Should import new NGO when not exists")
        void shouldImportNewNgoWhenNotExists() {
            // Given
            DirectoryNgo ngo = new DirectoryNgo();
            ngo.setRegistrationNumber("NGO999");
            ngo.setOrgName("New NGO");
            ngo.setLatitude(40.7128);
            ngo.setLongitude(-74.0060);
            List<DirectoryNgo> ngos = List.of(ngo);

            when(directoryNgoRepository.findByRegistrationNumber("NGO999")).thenReturn(null);
            when(directoryNgoRepository.save(any(DirectoryNgo.class))).thenReturn(ngo);

            // When
            ImportSummary result = directoryService.importNgos(ngos, ImportMode.SKIP);

            // Then
            assertThat(result.getImportedCount()).isEqualTo(1);
            verify(directoryNgoRepository).save(any(DirectoryNgo.class));
        }

        @Test
        @DisplayName("Should update existing NGO when mode is UPDATE")
        void shouldUpdateExistingNgoWhenModeIsUpdate() {
            // Given
            DirectoryNgo newNgo = new DirectoryNgo();
            newNgo.setRegistrationNumber("NGO123");
            newNgo.setOrgName("Updated NGO");
            newNgo.setFocusArea("New Focus");
            newNgo.setLatitude(40.7128);
            newNgo.setLongitude(-74.0060);
            List<DirectoryNgo> ngos = List.of(newNgo);

            DirectoryNgo existingNgo = new DirectoryNgo();
            existingNgo.setId(1);
            existingNgo.setRegistrationNumber("NGO123");
            existingNgo.setSource("EXTERNAL_IMPORT");

            when(directoryNgoRepository.findByRegistrationNumber("NGO123")).thenReturn(existingNgo);
            when(directoryNgoRepository.save(any(DirectoryNgo.class))).thenReturn(existingNgo);

            // When
            ImportSummary result = directoryService.importNgos(ngos, ImportMode.UPDATE);

            // Then
            assertThat(result.getUpdatedCount()).isEqualTo(1);
            verify(directoryNgoRepository).save(existingNgo);
        }
    }

    @Nested
    @DisplayName("searchLawyers Method Tests")
    class SearchLawyersTests {

        @Test
        @DisplayName("Should search lawyers by city and specialization")
        void shouldSearchLawyersByCityAndSpecialization() {
            // Given
            String city = "New York";
            String specialization = "Criminal Law";

            List<DirectoryLawyer> externalLawyers = new ArrayList<>(List.of(testLawyer));
            when(directoryLawyerRepository.findByCityContainingIgnoreCase(city)).thenReturn(externalLawyers);
            when(directoryLawyerRepository.findBySpecializationContainingIgnoreCase(specialization))
                    .thenReturn(new ArrayList<>(List.of(testLawyer)));

            List<LawyerProfile> internalProfiles = List.of(testLawyerProfile);
            when(lawyerProfileRepository.findByCityContainingIgnoreCase(city)).thenReturn(internalProfiles);
            when(lawyerProfileRepository.findBySpecializationContainingIgnoreCase(specialization))
                    .thenReturn(internalProfiles);

            // When
            List<DirectoryLawyer> result = directoryService.searchLawyers(city, specialization);

            // Then
            assertThat(result).isNotEmpty();
            verify(directoryLawyerRepository).findByCityContainingIgnoreCase(city);
        }

        @Test
        @DisplayName("Should return all lawyers when no filters provided")
        void shouldReturnAllLawyersWhenNoFiltersProvided() {
            // Given
            List<DirectoryLawyer> allLawyers = List.of(testLawyer);
            when(directoryLawyerRepository.findAll()).thenReturn(allLawyers);
            when(lawyerProfileRepository.findAll()).thenReturn(List.of(testLawyerProfile));

            // When
            List<DirectoryLawyer> result = directoryService.searchLawyers(null, null);

            // Then
            assertThat(result).isNotEmpty();
            verify(directoryLawyerRepository).findAll();
        }
    }

    @Nested
    @DisplayName("searchNgos Method Tests")
    class SearchNgosTests {

        @Test
        @DisplayName("Should search NGOs by city and focus area")
        void shouldSearchNgosByCityAndFocusArea() {
            // Given
            String city = "New York";
            String focusArea = "Human Rights";

            List<DirectoryNgo> externalNgos = new ArrayList<>(List.of(testNgo));
            when(directoryNgoRepository.findByCityContainingIgnoreCase(city)).thenReturn(externalNgos);
            when(directoryNgoRepository.findByFocusAreaContainingIgnoreCase(focusArea))
                    .thenReturn(new ArrayList<>(List.of(testNgo)));

            List<NGOProfile> internalProfiles = List.of(testNgoProfile);
            when(ngoProfileRepository.findByCityContainingIgnoreCase(city)).thenReturn(internalProfiles);

            // When
            List<DirectoryNgo> result = directoryService.searchNgos(city, focusArea);

            // Then
            assertThat(result).isNotEmpty();
            verify(directoryNgoRepository).findByCityContainingIgnoreCase(city);
        }
    }

    @Nested
    @DisplayName("searchDirectory Method Tests")
    class SearchDirectoryTests {

        @Test
        @DisplayName("Should search for lawyers when type is LAWYER")
        void shouldSearchForLawyersWhenTypeIsLawyer() {
            // Given
            DirectorySearchRequest request = new DirectorySearchRequest();
            request.setType("LAWYER");
            request.setQuery("John");
            request.setLocation("New York");
            request.setExpertise("Criminal");
            request.setPage(0);
            request.setSize(10);
            request.setSortBy("id");
            request.setSortDir("asc");

            Page<DirectoryLawyer> lawyerPage = new PageImpl<>(List.of(testLawyer));
            when(directoryLawyerRepository.searchLawyers(any(), any(), any(), 
                    any(), any(), any(Pageable.class))).thenReturn(lawyerPage);
            when(lawyerProfileRepository.searchLawyers(any(), any(), any(), 
                    any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(testLawyerProfile)));

            // When
            Object result = directoryService.searchDirectory(request);

            // Then
            assertThat(result).isInstanceOf(Page.class);
            @SuppressWarnings("unchecked")
            Page<DirectoryLawyer> page = (Page<DirectoryLawyer>) result;
            assertThat(page.getContent()).isNotEmpty();
        }

        @Test
        @DisplayName("Should search for NGOs when type is NGO")
        void shouldSearchForNgosWhenTypeIsNgo() {
            // Given
            DirectorySearchRequest request = new DirectorySearchRequest();
            request.setType("NGO");
            request.setQuery("Legal Aid");
            request.setPage(0);
            request.setSize(10);

            Page<DirectoryNgo> ngoPage = new PageImpl<>(List.of(testNgo));
            when(directoryNgoRepository.searchNgos(any(), any(), any(), 
                    any(), any(), any(Pageable.class))).thenReturn(ngoPage);
            when(ngoProfileRepository.searchNgos(any(), any(), any(), 
                    any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(testNgoProfile)));

            // When
            Object result = directoryService.searchDirectory(request);

            // Then
            assertThat(result).isInstanceOf(Page.class);
            @SuppressWarnings("unchecked")
            Page<DirectoryNgo> page = (Page<DirectoryNgo>) result;
            assertThat(page.getContent()).isNotEmpty();
        }

        @Test
        @DisplayName("Should search for both when type is BOTH")
        void shouldSearchForBothWhenTypeIsBoth() {
            // Given
            DirectorySearchRequest request = new DirectorySearchRequest();
            request.setType("BOTH");
            request.setPage(0);
            request.setSize(10);

            Page<DirectoryLawyer> lawyerPage = new PageImpl<>(List.of(testLawyer));
            Page<DirectoryNgo> ngoPage = new PageImpl<>(List.of(testNgo));

            when(directoryLawyerRepository.searchLawyers(any(), any(), any(), 
                    any(), any(), any(Pageable.class))).thenReturn(lawyerPage);
            when(lawyerProfileRepository.searchLawyers(any(), any(), any(), 
                    any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(testLawyerProfile)));
            when(directoryNgoRepository.searchNgos(any(), any(), any(), 
                    any(), any(), any(Pageable.class))).thenReturn(ngoPage);
            when(ngoProfileRepository.searchNgos(any(), any(), any(), 
                    any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(testNgoProfile)));

            // When
            Object result = directoryService.searchDirectory(request);

            // Then
            assertThat(result).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result;
            assertThat(map).containsKeys("lawyers", "ngos");
        }

        @Test
        @DisplayName("Should handle null type as BOTH")
        void shouldHandleNullTypeAsBoth() {
            // Given
            DirectorySearchRequest request = new DirectorySearchRequest();
            request.setType(null);
            request.setPage(0);
            request.setSize(10);

            Page<DirectoryLawyer> lawyerPage = new PageImpl<>(List.of(testLawyer));
            Page<DirectoryNgo> ngoPage = new PageImpl<>(List.of(testNgo));

            when(directoryLawyerRepository.searchLawyers(any(), any(), any(), 
                    any(), any(), any(Pageable.class))).thenReturn(lawyerPage);
            when(lawyerProfileRepository.searchLawyers(any(), any(), any(), 
                    any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(testLawyerProfile)));
            when(directoryNgoRepository.searchNgos(any(), any(), any(), 
                    any(), any(), any(Pageable.class))).thenReturn(ngoPage);
            when(ngoProfileRepository.searchNgos(any(), any(), any(), 
                    any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(testNgoProfile)));

            // When
            Object result = directoryService.searchDirectory(request);

            // Then
            assertThat(result).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("Should handle distance filtering for lawyers")
        void shouldHandleDistanceFilteringForLawyers() {
            // Given
            DirectorySearchRequest request = new DirectorySearchRequest();
            request.setType("LAWYER");
            request.setPage(0);
            request.setSize(10);
            request.setLatitude(40.7128);
            request.setLongitude(-74.0060);
            request.setRadiusKm(10.0);

            List<DirectoryLawyer> lawyers = List.of(testLawyer);
            when(directoryLawyerRepository.findAllWithinDistanceFiltered(anyDouble(), anyDouble(), 
                    anyDouble(), any(), any(), any())).thenReturn(lawyers);
            when(lawyerProfileRepository.searchLawyers(any(), any(), any(), 
                    any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(testLawyerProfile)));

            // When
            Object result = directoryService.searchDirectory(request);

            // Then
            assertThat(result).isInstanceOf(Page.class);
            verify(directoryLawyerRepository).findAllWithinDistanceFiltered(anyDouble(), anyDouble(), 
                    anyDouble(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getAllSpecializations Method Tests")
    class GetAllSpecializationsTests {

        @Test
        @DisplayName("Should return all unique specializations")
        void shouldReturnAllUniqueSpecializations() {
            // Given
            List<String> externalSpecs = List.of("Criminal Law", "Family Law");
            List<String> internalSpecs = List.of("Family Law", "Corporate Law");

            when(directoryLawyerRepository.findDistinctSpecializations()).thenReturn(externalSpecs);
            when(lawyerProfileRepository.findDistinctSpecializations()).thenReturn(internalSpecs);

            // When
            List<String> result = directoryService.getAllSpecializations();

            // Then
            assertThat(result).hasSize(3); // Criminal Law, Family Law, Corporate Law
            assertThat(result).contains("Criminal Law", "Family Law", "Corporate Law");
        }

        @Test
        @DisplayName("Should filter out null and empty specializations")
        void shouldFilterOutNullAndEmptySpecializations() {
            // Given
            List<String> externalSpecs = Arrays.asList("Criminal Law", null, "", "Family Law");
            List<String> internalSpecs = List.of("Corporate Law");

            when(directoryLawyerRepository.findDistinctSpecializations()).thenReturn(externalSpecs);
            when(lawyerProfileRepository.findDistinctSpecializations()).thenReturn(internalSpecs);

            // When
            List<String> result = directoryService.getAllSpecializations();

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).doesNotContainNull();
            assertThat(result).doesNotContain("");
        }
    }

    @Nested
    @DisplayName("getAllFocusAreas Method Tests")
    class GetAllFocusAreasTests {

        @Test
        @DisplayName("Should return all unique focus areas")
        void shouldReturnAllUniqueFocusAreas() {
            // Given
            List<String> externalFocusAreas = List.of("Human Rights", "Education");
            List<String> internalFocusAreas = List.of("Education", "Healthcare");

            when(directoryNgoRepository.findDistinctFocusAreas()).thenReturn(externalFocusAreas);
            when(ngoProfileRepository.findDistinctFocusAreas()).thenReturn(internalFocusAreas);

            // When
            List<String> result = directoryService.getAllFocusAreas();

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).contains("Human Rights", "Education", "Healthcare");
        }
    }

    @Nested
    @DisplayName("checkLawyerImports Method Tests")
    class CheckLawyerImportsTests {

        @Test
        @DisplayName("Should return MATCH status for existing lawyers")
        void shouldReturnMatchStatusForExistingLawyers() {
            // Given
            List<String> barIds = List.of("BAR123", "BAR456");
            when(directoryLawyerRepository.existsByBarRegistrationId("BAR123")).thenReturn(true);
            when(directoryLawyerRepository.existsByBarRegistrationId("BAR456")).thenReturn(false);

            // When
            List<Map<String, Object>> result = directoryService.checkLawyerImports(barIds);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).get("status")).isEqualTo("MATCH");
            assertThat(result.get(1).get("status")).isEqualTo("NEW_IMPORT");
        }

        @Test
        @DisplayName("Should return empty list when barIds is null")
        void shouldReturnEmptyListWhenBarIdsIsNull() {
            // When
            List<Map<String, Object>> result = directoryService.checkLawyerImports(null);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("checkNgoImports Method Tests")
    class CheckNgoImportsTests {

        @Test
        @DisplayName("Should return MATCH status for existing NGOs")
        void shouldReturnMatchStatusForExistingNgos() {
            // Given
            List<String> regNumbers = List.of("NGO123", "NGO456");
            when(directoryNgoRepository.existsByRegistrationNumber("NGO123")).thenReturn(true);
            when(directoryNgoRepository.existsByRegistrationNumber("NGO456")).thenReturn(false);

            // When
            List<Map<String, Object>> result = directoryService.checkNgoImports(regNumbers);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).get("status")).isEqualTo("MATCH");
            assertThat(result.get(1).get("status")).isEqualTo("NEW_IMPORT");
        }
    }

    @Nested
    @DisplayName("getLawyerDetailsById Method Tests")
    class GetLawyerDetailsByIdTests {

        @Test
        @DisplayName("Should return lawyer details from internal profile")
        void shouldReturnLawyerDetailsFromInternalProfile() {
            // Given
            when(lawyerProfileRepository.findById(1)).thenReturn(Optional.of(testLawyerProfile));

            // When
            ProviderDetailsDTO result = directoryService.getLawyerDetailsById(1);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getProviderType()).isEqualTo("LAWYER");
            assertThat(result.getSource()).isEqualTo("INTERNAL");
            assertThat(result.getName()).isEqualTo("Jane Smith");
        }

        @Test
        @DisplayName("Should return lawyer details from external directory when not in internal")
        void shouldReturnLawyerDetailsFromExternalDirectoryWhenNotInInternal() {
            // Given
            when(lawyerProfileRepository.findById(1)).thenReturn(Optional.empty());
            when(directoryLawyerRepository.findById(1)).thenReturn(Optional.of(testLawyer));

            // When
            ProviderDetailsDTO result = directoryService.getLawyerDetailsById(1);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getProviderType()).isEqualTo("LAWYER");
            assertThat(result.getSource()).isEqualTo("EXTERNAL");
            assertThat(result.getName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should return null when lawyer not found")
        void shouldReturnNullWhenLawyerNotFound() {
            // Given
            when(lawyerProfileRepository.findById(999)).thenReturn(Optional.empty());
            when(directoryLawyerRepository.findById(999)).thenReturn(Optional.empty());

            // When
            ProviderDetailsDTO result = directoryService.getLawyerDetailsById(999);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getNgoDetailsById Method Tests")
    class GetNgoDetailsByIdTests {

        @Test
        @DisplayName("Should return NGO details from internal profile")
        void shouldReturnNgoDetailsFromInternalProfile() {
            // Given
            when(ngoProfileRepository.findById(1)).thenReturn(Optional.of(testNgoProfile));

            // When
            ProviderDetailsDTO result = directoryService.getNgoDetailsById(1);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getProviderType()).isEqualTo("NGO");
            assertThat(result.getSource()).isEqualTo("INTERNAL");
        }

        @Test
        @DisplayName("Should return NGO details from external directory when not in internal")
        void shouldReturnNgoDetailsFromExternalDirectoryWhenNotInInternal() {
            // Given
            when(ngoProfileRepository.findById(1)).thenReturn(Optional.empty());
            when(directoryNgoRepository.findById(1)).thenReturn(Optional.of(testNgo));

            // When
            ProviderDetailsDTO result = directoryService.getNgoDetailsById(1);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getProviderType()).isEqualTo("NGO");
            assertThat(result.getSource()).isEqualTo("EXTERNAL");
        }

        @Test
        @DisplayName("Should return null when NGO not found")
        void shouldReturnNullWhenNgoNotFound() {
            // Given
            when(ngoProfileRepository.findById(999)).thenReturn(Optional.empty());
            when(directoryNgoRepository.findById(999)).thenReturn(Optional.empty());

            // When
            ProviderDetailsDTO result = directoryService.getNgoDetailsById(999);

            // Then
            assertThat(result).isNull();
        }
    }
}
