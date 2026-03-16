package com.legalaid.backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CaseStatsResponse {

    private long totalCases;
    private long openCases;
    private long inProgressCases;
    private long closedCases;
}
