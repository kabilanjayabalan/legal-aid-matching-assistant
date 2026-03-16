package com.legalaid.backend.service;

import com.legalaid.backend.model.CaseNumberSequence;
import com.legalaid.backend.model.CaseType;
import com.legalaid.backend.repository.CaseNumberSequenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Case Number Generator Service Tests")
class CaseNumberGeneratorServiceTest {

    @Mock
    private CaseNumberSequenceRepository sequenceRepository;

    @InjectMocks
    private CaseNumberGeneratorService caseNumberGeneratorService;

    private CaseNumberSequence testSequence;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testDateTime = LocalDateTime.of(2026, 1, 15, 10, 30);
        testSequence = new CaseNumberSequence();
        testSequence.setId(1);
        testSequence.setCaseType("CS");
        testSequence.setYear(2026);
        testSequence.setLastNumber(5);
    }

    @Nested
    @DisplayName("generateCaseNumber Method Tests")
    class GenerateCaseNumberTests {

        @Test
        @DisplayName("Should generate first case number when no sequence exists")
        void shouldGenerateFirstCaseNumberWhenNoSequenceExists() {
            // Given
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CS", 2026))
                .thenReturn(Optional.empty());

            CaseNumberSequence newSequence = new CaseNumberSequence();
            newSequence.setCaseType("CS");
            newSequence.setYear(2026);
            newSequence.setLastNumber(0);
            when(sequenceRepository.save(any(CaseNumberSequence.class)))
                .thenReturn(newSequence);

            // When
            String result = caseNumberGeneratorService.generateCaseNumber(CaseType.CS, testDateTime);

            // Then
            assertThat(result).isEqualTo("CS/0001/2026");
            verify(sequenceRepository).save(argThat(sequence ->
                sequence.getCaseType().equals("CS") &&
                sequence.getYear().equals(2026) &&
                sequence.getLastNumber().equals(1)
            ));
        }

        @Test
        @DisplayName("Should generate next case number when sequence exists")
        void shouldGenerateNextCaseNumberWhenSequenceExists() {
            // Given
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CS", 2026))
                .thenReturn(Optional.of(testSequence));
            when(sequenceRepository.save(testSequence)).thenReturn(testSequence);

            // When
            String result = caseNumberGeneratorService.generateCaseNumber(CaseType.CS, testDateTime);

            // Then
            assertThat(result).isEqualTo("CS/0006/2026");
            assertThat(testSequence.getLastNumber()).isEqualTo(6);
            verify(sequenceRepository).save(testSequence);
        }

        @ParameterizedTest
        @EnumSource(CaseType.class)
        @DisplayName("Should generate case numbers for all case types")
        void shouldGenerateCaseNumbersForAllCaseTypes(CaseType caseType) {
            // Given
            String typeCode = caseType.getCode();
            CaseNumberSequence sequence = new CaseNumberSequence();
            sequence.setCaseType(typeCode);
            sequence.setYear(2026);
            sequence.setLastNumber(10);

            when(sequenceRepository.findByCaseTypeAndYearForUpdate(typeCode, 2026))
                .thenReturn(Optional.of(sequence));
            when(sequenceRepository.save(sequence)).thenReturn(sequence);

            // When
            String result = caseNumberGeneratorService.generateCaseNumber(caseType, testDateTime);

            // Then
            assertThat(result).isEqualTo(String.format("%s/0011/2026", typeCode));
            assertThat(sequence.getLastNumber()).isEqualTo(11);
        }

        @ParameterizedTest
        @CsvSource({
            "2025, 1, 1, CS/0001/2025",
            "2026, 6, 15, CS/0001/2026",
            "2027, 12, 31, CS/0001/2027",
            "2030, 7, 20, CS/0001/2030"
        })
        @DisplayName("Should generate case numbers for different years")
        void shouldGenerateCaseNumbersForDifferentYears(int year, int month, int day, String expectedPattern) {
            // Given
            LocalDateTime dateTime = LocalDateTime.of(year, month, day, 10, 0);
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CS", year))
                .thenReturn(Optional.empty());

            CaseNumberSequence newSequence = new CaseNumberSequence();
            newSequence.setCaseType("CS");
            newSequence.setYear(year);
            newSequence.setLastNumber(0);
            when(sequenceRepository.save(any(CaseNumberSequence.class)))
                .thenReturn(newSequence);

            // When
            String result = caseNumberGeneratorService.generateCaseNumber(CaseType.CS, dateTime);

            // Then
            assertThat(result).isEqualTo(expectedPattern);
        }

        @Test
        @DisplayName("Should format case number with proper zero padding")
        void shouldFormatCaseNumberWithProperZeroPadding() {
            // Given - Test various sequence numbers
            CaseNumberSequence sequence1 = new CaseNumberSequence();
            sequence1.setCaseType("WP");
            sequence1.setYear(2026);
            sequence1.setLastNumber(0);

            CaseNumberSequence sequence2 = new CaseNumberSequence();
            sequence2.setCaseType("WP");
            sequence2.setYear(2026);
            sequence2.setLastNumber(99);

            CaseNumberSequence sequence3 = new CaseNumberSequence();
            sequence3.setCaseType("WP");
            sequence3.setYear(2026);
            sequence3.setLastNumber(9999);

            when(sequenceRepository.findByCaseTypeAndYearForUpdate("WP", 2026))
                .thenReturn(Optional.of(sequence1))
                .thenReturn(Optional.of(sequence2))
                .thenReturn(Optional.of(sequence3));

            when(sequenceRepository.save(any(CaseNumberSequence.class)))
                .thenReturn(sequence1, sequence2, sequence3);

            // When & Then
            String result1 = caseNumberGeneratorService.generateCaseNumber(CaseType.WP, testDateTime);
            assertThat(result1).isEqualTo("WP/0001/2026");

            String result2 = caseNumberGeneratorService.generateCaseNumber(CaseType.WP, testDateTime);
            assertThat(result2).isEqualTo("WP/0100/2026");

            String result3 = caseNumberGeneratorService.generateCaseNumber(CaseType.WP, testDateTime);
            assertThat(result3).isEqualTo("WP/10000/2026");
        }

        @Test
        @DisplayName("Should handle large sequence numbers")
        void shouldHandleLargeSequenceNumbers() {
            // Given
            testSequence.setLastNumber(99999);
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CS", 2026))
                .thenReturn(Optional.of(testSequence));
            when(sequenceRepository.save(testSequence)).thenReturn(testSequence);

            // When
            String result = caseNumberGeneratorService.generateCaseNumber(CaseType.CS, testDateTime);

            // Then
            assertThat(result).isEqualTo("CS/100000/2026");
            assertThat(testSequence.getLastNumber()).isEqualTo(100000);
        }

        @Test
        @DisplayName("Should handle repository exception gracefully")
        void shouldHandleRepositoryExceptionGracefully() {
            // Given
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CS", 2026))
                .thenThrow(new RuntimeException("Database connection error"));

            // When & Then
            assertThatThrownBy(() -> caseNumberGeneratorService.generateCaseNumber(CaseType.CS, testDateTime))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection error");
        }
    }

    @Nested
    @DisplayName("generateCaseNumberFromCategory Method Tests")
    class GenerateCaseNumberFromCategoryTests {

        @ParameterizedTest
        @CsvSource({
            "CIVIL, CS",
            "CRIMINAL, CR",
            "FAMILY, FA",
            "PROPERTY, PR",
            "EMPLOYMENT, EM"
        })
        @DisplayName("Should generate case numbers from category strings")
        void shouldGenerateCaseNumbersFromCategoryStrings(String category, String expectedTypeCode) {
            // Given
            CaseNumberSequence sequence = new CaseNumberSequence();
            sequence.setCaseType(expectedTypeCode);
            sequence.setYear(2026);
            sequence.setLastNumber(42);

            when(sequenceRepository.findByCaseTypeAndYearForUpdate(expectedTypeCode, 2026))
                .thenReturn(Optional.of(sequence));
            when(sequenceRepository.save(sequence)).thenReturn(sequence);

            // When
            String result = caseNumberGeneratorService.generateCaseNumberFromCategory(category, testDateTime);

            // Then
            assertThat(result).isEqualTo(String.format("%s/0043/2026", expectedTypeCode));
        }

        @Test
        @DisplayName("Should default to CS for unknown category")
        void shouldDefaultToCSForUnknownCategory() {
            // Given
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CS", 2026))
                .thenReturn(Optional.of(testSequence));
            when(sequenceRepository.save(testSequence)).thenReturn(testSequence);

            // When
            String result = caseNumberGeneratorService.generateCaseNumberFromCategory("UNKNOWN", testDateTime);

            // Then
            assertThat(result).isEqualTo("CS/0006/2026");
        }

        @Test
        @DisplayName("Should handle null category")
        void shouldHandleNullCategory() {
            // Given
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CS", 2026))
                .thenReturn(Optional.of(testSequence));
            when(sequenceRepository.save(testSequence)).thenReturn(testSequence);

            // When
            String result = caseNumberGeneratorService.generateCaseNumberFromCategory(null, testDateTime);

            // Then
            assertThat(result).isEqualTo("CS/0006/2026");
        }

        @Test
        @DisplayName("Should handle case-insensitive categories")
        void shouldHandleCaseInsensitiveCategories() {
            // Given - Each call increments the sequence, so we need separate sequences
            CaseNumberSequence sequence1 = new CaseNumberSequence();
            sequence1.setCaseType("FA");
            sequence1.setYear(2026);
            sequence1.setLastNumber(0);

            CaseNumberSequence sequence2 = new CaseNumberSequence();
            sequence2.setCaseType("FA");
            sequence2.setYear(2026);
            sequence2.setLastNumber(1);

            CaseNumberSequence sequence3 = new CaseNumberSequence();
            sequence3.setCaseType("FA");
            sequence3.setYear(2026);
            sequence3.setLastNumber(2);

            when(sequenceRepository.findByCaseTypeAndYearForUpdate("FA", 2026))
                .thenReturn(Optional.of(sequence1))
                .thenReturn(Optional.of(sequence2))
                .thenReturn(Optional.of(sequence3));
            when(sequenceRepository.save(any(CaseNumberSequence.class)))
                .thenReturn(sequence1, sequence2, sequence3);

            // When
            String result1 = caseNumberGeneratorService.generateCaseNumberFromCategory("family", testDateTime);
            String result2 = caseNumberGeneratorService.generateCaseNumberFromCategory("FAMILY", testDateTime);
            String result3 = caseNumberGeneratorService.generateCaseNumberFromCategory("Family", testDateTime);

            // Then - All should map to FA type, but sequence increments
            assertThat(result1).isEqualTo("FA/0001/2026");
            assertThat(result2).isEqualTo("FA/0002/2026");
            assertThat(result3).isEqualTo("FA/0003/2026");
            // Verify all map to the same case type (FA) regardless of case
            assertThat(result1).startsWith("FA/");
            assertThat(result2).startsWith("FA/");
            assertThat(result3).startsWith("FA/");
        }
    }

    @Nested
    @DisplayName("Transaction and Concurrency Tests")
    class TransactionAndConcurrencyTests {

        @Test
        @DisplayName("Should use pessimistic locking for concurrent access")
        void shouldUsePessimisticLockingForConcurrentAccess() {
            // Given
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CS", 2026))
                .thenReturn(Optional.of(testSequence));
            when(sequenceRepository.save(testSequence)).thenReturn(testSequence);

            // When
            String result = caseNumberGeneratorService.generateCaseNumber(CaseType.CS, testDateTime);

            // Then
            assertThat(result).isEqualTo("CS/0006/2026");
            verify(sequenceRepository).findByCaseTypeAndYearForUpdate("CS", 2026);
            verify(sequenceRepository).save(testSequence);
        }

        @Test
        @DisplayName("Should handle multiple concurrent requests properly")
        void shouldHandleMultipleConcurrentRequestsProperly() throws Exception {
            // Given
            ExecutorService executor = Executors.newFixedThreadPool(5);
            CaseNumberSequence concurrentSequence = new CaseNumberSequence();
            concurrentSequence.setCaseType("CR");
            concurrentSequence.setYear(2026);
            concurrentSequence.setLastNumber(0);

            // Mock the repository to simulate sequential increments
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CR", 2026))
                .thenReturn(Optional.of(concurrentSequence));
            when(sequenceRepository.save(any(CaseNumberSequence.class)))
                .thenAnswer(invocation -> {
                    CaseNumberSequence seq = invocation.getArgument(0);
                    seq.setLastNumber(seq.getLastNumber());
                    return seq;
                });

            // When - Simulate concurrent requests
            CompletableFuture<String>[] futures = IntStream.range(0, 5)
                .mapToObj(i -> CompletableFuture.supplyAsync(() ->
                    caseNumberGeneratorService.generateCaseNumber(CaseType.CR, testDateTime), executor))
                .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

            // Then - Verify all requests completed
            for (CompletableFuture<String> future : futures) {
                assertThat(future.get()).startsWith("CR/");
                assertThat(future.get()).endsWith("/2026");
            }

            executor.shutdown();
            verify(sequenceRepository, times(5)).findByCaseTypeAndYearForUpdate("CR", 2026);
            verify(sequenceRepository, times(5)).save(any(CaseNumberSequence.class));
        }

        @Test
        @DisplayName("Should rollback on save failure")
        void shouldRollbackOnSaveFailure() {
            // Given
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CS", 2026))
                .thenReturn(Optional.of(testSequence));
            when(sequenceRepository.save(testSequence))
                .thenThrow(new RuntimeException("Save failed"));

            // When & Then
            assertThatThrownBy(() -> caseNumberGeneratorService.generateCaseNumber(CaseType.CS, testDateTime))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Save failed");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle extreme dates")
        void shouldHandleExtremeDates() {
            // Given
            LocalDateTime extremeDate1 = LocalDateTime.of(1900, 1, 1, 0, 0);
            LocalDateTime extremeDate2 = LocalDateTime.of(2099, 12, 31, 23, 59);

            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CS", 1900))
                .thenReturn(Optional.empty());
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CS", 2099))
                .thenReturn(Optional.empty());

            CaseNumberSequence seq1900 = new CaseNumberSequence();
            seq1900.setCaseType("CS");
            seq1900.setYear(1900);
            seq1900.setLastNumber(0);

            CaseNumberSequence seq2099 = new CaseNumberSequence();
            seq2099.setCaseType("CS");
            seq2099.setYear(2099);
            seq2099.setLastNumber(0);

            when(sequenceRepository.save(any(CaseNumberSequence.class)))
                .thenReturn(seq1900, seq2099);

            // When
            String result1 = caseNumberGeneratorService.generateCaseNumber(CaseType.CS, extremeDate1);
            String result2 = caseNumberGeneratorService.generateCaseNumber(CaseType.CS, extremeDate2);

            // Then
            assertThat(result1).isEqualTo("CS/0001/1900");
            assertThat(result2).isEqualTo("CS/0001/2099");
        }

        @Test
        @DisplayName("Should handle year boundaries correctly")
        void shouldHandleYearBoundariesCorrectly() {
            // Given
            LocalDateTime endOfYear = LocalDateTime.of(2025, 12, 31, 23, 59, 59);
            LocalDateTime startOfNextYear = LocalDateTime.of(2026, 1, 1, 0, 0, 1);

            // Different sequences for different years
            CaseNumberSequence seq2025 = new CaseNumberSequence();
            seq2025.setCaseType("WP");
            seq2025.setYear(2025);
            seq2025.setLastNumber(999);

            CaseNumberSequence seq2026 = new CaseNumberSequence();
            seq2026.setCaseType("WP");
            seq2026.setYear(2026);
            seq2026.setLastNumber(0);

            when(sequenceRepository.findByCaseTypeAndYearForUpdate("WP", 2025))
                .thenReturn(Optional.of(seq2025));
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("WP", 2026))
                .thenReturn(Optional.of(seq2026));

            when(sequenceRepository.save(any(CaseNumberSequence.class)))
                .thenReturn(seq2025, seq2026);

            // When
            String result2025 = caseNumberGeneratorService.generateCaseNumber(CaseType.WP, endOfYear);
            String result2026 = caseNumberGeneratorService.generateCaseNumber(CaseType.WP, startOfNextYear);

            // Then
            assertThat(result2025).isEqualTo("WP/1000/2025");
            assertThat(result2026).isEqualTo("WP/0001/2026");
        }

        @Test
        @DisplayName("Should handle sequence reset for new case types")
        void shouldHandleSequenceResetForNewCaseTypes() {
            // Given
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("MA", 2026))
                .thenReturn(Optional.empty());

            CaseNumberSequence newSequence = new CaseNumberSequence();
            newSequence.setCaseType("MA");
            newSequence.setYear(2026);
            newSequence.setLastNumber(0);
            when(sequenceRepository.save(any(CaseNumberSequence.class)))
                .thenReturn(newSequence);

            // When
            String result = caseNumberGeneratorService.generateCaseNumber(CaseType.MA, testDateTime);

            // Then
            assertThat(result).isEqualTo("MA/0001/2026");
            verify(sequenceRepository).save(argThat(sequence ->
                sequence.getCaseType().equals("MA") &&
                sequence.getYear().equals(2026) &&
                sequence.getLastNumber().equals(1)
            ));
        }

        @Test
        @DisplayName("Should handle service instantiation correctly")
        void shouldHandleServiceInstantiationCorrectly() {
            // Given
            CaseNumberSequenceRepository mockRepo = mock(CaseNumberSequenceRepository.class);

            // When
            CaseNumberGeneratorService service = new CaseNumberGeneratorService(mockRepo);

            // Then
            assertThat(service).isNotNull();
        }
    }

    @Nested
    @DisplayName("Business Logic Validation Tests")
    class BusinessLogicValidationTests {

        @Test
        @DisplayName("Should ensure case numbers are always unique within same type and year")
        void shouldEnsureCaseNumbersAreAlwaysUniqueWithinSameTypeAndYear() {
            // Given
            CaseNumberSequence sequence = new CaseNumberSequence();
            sequence.setCaseType("CR");
            sequence.setYear(2026);
            sequence.setLastNumber(100);

            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CR", 2026))
                .thenReturn(Optional.of(sequence));
            when(sequenceRepository.save(sequence))
                .thenReturn(sequence);

            // When
            String result1 = caseNumberGeneratorService.generateCaseNumber(CaseType.CR, testDateTime);
            String result2 = caseNumberGeneratorService.generateCaseNumber(CaseType.CR, testDateTime);

            // Then
            assertThat(result1).isEqualTo("CR/0101/2026");
            assertThat(result2).isEqualTo("CR/0102/2026");
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("Should allow same sequence numbers for different case types")
        void shouldAllowSameSequenceNumbersForDifferentCaseTypes() {
            // Given
            CaseNumberSequence csSequence = new CaseNumberSequence();
            csSequence.setCaseType("CS");
            csSequence.setYear(2026);
            csSequence.setLastNumber(50);

            CaseNumberSequence crSequence = new CaseNumberSequence();
            crSequence.setCaseType("CR");
            crSequence.setYear(2026);
            crSequence.setLastNumber(50);

            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CS", 2026))
                .thenReturn(Optional.of(csSequence));
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("CR", 2026))
                .thenReturn(Optional.of(crSequence));
            when(sequenceRepository.save(any(CaseNumberSequence.class)))
                .thenReturn(csSequence, crSequence);

            // When
            String civilResult = caseNumberGeneratorService.generateCaseNumber(CaseType.CS, testDateTime);
            String criminalResult = caseNumberGeneratorService.generateCaseNumber(CaseType.CR, testDateTime);

            // Then
            assertThat(civilResult).isEqualTo("CS/0051/2026");
            assertThat(criminalResult).isEqualTo("CR/0051/2026");
            // Same sequence number but different types - this is expected and valid
        }

        @Test
        @DisplayName("Should allow same sequence numbers for different years")
        void shouldAllowSameSequenceNumbersForDifferentYears() {
            // Given
            LocalDateTime date2025 = LocalDateTime.of(2025, 6, 15, 10, 0);
            LocalDateTime date2026 = LocalDateTime.of(2026, 6, 15, 10, 0);

            CaseNumberSequence seq2025 = new CaseNumberSequence();
            seq2025.setCaseType("FA");
            seq2025.setYear(2025);
            seq2025.setLastNumber(25);

            CaseNumberSequence seq2026 = new CaseNumberSequence();
            seq2026.setCaseType("FA");
            seq2026.setYear(2026);
            seq2026.setLastNumber(25);

            when(sequenceRepository.findByCaseTypeAndYearForUpdate("FA", 2025))
                .thenReturn(Optional.of(seq2025));
            when(sequenceRepository.findByCaseTypeAndYearForUpdate("FA", 2026))
                .thenReturn(Optional.of(seq2026));
            when(sequenceRepository.save(any(CaseNumberSequence.class)))
                .thenReturn(seq2025, seq2026);

            // When
            String result2025 = caseNumberGeneratorService.generateCaseNumber(CaseType.FA, date2025);
            String result2026 = caseNumberGeneratorService.generateCaseNumber(CaseType.FA, date2026);

            // Then
            assertThat(result2025).isEqualTo("FA/0026/2025");
            assertThat(result2026).isEqualTo("FA/0026/2026");
            // Same sequence number but different years - this is expected and valid
        }
    }
}
