package com.legalaid.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppointmentStatsDTO {

    private long total;
    private long upcoming;
    private long pending;
    private long confirmed;
    private long completed;
    private long cancelled;
}

