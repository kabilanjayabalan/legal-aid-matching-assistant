package com.legalaid.backend.dto;

import com.legalaid.backend.model.CaseStatus;
import com.legalaid.backend.model.MatchStatus;

public record MatchCardDTO(
        String source,          // REGISTERED | DIRECTORY
        String providerType,    // LAWYER | NGO
        Integer matchId,        // null for directory
        String name,
        String city,
        String expertise,
        Integer score,
        Boolean verified,
        Boolean canInteract,     // false for directory
        MatchStatus matchStatus,
        CaseStatus caseStatus
) {}

