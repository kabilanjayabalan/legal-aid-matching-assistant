package com.legalaid.backend.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Custom Logback appender that persists selected log events to the "logs" table.
 * <p>
 * Uses virtual threads and batch inserts for high performance, non-blocking logging.
 * Only events from the application's own packages (com.legalaid...)
 * and events whose message contains "Tomcat started on port" are stored.
 */
public class DBLogAppender extends AppenderBase<ILoggingEvent> {

    private String url;
    private String user;
    private String password;

    // Queue for buffering log events
    private final BlockingQueue<LogEvent> logQueue = new LinkedBlockingQueue<>(10000);

    // Virtual thread executor for async batch processing
    private ExecutorService virtualThreadExecutor;

    // Batch configuration
    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL_MS = 2000;

    private volatile boolean running = false;
    private volatile boolean dbAvailable = true;

    // Internal record to hold log event data
    private record LogEvent(long timestamp, String level, String loggerName, String message) {}

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void start() {
        try {
            // Validate configuration
            if (url == null || url.isEmpty() || 
                user == null || user.isEmpty() || user.contains("UNDEFINED") ||
                password == null || password.isEmpty() || password.contains("UNDEFINED")) {
                addWarn("DBLogAppender: Database credentials not properly configured. DB logging will be disabled.");
                dbAvailable = false;
                super.start();
                return;
            }

            // Ensure PostgreSQL driver is loaded
            Class.forName("org.postgresql.Driver");

            // Create virtual thread executor
            virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

            running = true;

            // Start the batch flush worker using a virtual thread
            virtualThreadExecutor.submit(this::batchFlushWorker);

            super.start();
            addInfo("DBLogAppender started with virtual threads and batch processing");
        } catch (ClassNotFoundException e) {
            addError("PostgreSQL JDBC Driver not found", e);
            dbAvailable = false;
        }
    }

    @Override
    public void stop() {
        running = false;

        // Flush remaining logs
        flushQueue();

        if (virtualThreadExecutor != null) {
            virtualThreadExecutor.shutdown();
            try {
                if (!virtualThreadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    virtualThreadExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                virtualThreadExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        super.stop();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!isStarted() || !dbAvailable) {
            return;
        }

        String loggerName = eventObject.getLoggerName();
        String message = eventObject.getFormattedMessage();

        boolean isAppLogger = loggerName != null && loggerName.startsWith("com.legalaid");
        boolean isTomcatStarted = message != null && message.contains("Tomcat started on port");

        // Only persist the events the user cares about
        if (!isAppLogger && !isTomcatStarted) {
            return;
        }

        // Non-blocking add to queue (drops if queue is full to prevent backpressure)
        LogEvent logEvent = new LogEvent(
                eventObject.getTimeStamp(),
                eventObject.getLevel().toString(),
                loggerName,
                message
        );

        if (!logQueue.offer(logEvent)) {
            addWarn("Log queue full, dropping log event");
        }
    }

    /**
     * Background worker that flushes logs in batches using virtual threads.
     */
    private void batchFlushWorker() {
        while (running) {
            try {
                Thread.sleep(FLUSH_INTERVAL_MS);
                flushQueue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Flushes all queued log events to the database in a batch.
     */
    private void flushQueue() {
        List<LogEvent> batch = new ArrayList<>();
        logQueue.drainTo(batch, BATCH_SIZE);

        if (batch.isEmpty()) {
            return;
        }

        // Execute batch insert using a virtual thread
        virtualThreadExecutor.submit(() -> batchInsert(batch));
    }

    /**
     * Performs batch insert of log events into the database.
     */
    private void batchInsert(List<LogEvent> batch) {
        if (!dbAvailable || url == null || user == null || password == null) {
            return;
        }

        String sql = "INSERT INTO logs (log_timestamp, level, logger, message) VALUES (?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            connection.setAutoCommit(false);

            for (LogEvent event : batch) {
                stmt.setTimestamp(1, new Timestamp(event.timestamp()));
                stmt.setString(2, event.level());
                stmt.setString(3, event.loggerName());
                stmt.setString(4, event.message());
                stmt.addBatch();
            }

            stmt.executeBatch();
            connection.commit();

        } catch (Exception e) {
            // Silently disable DB logging if connection fails repeatedly
            // This prevents log spam when database is unavailable (e.g., during tests)
            dbAvailable = false;
            // Only log the first error to avoid log spam
            if (batch.size() > 0) {
                // Use System.err to avoid recursive logging
                System.err.println("DBLogAppender: Database connection failed. DB logging disabled. Error: " + e.getMessage());
            }
        }
    }
}


