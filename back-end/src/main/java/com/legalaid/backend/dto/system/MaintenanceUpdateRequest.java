package com.legalaid.backend.dto.system;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MaintenanceUpdateRequest(

    @NotNull
    boolean enabled,

    LocalDateTime start,

    LocalDateTime end,

    @Size(max = 300)
    String message
) {}
