package com.legalaid.backend.controller;

import com.legalaid.backend.model.LogEntry;
import com.legalaid.backend.repository.LogEntryRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/logs")
public class LogController {

    private final LogEntryRepository logEntryRepository;

    public LogController(LogEntryRepository logEntryRepository) {
        this.logEntryRepository = logEntryRepository;
    }

    /**
     * Returns log entries in pages of 10 records with optional filtering and sorting.
     *
     * @param page zero-based page index (default 0)
     * @param level filter by log level (e.g., INFO, DEBUG, ERROR, WARN)
     * @param logger filter by logger name (partial match supported)
     * @param startDate filter logs from this timestamp (inclusive)
     * @param endDate filter logs until this timestamp (inclusive)
     * @param sortBy field to sort by (logTimestamp, level, logger) - default: logTimestamp
     * @param sortDir sort direction (asc, desc) - default: desc
     */
    @GetMapping
    public Page<LogEntry> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String logger,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "logTimestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        int pageSize = 10; // fixed page size of 10
        
        // specification for filtering
        Specification<LogEntry> spec = buildSpecification(level, logger, startDate, endDate);
        
        // sort
        Sort sort = buildSort(sortBy, sortDir);
        
        Pageable pageable = PageRequest.of(page, pageSize, sort);
        return logEntryRepository.findAll(spec, pageable);
    }
    
    private Specification<LogEntry> buildSpecification(String level, String logger, 
                                                       LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (level != null && !level.isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("level")), level.toUpperCase()));
            }
            
            if (logger != null && !logger.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("logger")), 
                    "%" + logger.toLowerCase() + "%"));
            }
            
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("logTimestamp"), startDate));
            }
            
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("logTimestamp"), endDate));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    private Sort buildSort(String sortBy, String sortDir) {
        // Validate sortBy field - only allow valid fields
        String validSortBy;
        String lowerSortBy = sortBy.toLowerCase();
        if ("level".equals(lowerSortBy) || "logger".equals(lowerSortBy) || 
            "logtimestamp".equals(lowerSortBy)) {
            validSortBy = sortBy;
        } else {
            validSortBy = "logTimestamp";
        }
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        return Sort.by(direction, validSortBy);
    }
}


