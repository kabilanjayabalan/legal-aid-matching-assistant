package com.legalaid.backend.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service to collect and aggregate system metrics from Spring Boot Actuator
 * Provides comprehensive system monitoring data for admin dashboard
 */
@Slf4j
@Service
public class ActuatorMetricsService {

    private final MeterRegistry meterRegistry;
    private final MetricsEndpoint metricsEndpoint;
    private final HealthEndpoint healthEndpoint;
    private final Optional<BuildProperties> buildProperties;

    public ActuatorMetricsService(MeterRegistry meterRegistry,
                                   MetricsEndpoint metricsEndpoint,
                                   HealthEndpoint healthEndpoint,
                                   Optional<BuildProperties> buildProperties) {
        this.meterRegistry = meterRegistry;
        this.metricsEndpoint = metricsEndpoint;
        this.healthEndpoint = healthEndpoint;
        this.buildProperties = buildProperties;
    }

    /**
     * Get comprehensive system metrics from actuator
     */
    public ActuatorSystemMetrics getSystemMetrics() {
        try {
            return new ActuatorSystemMetrics(
                getCpuMetrics(),
                getMemoryMetrics(),
                getJvmMetrics(),
                getDiskMetrics(),
                getThreadMetrics(),
                getGcMetrics(),
                getHttpMetrics(),
                getHealthStatus(),
                getApplicationInfo()
            );
        } catch (Exception e) {
            log.error("Error collecting actuator system metrics", e);
            throw new RuntimeException("Failed to collect system metrics", e);
        }
    }

    /**
     * CPU related metrics
     */
    private CpuMetrics getCpuMetrics() {
        double systemCpuUsage = getMetricValue("system.cpu.usage") * 100;
        double processCpuUsage = getMetricValue("process.cpu.usage") * 100;
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        double systemLoadAverage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();

        return new CpuMetrics(
            Math.max(0, Math.min(100, systemCpuUsage)),
            Math.max(0, Math.min(100, processCpuUsage)),
            availableProcessors,
            systemLoadAverage >= 0 ? systemLoadAverage : 0
        );
    }

    /**
     * Memory related metrics
     */
    private MemoryMetrics getMemoryMetrics() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        long heapUsed = heapUsage.getUsed();
        long heapMax = heapUsage.getMax();
        long heapCommitted = heapUsage.getCommitted();

        long nonHeapUsed = nonHeapUsage.getUsed();
        long nonHeapCommitted = nonHeapUsage.getCommitted();

        double heapUsagePercent = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;

        // Get buffer pool metrics if available
        double directBufferUsed = getMetricValue("jvm.buffer.memory.used", "id", "direct");
        double mappedBufferUsed = getMetricValue("jvm.buffer.memory.used", "id", "mapped");

