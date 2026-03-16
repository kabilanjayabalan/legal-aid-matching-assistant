package com.legalaid.backend.config;

import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Configuration class for Spring Boot Actuator endpoints
 * Ensures that required actuator endpoints are available as Spring beans
 */
@Configuration
public class ActuatorConfig {

    /**
     * Creates MetricsEndpoint bean for accessing metrics programmatically
     */
    @Bean
    public MetricsEndpoint metricsEndpoint(MeterRegistry meterRegistry) {
        return new MetricsEndpoint(meterRegistry);
    }
}

