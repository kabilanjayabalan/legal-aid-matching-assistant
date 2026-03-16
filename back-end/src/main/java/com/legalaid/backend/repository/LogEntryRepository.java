package com.legalaid.backend.repository;

import com.legalaid.backend.model.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface LogEntryRepository extends JpaRepository<LogEntry, Integer>, JpaSpecificationExecutor<LogEntry> {

    Page<LogEntry> findAllByOrderByLogTimestampDesc(Pageable pageable);

    @Query("SELECT COUNT(l) FROM LogEntry l WHERE UPPER(l.level) = 'ERROR'")
    long countByErrorLevel();

    @Query("SELECT COUNT(l) FROM LogEntry l WHERE UPPER(l.level) = 'WARN'")
    long countByWarnLevel();

    @Query("SELECT COUNT(l) FROM LogEntry l WHERE UPPER(l.level) = 'INFO'")
    long countByInfoLevel();

    @Query("SELECT FUNCTION('DATE', l.logTimestamp) AS logDate, COUNT(l) AS count " +
            "FROM LogEntry l " +
            "WHERE l.logTimestamp >= :from " +
            "GROUP BY FUNCTION('DATE', l.logTimestamp) " +
            "ORDER BY logDate")
    List<Object[]> countLogsByDateSince(LocalDateTime from);

    @Query("SELECT COALESCE(l.logger, 'Unknown') AS logger, COUNT(l) AS count " +
            "FROM LogEntry l " +
            "GROUP BY COALESCE(l.logger, 'Unknown') " +
            "ORDER BY count DESC")
    List<Object[]> countLogsByLogger();
    @Transactional
    @Modifying
    @Query("DELETE FROM LogEntry l WHERE l.logTimestamp < :cutoffDate")
        void deleteLogsOlderThan(LocalDateTime cutoffDate);
}


