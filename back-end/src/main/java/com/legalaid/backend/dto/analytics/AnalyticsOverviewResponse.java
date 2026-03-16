package com.legalaid.backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AnalyticsOverviewResponse {

    private long totalUsers;
    private long totalLawyers;
    private long totalNGOs;
    private long totalCases;
    private long totalMatches;
    private long resolvedCases;
    private long activeAppointments;
}
