package com.legalaid.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MatchStatsDTO {
    private long totalMatches;
    private long pending;
    private long citizenAccepted;
    private long providerConfirmed;
    private long rejected;
    private double averageMatchPercentage;
}

