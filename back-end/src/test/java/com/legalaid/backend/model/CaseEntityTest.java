package com.legalaid.backend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Case Entity Tests")
class CaseEntityTest {

    private Case testCase;
    private User testUser;
    private EvidenceFile testEvidenceFile;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");
        testUser.setRole(Role.CITIZEN);

        testCase = new Case();
        testEvidenceFile = new EvidenceFile();
        testEvidenceFile.setId(1);
        testEvidenceFile.setFileName("test.pdf");
        testEvidenceFile.setFileType("application/pdf");
        testEvidenceFile.setData("test data".getBytes());
        testEvidenceFile.setCaseObj(testCase);
    }

    @Nested
    @DisplayName("Getters and Setters Tests")
    class GettersAndSettersTests {

        @Test
        @DisplayName("Should set and get basic properties")
        void shouldSetAndGetBasicProperties() {
            // Given
            String title = "Legal Aid Case";
            String description = "Description of the case";
            String caseNumber = "CASE-2026-001";
            CaseType caseType = CaseType.CS;
            CaseStatus status = CaseStatus.OPEN;
            CasePriority priority = CasePriority.HIGH;

            // When
            testCase.setTitle(title);
            testCase.setDescription(description);
            testCase.setCaseNumber(caseNumber);
            testCase.setCaseType(caseType);
            testCase.setStatus(status);
            testCase.setPriority(priority);

            // Then
            assertThat(testCase.getTitle()).isEqualTo(title);
            assertThat(testCase.getDescription()).isEqualTo(description);
            assertThat(testCase.getCaseNumber()).isEqualTo(caseNumber);
            assertThat(testCase.getCaseType()).isEqualTo(caseType);
            assertThat(testCase.getStatus()).isEqualTo(status);
            assertThat(testCase.getPriority()).isEqualTo(priority);
        }

        @Test
        @DisplayName("Should set and get user relationships")
        void shouldSetAndGetUserRelationships() {
            // Given
            User assignedTo = new User();
            assignedTo.setId(2);
            assignedTo.setEmail("lawyer@example.com");
            assignedTo.setRole(Role.LAWYER);

            User closedBy = new User();
            closedBy.setId(3);
            closedBy.setEmail("admin@example.com");
            closedBy.setRole(Role.ADMIN);

            // When
            testCase.setCreatedBy(testUser);
            testCase.setAssignedTo(assignedTo);
            testCase.setClosedBy(closedBy);

            // Then
            assertThat(testCase.getCreatedBy()).isEqualTo(testUser);
            assertThat(testCase.getAssignedTo()).isEqualTo(assignedTo);
            assertThat(testCase.getClosedBy()).isEqualTo(closedBy);
        }

        @Test
        @DisplayName("Should set and get location properties")
        void shouldSetAndGetLocationProperties() {
            // Given
            String location = "New York, NY";
            String city = "New York";
            Double latitude = 40.7128;
            Double longitude = -74.0060;

            // When
            testCase.setLocation(location);
            testCase.setCity(city);
            testCase.setLatitude(latitude);
            testCase.setLongitude(longitude);

            // Then
            assertThat(testCase.getLocation()).isEqualTo(location);
            assertThat(testCase.getCity()).isEqualTo(city);
            assertThat(testCase.getLatitude()).isEqualTo(latitude);
            assertThat(testCase.getLongitude()).isEqualTo(longitude);
        }

        @Test
        @DisplayName("Should set and get case details")
        void shouldSetAndGetCaseDetails() {
            // Given
            Boolean isUrgent = true;
            String contactInfo = "Contact: +1-555-0123";
            String category = "Family Law";
            List<String> expertiseTags = Arrays.asList("divorce", "custody");
            String preferredLanguage = "English";
            String parties = "John Doe vs Jane Doe";
            String closureReason = "Settled out of court";

            // When
            testCase.setIsUrgent(isUrgent);
            testCase.setContactInfo(contactInfo);
            testCase.setCategory(category);
            testCase.setExpertiseTags(expertiseTags);
            testCase.setPreferredLanguage(preferredLanguage);
            testCase.setParties(parties);
            testCase.setClosureReason(closureReason);

            // Then
            assertThat(testCase.getIsUrgent()).isEqualTo(isUrgent);
            assertThat(testCase.getContactInfo()).isEqualTo(contactInfo);
            assertThat(testCase.getCategory()).isEqualTo(category);
            assertThat(testCase.getExpertiseTags()).isEqualTo(expertiseTags);
            assertThat(testCase.getPreferredLanguage()).isEqualTo(preferredLanguage);
            assertThat(testCase.getParties()).isEqualTo(parties);
            assertThat(testCase.getClosureReason()).isEqualTo(closureReason);
        }

        @Test
        @DisplayName("Should set and get timestamp properties")
        void shouldSetAndGetTimestampProperties() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime createdAt = now.minusDays(1);
            LocalDateTime updatedAt = now;
            LocalDateTime assignedAt = now.minusHours(12);
            LocalDateTime closedAt = now.minusHours(1);

            // When
            testCase.setCreatedAt(createdAt);
            testCase.setUpdatedAt(updatedAt);
            testCase.setAssignedAt(assignedAt);
            testCase.setClosedAt(closedAt);

            // Then
            assertThat(testCase.getCreatedAt()).isEqualTo(createdAt);
            assertThat(testCase.getUpdatedAt()).isEqualTo(updatedAt);
            assertThat(testCase.getAssignedAt()).isEqualTo(assignedAt);
            assertThat(testCase.getClosedAt()).isEqualTo(closedAt);
        }

        @Test
        @DisplayName("Should set and get evidence files")
        void shouldSetAndGetEvidenceFiles() {
            // Given
            List<EvidenceFile> evidenceFiles = Arrays.asList(testEvidenceFile);

            // When
            testCase.setEvidenceFiles(evidenceFiles);

            // Then
            assertThat(testCase.getEvidenceFiles()).isNotNull();
            assertThat(testCase.getEvidenceFiles()).hasSize(1);
            assertThat(testCase.getEvidenceFiles().get(0)).isEqualTo(testEvidenceFile);
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have default status as OPEN")
        void shouldHaveDefaultStatusAsOpen() {
            // Given & When
            Case newCase = new Case();

            // Then
            assertThat(newCase.getStatus()).isEqualTo(CaseStatus.OPEN);
        }

        @Test
        @DisplayName("Should have default priority as MEDIUM")
        void shouldHaveDefaultPriorityAsMedium() {
            // Given & When
            Case newCase = new Case();

            // Then
            assertThat(newCase.getPriority()).isEqualTo(CasePriority.MEDIUM);
        }

        @Test
        @DisplayName("Should have default isUrgent as false")
        void shouldHaveDefaultIsUrgentAsFalse() {
            // Given & When
            Case newCase = new Case();

            // Then
            assertThat(newCase.getIsUrgent()).isFalse();
        }
    }

    @Nested
    @DisplayName("Lifecycle Methods Tests")
    class LifecycleMethodsTests {

        @Test
        @DisplayName("@PrePersist should set createdAt and updatedAt when createdAt is null")
        void prePersistShouldSetTimestampsWhenCreatedAtIsNull() {
            // Given
            testCase.setCreatedAt(null);
            testCase.setUpdatedAt(null);

            // When
            testCase.prePersist();

            // Then
            assertThat(testCase.getCreatedAt()).isNotNull();
            assertThat(testCase.getUpdatedAt()).isNotNull();
            assertThat(testCase.getUpdatedAt()).isEqualTo(testCase.getCreatedAt());
        }

        @Test
        @DisplayName("@PrePersist should not override existing createdAt but update updatedAt")
        void prePersistShouldNotOverrideExistingCreatedAt() {
            // Given
            LocalDateTime existingCreatedAt = LocalDateTime.now().minusDays(1);
            testCase.setCreatedAt(existingCreatedAt);
            testCase.setUpdatedAt(null);

            // When
            testCase.prePersist();

            // Then
            assertThat(testCase.getCreatedAt()).isEqualTo(existingCreatedAt);
            assertThat(testCase.getUpdatedAt()).isEqualTo(existingCreatedAt);
        }

        @Test
        @DisplayName("@PreUpdate should update updatedAt timestamp")
        void preUpdateShouldUpdateTimestamp() {
            // Given
            LocalDateTime oldUpdatedAt = LocalDateTime.now().minusHours(1);
            testCase.setUpdatedAt(oldUpdatedAt);

            // When
            testCase.preUpdate();

            // Then
            assertThat(testCase.getUpdatedAt()).isAfter(oldUpdatedAt);
            assertThat(testCase.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create case with no-args constructor")
        void shouldCreateCaseWithNoArgsConstructor() {
            // Given & When
            Case newCase = new Case();

            // Then
            assertThat(newCase).isNotNull();
            assertThat(newCase.getId()).isNull();
            assertThat(newCase.getStatus()).isEqualTo(CaseStatus.OPEN);
            assertThat(newCase.getPriority()).isEqualTo(CasePriority.MEDIUM);
            assertThat(newCase.getIsUrgent()).isFalse();
        }

        @Test
        @DisplayName("Should create case with all-args constructor")
        void shouldCreateCaseWithAllArgsConstructor() {
            // Given
            Integer id = 1;
            String caseNumber = "CASE-2026-001";
            CaseType caseType = CaseType.CS;
            String title = "Test Case";
            String description = "Test Description";
            CaseStatus status = CaseStatus.IN_PROGRESS;
            CasePriority priority = CasePriority.HIGH;
            String location = "Test Location";
            Boolean isUrgent = true;
            String contactInfo = "Test Contact";
            String category = "Test Category";
            List<String> expertiseTags = Arrays.asList("test", "legal");
            String preferredLanguage = "English";
            String parties = "Test vs Test";
            List<EvidenceFile> evidenceFiles = Arrays.asList(testEvidenceFile);
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
            LocalDateTime updatedAt = LocalDateTime.now();
            String city = "Test City";
            Double latitude = 40.0;
            Double longitude = -70.0;
            LocalDateTime assignedAt = LocalDateTime.now().minusHours(12);
            LocalDateTime closedAt = LocalDateTime.now().minusHours(1);
            String closureReason = "Test closure";

            // When
            Case newCase = new Case(
                id, caseNumber, caseType, title, description, status, priority,
                testUser, testUser, location, isUrgent, contactInfo, category,
                expertiseTags, preferredLanguage, parties, evidenceFiles,
                createdAt, updatedAt, city, latitude, longitude,
                assignedAt, closedAt, testUser, closureReason
            );

            // Then
            assertThat(newCase.getId()).isEqualTo(id);
            assertThat(newCase.getCaseNumber()).isEqualTo(caseNumber);
            assertThat(newCase.getCaseType()).isEqualTo(caseType);
            assertThat(newCase.getTitle()).isEqualTo(title);
            assertThat(newCase.getDescription()).isEqualTo(description);
            assertThat(newCase.getStatus()).isEqualTo(status);
            assertThat(newCase.getPriority()).isEqualTo(priority);
            assertThat(newCase.getCreatedBy()).isEqualTo(testUser);
            assertThat(newCase.getAssignedTo()).isEqualTo(testUser);
            assertThat(newCase.getLocation()).isEqualTo(location);
            assertThat(newCase.getIsUrgent()).isEqualTo(isUrgent);
            assertThat(newCase.getContactInfo()).isEqualTo(contactInfo);
            assertThat(newCase.getCategory()).isEqualTo(category);
            assertThat(newCase.getExpertiseTags()).isEqualTo(expertiseTags);
            assertThat(newCase.getPreferredLanguage()).isEqualTo(preferredLanguage);
            assertThat(newCase.getParties()).isEqualTo(parties);
            assertThat(newCase.getEvidenceFiles()).isEqualTo(evidenceFiles);
            assertThat(newCase.getCreatedAt()).isEqualTo(createdAt);
            assertThat(newCase.getUpdatedAt()).isEqualTo(updatedAt);
            assertThat(newCase.getCity()).isEqualTo(city);
            assertThat(newCase.getLatitude()).isEqualTo(latitude);
            assertThat(newCase.getLongitude()).isEqualTo(longitude);
            assertThat(newCase.getAssignedAt()).isEqualTo(assignedAt);
            assertThat(newCase.getClosedAt()).isEqualTo(closedAt);
            assertThat(newCase.getClosedBy()).isEqualTo(testUser);
            assertThat(newCase.getClosureReason()).isEqualTo(closureReason);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null values gracefully")
        void shouldHandleNullValuesGracefully() {
            // Given & When
            testCase.setTitle(null);
            testCase.setDescription(null);
            testCase.setLocation(null);
            testCase.setExpertiseTags(null);
            testCase.setEvidenceFiles(null);

            // Then
            assertThat(testCase.getTitle()).isNull();
            assertThat(testCase.getDescription()).isNull();
            assertThat(testCase.getLocation()).isNull();
            assertThat(testCase.getExpertiseTags()).isNull();
            assertThat(testCase.getEvidenceFiles()).isNull();
        }

        @Test
        @DisplayName("Should handle empty lists")
        void shouldHandleEmptyLists() {
            // Given
            List<String> emptyTags = Arrays.asList();
            List<EvidenceFile> emptyFiles = Arrays.asList();

            // When
            testCase.setExpertiseTags(emptyTags);
            testCase.setEvidenceFiles(emptyFiles);

            // Then
            assertThat(testCase.getExpertiseTags()).isEmpty();
            assertThat(testCase.getEvidenceFiles()).isEmpty();
        }

        @Test
        @DisplayName("Should handle extreme coordinate values")
        void shouldHandleExtremeCoordinateValues() {
            // Given
            Double maxLatitude = 90.0;
            Double minLatitude = -90.0;
            Double maxLongitude = 180.0;
            Double minLongitude = -180.0;

            // When & Then
            testCase.setLatitude(maxLatitude);
            testCase.setLongitude(maxLongitude);
            assertThat(testCase.getLatitude()).isEqualTo(maxLatitude);
            assertThat(testCase.getLongitude()).isEqualTo(maxLongitude);

            testCase.setLatitude(minLatitude);
            testCase.setLongitude(minLongitude);
            assertThat(testCase.getLatitude()).isEqualTo(minLatitude);
            assertThat(testCase.getLongitude()).isEqualTo(minLongitude);
        }
    }
}
