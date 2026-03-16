package com.legalaid.backend.service.analytics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.legalaid.backend.dto.CaseMonitoringResponse;
import com.legalaid.backend.dto.analytics.CaseAnalyticsResponse;
import com.legalaid.backend.dto.analytics.CaseStatsResponse;
import com.legalaid.backend.model.CaseStatus;
import com.legalaid.backend.model.Match;
import com.legalaid.backend.model.ProviderType;
import com.legalaid.backend.repository.CaseRepository;
import com.legalaid.backend.repository.LawyerProfileRepository;
import com.legalaid.backend.repository.MatchRepository;
import com.legalaid.backend.repository.NGOProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CaseAnalyticsService {

    private final CaseRepository caseRepository;
    private final MatchRepository matchRepository;
    private final LawyerProfileRepository lawyerProfileRepository;
    private final NGOProfileRepository ngoProfileRepository;

    private static final List<String> FIXED_CATEGORIES = List.of(
            "CIVIL",
            "CRIMINAL",
            "FAMILY",
            "PROPERTY",
            "EMPLOYMENT"
    );

    private static final Map<String, String> CATEGORY_LOOKUP = Map.of(
            "civil", "CIVIL",
            "criminal", "CRIMINAL",
            "family", "FAMILY",
            "property", "PROPERTY",
            "employment", "EMPLOYMENT"
    );

    private static final List<CaseStatus> FIXED_STATUSES = List.of(
            CaseStatus.OPEN,
            CaseStatus.IN_PROGRESS,
            CaseStatus.CLOSED
    );

    public CaseAnalyticsResponse getCaseAnalytics() {

        long totalCases = caseRepository.count();
        long resolvedCases = caseRepository.countByStatus(CaseStatus.CLOSED);

        Map<String, Map<String, Long>> casesByCategory = new HashMap<>();

        // Step 1: Initialize
        for (String category : FIXED_CATEGORIES) {
            Map<String, Long> statusMap = new HashMap<>();
            for (CaseStatus status : FIXED_STATUSES) {
                statusMap.put(status.name(), 0L);
            }
            statusMap.put("TOTAL", 0L);
            casesByCategory.put(category, statusMap);
        }

        // Step 2: Populate from DB (case-insensitive)
        caseRepository.countCasesByCategoryAndStatus()
                .forEach(row -> {
                    String rawCategory = row[0].toString().toLowerCase();
                    CaseStatus status = (CaseStatus) row[1];
                    Long count = (Long) row[2];

                    String normalizedCategory = CATEGORY_LOOKUP.get(rawCategory);
                    if (normalizedCategory == null) return;

                    Map<String, Long> statusMap = casesByCategory.get(normalizedCategory);

                    statusMap.put(status.name(), count);
                    statusMap.put("TOTAL", statusMap.get("TOTAL") + count);
                });

        return new CaseAnalyticsResponse(
                totalCases,
                resolvedCases,
                casesByCategory
        );
    }
    public CaseStatsResponse getCaseStats() {

        long totalCases = caseRepository.count();

        long open = 0;
        long inProgress = 0;
        long closed = 0;

        for (Object[] row : caseRepository.countCasesByStatus()) {
            CaseStatus status = (CaseStatus) row[0];
            long count = (Long) row[1];

            switch (status) {
                case OPEN -> open = count;
                case IN_PROGRESS -> inProgress = count;
                case CLOSED -> closed += count;
            }
        }

        return new CaseStatsResponse(
                totalCases,
                open,
                inProgress,
                closed
        );
    }
    public Page<CaseMonitoringResponse> getPaginatedCases(
            String search,
            String status,
            Pageable pageable
    ) {

        CaseStatus caseStatus = null;

        if (status != null && !status.equalsIgnoreCase("ALL")) {
            caseStatus = CaseStatus.valueOf(status.toUpperCase());
        }

        if (search != null && search.isBlank()) {
            search = null;
        }
        Pageable safePageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize()
        );

        return caseRepository
                .findCasesForMonitoring(search, caseStatus, safePageable)
                .map(c -> {
                    // If case already has an assignedTo, use the simple method
                    if (c.getAssignedTo() != null) {
                        return CaseMonitoringResponse.fromEntity(c);
                    }

                    // Otherwise, look for an active match to get the provider name
                    String providerName = getProviderNameForCase(c.getId());
                    return CaseMonitoringResponse.fromEntity(c, providerName);
                });
    }

    private String getProviderNameForCase(Integer caseId) {
        Optional<Match> activeMatch = matchRepository.findActiveMatchByCaseId(caseId);

        if (activeMatch.isEmpty()) {
            return null;
        }

        Match match = activeMatch.get();
        Integer providerId = match.getProviderId();
        ProviderType providerType = match.getProviderType();

        if (providerType == ProviderType.LAWYER) {
            return lawyerProfileRepository.findByUserId(providerId)
                    .map(profile -> profile.getName())
                    .orElse(null);
        } else if (providerType == ProviderType.NGO) {
            return ngoProfileRepository.findByUserId(providerId)
                    .map(profile -> profile.getOrganization())
                    .orElse(null);
        }

        return null;
    }
}