        return new MemoryMetrics(
            bytesToMB(heapUsed),
            bytesToMB(heapMax),
            bytesToMB(heapCommitted),
            Math.round(heapUsagePercent * 100.0) / 100.0,
            bytesToMB(nonHeapUsed),
            bytesToMB(nonHeapCommitted),
            bytesToMB((long) directBufferUsed),
            bytesToMB((long) mappedBufferUsed)
        );
    }

    /**
     * JVM related metrics
     */
    private JvmMetrics getJvmMetrics() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptimeMillis = runtimeBean.getUptime();
        long startTime = runtimeBean.getStartTime();

        String jvmVersion = System.getProperty("java.version");
        String jvmVendor = System.getProperty("java.vendor");
        String jvmName = runtimeBean.getVmName();

        // Calculate uptime in different units
        Duration uptime = Duration.ofMillis(uptimeMillis);
        long uptimeDays = uptime.toDays();
        long uptimeHours = uptime.toHoursPart();
        long uptimeMinutes = uptime.toMinutesPart();

        return new JvmMetrics(
            jvmVersion,
            jvmVendor,
            jvmName,
            uptimeDays,
            uptimeHours,
            uptimeMinutes,
            Instant.ofEpochMilli(startTime).toString(),
            formatUptime(uptime)
        );
    }

    /**
     * Disk related metrics
     */
    private DiskMetrics getDiskMetrics() {
        double diskFree = getMetricValue("disk.free");
        double diskTotal = getMetricValue("disk.total");
        double diskUsed = diskTotal - diskFree;
        double diskUsagePercent = diskTotal > 0 ? (diskUsed / diskTotal) * 100 : 0;

        return new DiskMetrics(
            bytesToGB(diskFree),
            bytesToGB(diskTotal),
            bytesToGB(diskUsed),
            Math.round(diskUsagePercent * 100.0) / 100.0
        );
    }

    /**
     * Thread related metrics
     */
    private ThreadMetrics getThreadMetrics() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        int liveThreads = threadBean.getThreadCount();
        int daemonThreads = threadBean.getDaemonThreadCount();
        int peakThreads = threadBean.getPeakThreadCount();
        long totalStartedThreads = threadBean.getTotalStartedThreadCount();

        // Get thread states count
        Map<Thread.State, Integer> threadStates = new HashMap<>();
        for (Thread.State state : Thread.State.values()) {
            threadStates.put(state, 0);
        }

        long[] threadIds = threadBean.getAllThreadIds();
        for (long threadId : threadIds) {
            java.lang.management.ThreadInfo info = threadBean.getThreadInfo(threadId);
            if (info != null) {
                threadStates.merge(info.getThreadState(), 1, Integer::sum);
            }
        }

        return new ThreadMetrics(
            liveThreads,
            daemonThreads,
            peakThreads,
            totalStartedThreads,
            threadStates.getOrDefault(Thread.State.RUNNABLE, 0),
            threadStates.getOrDefault(Thread.State.BLOCKED, 0),
            threadStates.getOrDefault(Thread.State.WAITING, 0),
            threadStates.getOrDefault(Thread.State.TIMED_WAITING, 0)
        );
    }

    /**
     * Garbage Collection metrics
     */
    private GcMetrics getGcMetrics() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        long totalGcCount = 0;
        long totalGcTime = 0;
        List<GcCollectorInfo> collectors = new ArrayList<>();

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            long count = gcBean.getCollectionCount();
            long time = gcBean.getCollectionTime();
            if (count >= 0) {
                totalGcCount += count;
                totalGcTime += time;
                collectors.add(new GcCollectorInfo(gcBean.getName(), count, time));
            }
        }

        // Get GC pause metrics from micrometer if available
        double gcPauseMax = getMetricValue("jvm.gc.pause.max");
        double gcPauseCount = getMetricValue("jvm.gc.pause.count");

        return new GcMetrics(
            totalGcCount,
            totalGcTime,
            collectors,
            gcPauseMax,
            (long) gcPauseCount
        );
    }

    /**
     * HTTP request metrics
     */
    private HttpMetrics getHttpMetrics() {
        double totalRequests = getMetricValue("http.server.requests.count");
        double totalRequestTime = getMetricValue("http.server.requests.sum");
        double avgResponseTime = totalRequests > 0 ? totalRequestTime / totalRequests : 0;

        // Try to get success/error counts
        double successRequests = getMetricValue("http.server.requests", "status", "200") +
                                 getMetricValue("http.server.requests", "status", "201") +
                                 getMetricValue("http.server.requests", "status", "204");
        double clientErrors = getMetricValue("http.server.requests", "status", "4*");
        double serverErrors = getMetricValue("http.server.requests", "status", "5*");

        // Get active connections if available
        double activeConnections = getMetricValue("tomcat.threads.current");
        double maxConnections = getMetricValue("tomcat.threads.config.max");

        return new HttpMetrics(
            (long) totalRequests,
            avgResponseTime,
            (long) successRequests,
            (long) clientErrors,
            (long) serverErrors,
            (int) activeConnections,
            (int) maxConnections
        );
    }

    /**
     * Get overall health status from actuator
     */
    private HealthStatus getHealthStatus() {
        try {
            HealthComponent health = healthEndpoint.health();
            Status status = health.getStatus();

            Map<String, String> componentStatuses = new HashMap<>();
            if (health instanceof Health h) {
                Map<String, Object> details = h.getDetails();
                if (details != null) {
                    details.forEach((key, value) -> {
                        if (value instanceof Health componentHealth) {
                            componentStatuses.put(key, componentHealth.getStatus().getCode());
                        }
                    });
                }
            }

            return new HealthStatus(
                status.getCode(),
                status.equals(Status.UP),
                componentStatuses
            );
        } catch (Exception e) {
            log.warn("Could not retrieve health status", e);
            return new HealthStatus("UNKNOWN", false, Map.of());
        }
    }

    /**
     * Get application info
     */
    private ApplicationInfo getApplicationInfo() {
        String appName = buildProperties.map(BuildProperties::getName).orElse("Legal Aid Backend");
        String appVersion = buildProperties.map(BuildProperties::getVersion).orElse("1.0.0");
        String buildTime = buildProperties.map(bp -> bp.getTime().toString()).orElse("N/A");

        String springBootVersion = org.springframework.boot.SpringBootVersion.getVersion();
        String javaVersion = System.getProperty("java.version");

        return new ApplicationInfo(
            appName,
            appVersion,
            buildTime,
            springBootVersion,
            javaVersion,
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch")
        );
    }

    // Helper methods
    private double getMetricValue(String metricName) {
        try {
            MetricsEndpoint.MetricDescriptor descriptor = metricsEndpoint.metric(metricName, null);
            if (descriptor != null && !descriptor.getMeasurements().isEmpty()) {
                return descriptor.getMeasurements().get(0).getValue();
            }
        } catch (Exception e) {
            log.trace("Metric {} not available", metricName);
        }
        return 0.0;
    }

    private double getMetricValue(String metricName, String tagKey, String tagValue) {
        try {
            List<String> tags = List.of(tagKey + ":" + tagValue);
            MetricsEndpoint.MetricDescriptor descriptor = metricsEndpoint.metric(metricName, tags);
            if (descriptor != null && !descriptor.getMeasurements().isEmpty()) {
                return descriptor.getMeasurements().get(0).getValue();
            }
        } catch (Exception e) {
            log.trace("Metric {} with tag {}:{} not available", metricName, tagKey, tagValue);
        }
        return 0.0;
    }

    private double bytesToMB(long bytes) {
        return Math.round(bytes / (1024.0 * 1024.0) * 100.0) / 100.0;
    }

    private double bytesToGB(double bytes) {
        return Math.round(bytes / (1024.0 * 1024.0 * 1024.0) * 100.0) / 100.0;
    }

    private String formatUptime(Duration uptime) {
        long days = uptime.toDays();
        long hours = uptime.toHoursPart();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();

        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    // Record DTOs for metrics
    public record ActuatorSystemMetrics(
        CpuMetrics cpu,
        MemoryMetrics memory,
        JvmMetrics jvm,
        DiskMetrics disk,
        ThreadMetrics threads,
        GcMetrics gc,
        HttpMetrics http,
        HealthStatus health,
        ApplicationInfo application
    ) {}

    public record CpuMetrics(
        double systemCpuUsage,
        double processCpuUsage,
        int availableProcessors,
        double systemLoadAverage
    ) {}

    public record MemoryMetrics(
        double heapUsedMB,
        double heapMaxMB,
        double heapCommittedMB,
        double heapUsagePercent,
        double nonHeapUsedMB,
        double nonHeapCommittedMB,
        double directBufferMB,
        double mappedBufferMB
    ) {}

    public record JvmMetrics(
        String jvmVersion,
        String jvmVendor,
        String jvmName,
        long uptimeDays,
        long uptimeHours,
        long uptimeMinutes,
        String startTime,
        String formattedUptime
    ) {}

    public record DiskMetrics(
        double freeSpaceGB,
        double totalSpaceGB,
        double usedSpaceGB,
        double usagePercent
    ) {}

    public record ThreadMetrics(
        int liveThreads,
        int daemonThreads,
        int peakThreads,
        long totalStartedThreads,
        int runnableThreads,
        int blockedThreads,
        int waitingThreads,
        int timedWaitingThreads
    ) {}

    public record GcMetrics(
        long totalCollections,
        long totalTimeMs,
        List<GcCollectorInfo> collectors,
        double maxPauseMs,
        long pauseCount
    ) {}

    public record GcCollectorInfo(
        String name,
        long count,
        long timeMs
    ) {}

    public record HttpMetrics(
        long totalRequests,
        double avgResponseTimeMs,
        long successfulRequests,
        long clientErrors,
        long serverErrors,
        int activeConnections,
        int maxConnections
    ) {}

    public record HealthStatus(
        String status,
        boolean healthy,
        Map<String, String> components
    ) {}

    public record ApplicationInfo(
        String name,
        String version,
        String buildTime,
        String springBootVersion,
        String javaVersion,
        String osName,
        String osVersion,
        String osArch
    ) {}
}

