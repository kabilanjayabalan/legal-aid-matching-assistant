package com.legalaid.backend.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.legalaid.backend.model.CaseNumberSequence;
import com.legalaid.backend.model.CaseType;
import com.legalaid.backend.repository.CaseNumberSequenceRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating unique case numbers in the format: TYPE/NUMBER/YEAR
 * Example: WP/1234/2023 - Writ Petition number 1234 filed in 2023
 */
@Slf4j
@Service
public class CaseNumberGeneratorService {

    private final CaseNumberSequenceRepository sequenceRepository;

    public CaseNumberGeneratorService(CaseNumberSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * Generate the next case number for the given case type.
     * The format will be: TYPE/NNNN/YYYY where:
     * - TYPE is the case type code (e.g., WP, CS, CR)
     * - NNNN is a 4-digit sequential number (padded with zeros)
     * - YYYY is the current year
     *
     * This method is transactional and thread-safe to prevent duplicate case numbers.
     *
     * @param caseType The type of case
     * @param createdAt The creation timestamp (used to determine the year)
     * @return The generated case number (e.g., "WP/0001/2023")
     */
    @Transactional
    public String generateCaseNumber(CaseType caseType, LocalDateTime createdAt) {
        String typeCode = caseType.getCode();
        int year = createdAt.getYear();

        log.debug("Generating case number for type: {}, year: {}", typeCode, year);

        // Get or create the sequence for this type and year with pessimistic lock
        CaseNumberSequence sequence = sequenceRepository
            .findByCaseTypeAndYearForUpdate(typeCode, year)
            .orElseGet(() -> {
                log.info("Creating new sequence for type: {}, year: {}", typeCode, year);
                CaseNumberSequence newSequence = new CaseNumberSequence();
                newSequence.setCaseType(typeCode);
                newSequence.setYear(year);
                newSequence.setLastNumber(0);
                return sequenceRepository.save(newSequence);
            });

        // Increment the sequence number
        int nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        sequenceRepository.save(sequence);

        // Format the case number: TYPE/NNNN/YYYY
        String caseNumber = String.format("%s/%04d/%d", typeCode, nextNumber, year);

        log.info("Generated case number: {}", caseNumber);

        return caseNumber;
    }

    /**
     * Generate a case number based on category string
     *
     * @param category The category string (e.g., "CIVIL", "CRIMINAL")
     * @param createdAt The creation timestamp
     * @return The generated case number
     */
    @Transactional
    public String generateCaseNumberFromCategory(String category, LocalDateTime createdAt) {
        CaseType caseType = CaseType.fromCategory(category);
        return generateCaseNumber(caseType, createdAt);
    }
}

