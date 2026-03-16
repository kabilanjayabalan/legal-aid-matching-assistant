package com.legalaid.backend.controller;

import com.legalaid.backend.model.CasePriority;
import com.legalaid.backend.model.CaseStatus;
import com.legalaid.backend.model.CaseType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Case Controller Records Tests")
class CaseControllerRecordsTest {

    @Nested
    @DisplayName("CaseCreateRequest Record Tests")
    class CaseCreateRequestTests {

        @Test
        @DisplayName("Should create CaseCreateRequest with all fields")
        void shouldCreateCaseCreateRequestWithAllFields() {
            // Given
            String title = "Test Case";
            String description = "Test Description";
            String priority = "HIGH";
            String location = "Test Location";
            String city = "Test City";
            Double latitude = 40.0;
            Double longitude = -70.0;
            String contactInfo = "test@example.com";
            Boolean isUrgent = true;
            String category = "CIVIL";
            List<String> expertiseTags = Arrays.asList("civil", "contract");
            String preferredLanguage = "English";
            String parties = "John vs Jane";
            List<String> evidenceFiles = Arrays.asList("file1.pdf", "file2.pdf");

            // When
            CaseController.CaseCreateRequest request = new CaseController.CaseCreateRequest(
                title, description, priority, location, city, latitude, longitude,
                contactInfo, isUrgent, category, expertiseTags, preferredLanguage,
                parties, evidenceFiles
            );

            // Then
            assertThat(request.title()).isEqualTo(title);
            assertThat(request.description()).isEqualTo(description);
            assertThat(request.priority()).isEqualTo(priority);
            assertThat(request.location()).isEqualTo(location);
            assertThat(request.city()).isEqualTo(city);
            assertThat(request.latitude()).isEqualTo(latitude);
            assertThat(request.longitude()).isEqualTo(longitude);
            assertThat(request.contactInfo()).isEqualTo(contactInfo);
            assertThat(request.isUrgent()).isEqualTo(isUrgent);
            assertThat(request.category()).isEqualTo(category);
            assertThat(request.expertiseTags()).isEqualTo(expertiseTags);
            assertThat(request.preferredLanguage()).isEqualTo(preferredLanguage);
            assertThat(request.parties()).isEqualTo(parties);
            assertThat(request.evidenceFiles()).isEqualTo(evidenceFiles);
        }

        @Test
        @DisplayName("Should create CaseCreateRequest with null values")
        void shouldCreateCaseCreateRequestWithNullValues() {
            // When
            CaseController.CaseCreateRequest request = new CaseController.CaseCreateRequest(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null
            );

            // Then
            assertThat(request.title()).isNull();
            assertThat(request.description()).isNull();
            assertThat(request.priority()).isNull();
            assertThat(request.location()).isNull();
            assertThat(request.city()).isNull();
            assertThat(request.latitude()).isNull();
            assertThat(request.longitude()).isNull();
            assertThat(request.contactInfo()).isNull();
            assertThat(request.isUrgent()).isNull();
            assertThat(request.category()).isNull();
            assertThat(request.expertiseTags()).isNull();
            assertThat(request.preferredLanguage()).isNull();
            assertThat(request.parties()).isNull();
            assertThat(request.evidenceFiles()).isNull();
        }

