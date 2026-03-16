package com.legalaid.backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MatchAnalyticsResponse {

    private long totalMatches;
    private long citizenAccepted;
    private long providerConfirmed;
    private long rejectedMatches;
    private double successRate;
}

