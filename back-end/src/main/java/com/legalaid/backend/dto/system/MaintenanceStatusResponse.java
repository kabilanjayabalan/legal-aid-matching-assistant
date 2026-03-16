package com.legalaid.backend.dto.system;

import java.time.LocalDateTime;

public record MaintenanceStatusResponse(
    boolean enabled,
    LocalDateTime start,
    LocalDateTime end,
    String message
) {}
