package com.legalaid.backend.service;

import com.legalaid.backend.repository.LogEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sun.management.OperatingSystemMXBean;

@Slf4j
@Service
public class SystemHealthService {

    private final LogEntryRepository logEntryRepository;

    public SystemHealthService(LogEntryRepository logEntryRepository) {
        this.logEntryRepository = logEntryRepository;
    }

    public SystemHealthDTO getSystemHealth() {
        try {
            // CPU Usage
            double cpuUsage = getCpuUsage();

            // Memory Usage
            double memoryUsage = getMemoryUsage();

            // Uptime in days
            long uptimeDays = getUptimeDays();

            // Disk Usage
            double diskUsage = getDiskUsage();

            // Log counts
            long criticalErrors = logEntryRepository.countByErrorLevel();
            long warningAlerts = logEntryRepository.countByWarnLevel();
            long infoMessages = logEntryRepository.countByInfoLevel();

            return new SystemHealthDTO(
                    cpuUsage,
                    memoryUsage,
                    uptimeDays,
                    diskUsage,
                    criticalErrors,
                    warningAlerts,
                    infoMessages
            );
        } catch (Exception e) {
            log.error("Error collecting system health metrics", e);
            throw new RuntimeException("Failed to collect system health metrics", e);
        }
    }

    public List<SystemLoadPointDTO> getSystemLoadOverTime() {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(6); // last 7 days including today
        LocalDateTime fromDateTime = fromDate.atStartOfDay();

        List<Object[]> raw = logEntryRepository.countLogsByDateSince(fromDateTime);

        Map<LocalDate, Long> countsByDate = new LinkedHashMap<>();
        long maxCount = 0;

        for (Object[] row : raw) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            long count = ((Number) row[1]).longValue();
            countsByDate.put(date, count);
            if (count > maxCount) {
                maxCount = count;
            }
        }

        List<SystemLoadPointDTO> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = fromDate.plusDays(i);
            long count = countsByDate.getOrDefault(date, 0L);
            double loadPercent;
            if (maxCount == 0) {
                loadPercent = 0.0;
            } else {
                loadPercent = (double) count / maxCount * 40.0 + 40.0; // scale into 40-80% range
            }
            String dayLabel = date.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            result.add(new SystemLoadPointDTO(dayLabel, Math.round(loadPercent)));
        }

        return result;
    }

    public List<ServiceActivityDTO> getServiceActivityBreakdown() {
        List<Object[]> raw = logEntryRepository.countLogsByLogger();
        List<ServiceActivityDTO> result = new ArrayList<>();

        for (Object[] row : raw) {
            String loggerName = (String) row[0];
            long count = ((Number) row[1]).longValue();
            result.add(new ServiceActivityDTO(loggerName, count));
        }

        return result;
    }

    private double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getProcessCpuLoad() * 100;
            // CPU load might return -1 if not available, handle that case
            if (cpuLoad < 0) {
                return 0.0;
            }
            // Ensure value is between 0 and 100
            return Math.max(0, Math.min(100, cpuLoad));
        } catch (Exception e) {
            log.warn("Could not get CPU usage", e);
            return 0.0;
        }
    }

    private double getMemoryUsage() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            
            if (maxMemory > 0) {
                return (double) usedMemory / maxMemory * 100;
            }
            return 0.0;
        } catch (Exception e) {
            log.warn("Could not get memory usage", e);
            return 0.0;
        }
    }

    private long getUptimeDays() {
        try {
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            long uptimeMillis = runtimeBean.getUptime();
            return uptimeMillis / (1000 * 60 * 60 * 24); // Convert to days
        } catch (Exception e) {
            log.warn("Could not get uptime", e);
            return 0;
        }
    }

    private double getDiskUsage() {
        try {
            File root;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows: use the drive where the application is running
                root = new File(System.getProperty("user.dir").substring(0, 3));
            } else {
                // Unix-like systems
                root = new File("/");
            }
            
            if (!root.exists()) {
                log.warn("Root path does not exist: {}", root.getAbsolutePath());
                return 0.0;
            }
            
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            if (totalSpace > 0) {
                return (double) usedSpace / totalSpace * 100;
            }
            return 0.0;
        } catch (Exception e) {
            log.warn("Could not get disk usage", e);
            return 0.0;
        }
    }

    public record SystemHealthDTO(
            double cpuUsage,
            double memoryUsage,
            long uptimeDays,
            double diskUsage,
            long criticalErrors,
            long warningAlerts,
            long infoMessages
    ) {}

    public record SystemLoadPointDTO(
            String day,
            long load
    ) {}

    public record ServiceActivityDTO(
            String name,
            long count
    ) {}
}

