package com.legalaid.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderDashboardStatsDTO {
    private long matchRequestsCount;
    private long assignedCasesCount;
    private List<MatchSummaryDTO> recentMatchRequests;
    private List<CaseSummaryDTO> recentAssignedCases;
}
