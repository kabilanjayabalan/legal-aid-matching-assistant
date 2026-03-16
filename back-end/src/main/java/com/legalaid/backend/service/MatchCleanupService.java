package com.legalaid.backend.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.legalaid.backend.model.Match;
import com.legalaid.backend.model.MatchStatus;
import com.legalaid.backend.repository.MatchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchCleanupService {

    private final MatchRepository matchRepository;

    // Run every day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void rejectExpiredMatches() {
        log.info("Starting scheduled job to reject expired matches...");

        // Define expiration period (e.g., 7 days)
        LocalDateTime expirationThreshold = LocalDateTime.now().minusDays(7);

        // Define statuses that should be checked for expiration
        List<MatchStatus> targetStatuses = Arrays.asList(
            MatchStatus.PENDING,
            MatchStatus.CITIZEN_ACCEPTED
        );

        // Find matches that are in target statuses and older than the threshold
        List<Match> expiredMatches = matchRepository.findByStatusInAndCreatedAtBefore(
            targetStatuses, 
            expirationThreshold
        );

        if (expiredMatches.isEmpty()) {
            log.info("No expired matches found.");
            return;
        }

        log.info("Found {} expired matches. Rejecting them...", expiredMatches.size());

        for (Match match : expiredMatches) {
            match.setStatus(MatchStatus.REJECTED);
            // Optionally, you could add a reason or log this action
            log.debug("Rejecting match ID: {} due to expiration. Created at: {}", match.getId(), match.getCreatedAt());
        }

        matchRepository.saveAll(expiredMatches);
        log.info("Successfully rejected {} expired matches.", expiredMatches.size());
    }
}
