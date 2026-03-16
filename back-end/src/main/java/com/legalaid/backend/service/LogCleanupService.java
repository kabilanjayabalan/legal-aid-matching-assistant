package com.legalaid.backend.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.legalaid.backend.repository.LogEntryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogCleanupService {

    private final LogEntryRepository logEntryRepository;

    // Run every day at 1 AM
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void cleanupOldLogs() {
        log.info("Starting scheduled job to clean up old logs...");
        // Default retention: 30 days
        LocalDateTime logExpirationThreshold = LocalDateTime.now().minusDays(30);
        logEntryRepository.deleteLogsOlderThan(logExpirationThreshold);
        log.info("Successfully deleted logs older than 30 days.");
    }
    @Transactional
    public void cleanupLogsManually(int days) {
        log.info("Starting manual cleanup of logs older than {} days...", days);
        LocalDateTime logExpirationThreshold = LocalDateTime.now().minusDays(days);
        logEntryRepository.deleteLogsOlderThan(logExpirationThreshold);
        log.info("Successfully deleted logs older than {} days.", days);
    }
}