        @Test
        @DisplayName("Should handle empty lists in CaseCreateRequest")
        void shouldHandleEmptyListsInCaseCreateRequest() {
            // Given
            List<String> emptyTags = Arrays.asList();
            List<String> emptyFiles = Arrays.asList();

            // When
            CaseController.CaseCreateRequest request = new CaseController.CaseCreateRequest(
                "Title", "Description", "MEDIUM", "Location", "City", 0.0, 0.0,
                "contact", false, "CIVIL", emptyTags, "English", "Parties", emptyFiles
            );

            // Then
            assertThat(request.expertiseTags()).isEmpty();
            assertThat(request.evidenceFiles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("CaseResponse Record Tests")
    class CaseResponseTests {

        @Test
        @DisplayName("Should create CaseResponse with all fields")
        void shouldCreateCaseResponseWithAllFields() {
            // Given
            Integer id = 1;
            String caseNumber = "CS-2026-001";
            CaseType caseType = CaseType.CS;
            String title = "Test Case";
            String description = "Test Description";
            CaseStatus status = CaseStatus.OPEN;
            CasePriority priority = CasePriority.HIGH;
            String createdByEmail = "citizen@example.com";
            String assignedToEmail = "lawyer@example.com";
            String location = "Test Location";
            String city = "Test City";
            Double latitude = 40.0;
            Double longitude = -70.0;
            Boolean isUrgent = true;
            String contactInfo = "test@example.com";
            String category = "CIVIL";
            List<String> expertiseTags = Arrays.asList("civil", "contract");
            String preferredLanguage = "English";
            String parties = "John vs Jane";
            List<CaseController.EvidenceFileInfo> evidenceFiles = Arrays.asList(
                new CaseController.EvidenceFileInfo(1, "file1.pdf", "/cases/evidence/1")
            );
            LocalDateTime createdAt = LocalDateTime.now();
            LocalDateTime updatedAt = LocalDateTime.now();

            // When
            CaseController.CaseResponse response = new CaseController.CaseResponse(
                id, caseNumber, caseType, title, description, status, priority,
                createdByEmail, assignedToEmail, location, city, latitude, longitude,
                isUrgent, contactInfo, category, expertiseTags, preferredLanguage,
                parties, evidenceFiles, createdAt, updatedAt
            );

            // Then
            assertThat(response.id()).isEqualTo(id);
            assertThat(response.caseNumber()).isEqualTo(caseNumber);
            assertThat(response.caseType()).isEqualTo(caseType);
            assertThat(response.title()).isEqualTo(title);
            assertThat(response.description()).isEqualTo(description);
            assertThat(response.status()).isEqualTo(status);
            assertThat(response.priority()).isEqualTo(priority);
            assertThat(response.createdByEmail()).isEqualTo(createdByEmail);
            assertThat(response.assignedToEmail()).isEqualTo(assignedToEmail);
            assertThat(response.location()).isEqualTo(location);
            assertThat(response.city()).isEqualTo(city);
            assertThat(response.latitude()).isEqualTo(latitude);
            assertThat(response.longitude()).isEqualTo(longitude);
            assertThat(response.isUrgent()).isEqualTo(isUrgent);
            assertThat(response.contactInfo()).isEqualTo(contactInfo);
            assertThat(response.category()).isEqualTo(category);
            assertThat(response.expertiseTags()).isEqualTo(expertiseTags);
            assertThat(response.preferredLanguage()).isEqualTo(preferredLanguage);
            assertThat(response.parties()).isEqualTo(parties);
            assertThat(response.evidenceFiles()).isEqualTo(evidenceFiles);
            assertThat(response.createdAt()).isEqualTo(createdAt);
            assertThat(response.updatedAt()).isEqualTo(updatedAt);
        }

        @Test
        @DisplayName("Should create CaseResponse with null optional fields")
        void shouldCreateCaseResponseWithNullOptionalFields() {
            // When
            CaseController.CaseResponse response = new CaseController.CaseResponse(
                1, "CS-2026-001", CaseType.CS, "Title", "Description",
                CaseStatus.OPEN, CasePriority.MEDIUM, "creator@example.com", null,
                "Location", null, null, null, false, "contact", "CIVIL",
                null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
            );

            // Then
            assertThat(response.assignedToEmail()).isNull();
            assertThat(response.city()).isNull();
            assertThat(response.latitude()).isNull();
            assertThat(response.longitude()).isNull();
            assertThat(response.expertiseTags()).isNull();
            assertThat(response.preferredLanguage()).isNull();
            assertThat(response.parties()).isNull();
            assertThat(response.evidenceFiles()).isNull();
        }
    }

    @Nested
    @DisplayName("EvidenceFileInfo Record Tests")
    class EvidenceFileInfoTests {

        @Test
        @DisplayName("Should create EvidenceFileInfo with all fields")
        void shouldCreateEvidenceFileInfoWithAllFields() {
            // Given
            Integer fileId = 1;
            String fileName = "document.pdf";
            String downloadUrl = "/cases/evidence/1";

            // When
            CaseController.EvidenceFileInfo evidenceFileInfo = new CaseController.EvidenceFileInfo(
                fileId, fileName, downloadUrl
            );

            // Then
            assertThat(evidenceFileInfo.fileId()).isEqualTo(fileId);
            assertThat(evidenceFileInfo.fileName()).isEqualTo(fileName);
            assertThat(evidenceFileInfo.downloadUrl()).isEqualTo(downloadUrl);
        }

        @Test
        @DisplayName("Should create EvidenceFileInfo with null values")
        void shouldCreateEvidenceFileInfoWithNullValues() {
            // When
            CaseController.EvidenceFileInfo evidenceFileInfo = new CaseController.EvidenceFileInfo(
                null, null, null
            );

            // Then
            assertThat(evidenceFileInfo.fileId()).isNull();
            assertThat(evidenceFileInfo.fileName()).isNull();
            assertThat(evidenceFileInfo.downloadUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("UpdateCaseRequest Record Tests")
    class UpdateCaseRequestTests {

        @Test
        @DisplayName("Should create UpdateCaseRequest with all fields")
        void shouldCreateUpdateCaseRequestWithAllFields() {
            // Given
            String title = "Updated Title";
            String description = "Updated Description";
            String category = "FAMILY";
            String location = "Updated Location";
            String city = "Updated City";
            Double latitude = 41.0;
            Double longitude = -71.0;
            String contactInfo = "updated@example.com";
            Boolean isUrgent = true;
            List<String> expertiseTags = Arrays.asList("family", "divorce");
            String preferredLanguage = "Spanish";
            String parties = "Updated Parties";

            // When
            CaseController.UpdateCaseRequest request = new CaseController.UpdateCaseRequest(
                title, description, category, location, city, latitude, longitude,
                contactInfo, isUrgent, expertiseTags, preferredLanguage, parties
            );

            // Then
            assertThat(request.title()).isEqualTo(title);
            assertThat(request.description()).isEqualTo(description);
            assertThat(request.category()).isEqualTo(category);
            assertThat(request.location()).isEqualTo(location);
            assertThat(request.city()).isEqualTo(city);
            assertThat(request.latitude()).isEqualTo(latitude);
            assertThat(request.longitude()).isEqualTo(longitude);
            assertThat(request.contactInfo()).isEqualTo(contactInfo);
            assertThat(request.isUrgent()).isEqualTo(isUrgent);
            assertThat(request.expertiseTags()).isEqualTo(expertiseTags);
            assertThat(request.preferredLanguage()).isEqualTo(preferredLanguage);
            assertThat(request.parties()).isEqualTo(parties);
        }

        @Test
        @DisplayName("Should create UpdateCaseRequest with partial data")
        void shouldCreateUpdateCaseRequestWithPartialData() {
            // When
            CaseController.UpdateCaseRequest request = new CaseController.UpdateCaseRequest(
                "New Title", null, null, null, null, null, null,
                null, null, null, null, null
            );

            // Then
            assertThat(request.title()).isEqualTo("New Title");
            assertThat(request.description()).isNull();
            assertThat(request.category()).isNull();
            assertThat(request.location()).isNull();
            assertThat(request.city()).isNull();
            assertThat(request.latitude()).isNull();
            assertThat(request.longitude()).isNull();
            assertThat(request.contactInfo()).isNull();
            assertThat(request.isUrgent()).isNull();
            assertThat(request.expertiseTags()).isNull();
            assertThat(request.preferredLanguage()).isNull();
            assertThat(request.parties()).isNull();
        }
    }

    @Nested
    @DisplayName("CloseCaseRequest Record Tests")
    class CloseCaseRequestTests {

        @Test
        @DisplayName("Should create CloseCaseRequest with reason")
        void shouldCreateCloseCaseRequestWithReason() {
            // Given
            String reason = "Case resolved successfully";

            // When
            CaseController.CloseCaseRequest request = new CaseController.CloseCaseRequest(reason);

            // Then
            assertThat(request.reason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("Should create CloseCaseRequest with null reason")
        void shouldCreateCloseCaseRequestWithNullReason() {
            // When
            CaseController.CloseCaseRequest request = new CaseController.CloseCaseRequest(null);

            // Then
            assertThat(request.reason()).isNull();
        }
    }

    @Nested
    @DisplayName("CaseTimelineResponse Record Tests")
    class CaseTimelineResponseTests {

        @Test
        @DisplayName("Should create CaseTimelineResponse with all fields")
        void shouldCreateCaseTimelineResponseWithAllFields() {
            // Given
            int step = 1;
            CaseStatus status = CaseStatus.OPEN;
            String label = "Case Submitted";
            LocalDateTime timestamp = LocalDateTime.now();
            String performedBy = "citizen@example.com";
            String reason = "Initial submission";

            // When
            CaseController.CaseTimelineResponse response = new CaseController.CaseTimelineResponse(
                step, status, label, timestamp, performedBy, reason
            );

            // Then
            assertThat(response.step()).isEqualTo(step);
            assertThat(response.status()).isEqualTo(status);
            assertThat(response.label()).isEqualTo(label);
            assertThat(response.timestamp()).isEqualTo(timestamp);
            assertThat(response.performedBy()).isEqualTo(performedBy);
            assertThat(response.reason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("Should create CaseTimelineResponse with null optional fields")
        void shouldCreateCaseTimelineResponseWithNullOptionalFields() {
            // When
            CaseController.CaseTimelineResponse response = new CaseController.CaseTimelineResponse(
                2, CaseStatus.IN_PROGRESS, "Case Assigned", LocalDateTime.now(), null, null
            );

            // Then
            assertThat(response.performedBy()).isNull();
            assertThat(response.reason()).isNull();
        }
    }

    @Nested
    @DisplayName("MessageResponse Record Tests")
    class MessageResponseTests {

        @Test
        @DisplayName("Should create MessageResponse with message")
        void shouldCreateMessageResponseWithMessage() {
            // Given
            String message = "Operation successful";

            // When
            CaseController.MessageResponse response = new CaseController.MessageResponse(message);

            // Then
            assertThat(response.message()).isEqualTo(message);
        }

        @Test
        @DisplayName("Should create MessageResponse with null message")
        void shouldCreateMessageResponseWithNullMessage() {
            // When
            CaseController.MessageResponse response = new CaseController.MessageResponse(null);

            // Then
            assertThat(response.message()).isNull();
        }

        @Test
        @DisplayName("Should create MessageResponse with empty message")
        void shouldCreateMessageResponseWithEmptyMessage() {
            // When
            CaseController.MessageResponse response = new CaseController.MessageResponse("");

            // Then
            assertThat(response.message()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Record Equality and HashCode Tests")
    class RecordEqualityTests {

        @Test
        @DisplayName("Should demonstrate record equality for EvidenceFileInfo")
        void shouldDemonstrateRecordEqualityForEvidenceFileInfo() {
            // Given
            CaseController.EvidenceFileInfo fileInfo1 = new CaseController.EvidenceFileInfo(
                1, "document.pdf", "/cases/evidence/1"
            );
            CaseController.EvidenceFileInfo fileInfo2 = new CaseController.EvidenceFileInfo(
                1, "document.pdf", "/cases/evidence/1"
            );
            CaseController.EvidenceFileInfo fileInfo3 = new CaseController.EvidenceFileInfo(
                2, "document.pdf", "/cases/evidence/2"
            );

            // Then
            assertThat(fileInfo1).isEqualTo(fileInfo2);
            assertThat(fileInfo1).isNotEqualTo(fileInfo3);
            assertThat(fileInfo1.hashCode()).isEqualTo(fileInfo2.hashCode());
            assertThat(fileInfo1.hashCode()).isNotEqualTo(fileInfo3.hashCode());
        }

        @Test
        @DisplayName("Should demonstrate record toString method")
        void shouldDemonstrateRecordToStringMethod() {
            // Given
            CaseController.MessageResponse response = new CaseController.MessageResponse("Test message");

            // Then
            assertThat(response.toString()).contains("Test message");
            assertThat(response.toString()).contains("MessageResponse");
        }
    }

    @Nested
    @DisplayName("Record Immutability Tests")
    class RecordImmutabilityTests {

        @Test
        @DisplayName("Should demonstrate record immutability with lists")
        void shouldDemonstrateRecordImmutabilityWithLists() {
            // Given
            List<String> originalTags = Arrays.asList("civil", "contract");
            CaseController.UpdateCaseRequest request = new CaseController.UpdateCaseRequest(
                "Title", "Description", "CIVIL", "Location", "City", 0.0, 0.0,
                "contact", false, originalTags, "English", "Parties"
            );

            // When
            List<String> retrievedTags = request.expertiseTags();

            // Then
            assertThat(retrievedTags).isEqualTo(originalTags);
            assertThat(retrievedTags).isSameAs(originalTags); // Records don't defensive copy
        }

        @Test
        @DisplayName("Should demonstrate component accessor methods")
        void shouldDemonstrateComponentAccessorMethods() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            CaseController.CaseTimelineResponse timeline = new CaseController.CaseTimelineResponse(
                1, CaseStatus.OPEN, "Case Created", now, "user@example.com", "Initial creation"
            );

            // Then - All component methods should work
            assertThat(timeline.step()).isEqualTo(1);
            assertThat(timeline.status()).isEqualTo(CaseStatus.OPEN);
            assertThat(timeline.label()).isEqualTo("Case Created");
            assertThat(timeline.timestamp()).isEqualTo(now);
            assertThat(timeline.performedBy()).isEqualTo("user@example.com");
            assertThat(timeline.reason()).isEqualTo("Initial creation");
        }
    }
}
