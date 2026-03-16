package com.legalaid.backend.service.analytics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.legalaid.backend.dto.MatchMonitoringDTO;
import com.legalaid.backend.dto.MatchStatsDTO;
import com.legalaid.backend.dto.analytics.MatchAnalyticsResponse;
import com.legalaid.backend.model.MatchStatus;
import com.legalaid.backend.repository.MatchRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchAnalyticsService {

    private final MatchRepository matchRepository;

    public MatchAnalyticsResponse getMatchAnalytics() {

        long total = matchRepository.count();

        long citizenAccepted =
                matchRepository.countByStatus(MatchStatus.CITIZEN_ACCEPTED);

        long providerConfirmed =
                matchRepository.countByStatus(MatchStatus.PROVIDER_CONFIRMED);

        long rejected =
                matchRepository.countByStatus(MatchStatus.REJECTED);

        double successRate = total == 0
                ? 0
                : (providerConfirmed * 100.0) / total;

        return new MatchAnalyticsResponse(
                total,
                citizenAccepted,
                providerConfirmed,
                rejected,
                Math.round(successRate * 100.0) / 100.0
        );
    }

    public MatchStatsDTO getMatchStats() {
        long total = matchRepository.count();
        long pending = matchRepository.countByStatus(MatchStatus.PENDING);
        long citizenAccepted = matchRepository.countByStatus(MatchStatus.CITIZEN_ACCEPTED);
        long providerConfirmed = matchRepository.countByStatus(MatchStatus.PROVIDER_CONFIRMED);
        long rejected = matchRepository.countByStatus(MatchStatus.REJECTED);

        // Calculate average match percentage (confirmed matches / total matches)
        double averageMatchPercentage = total == 0
            ? 0.0
            : (providerConfirmed * 100.0) / total;

        return new MatchStatsDTO(
            total,
            pending,
            citizenAccepted,
            providerConfirmed,
            rejected,
            Math.round(averageMatchPercentage * 100.0) / 100.0
        );
    }

    public Page<MatchMonitoringDTO> getMatchesForMonitoring(
            String search,
            String statusStr,
            Pageable pageable) {

        // Convert status string to pass to repository
        String status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                // Validate the status is a valid MatchStatus enum value
                MatchStatus.valueOf(statusStr.toUpperCase());
                status = statusStr.toUpperCase();
            } catch (IllegalArgumentException ignored) {
                // Invalid status, return all
            }
        }

        return matchRepository.findMatchesForMonitoring(search, status, pageable);
    }
}

