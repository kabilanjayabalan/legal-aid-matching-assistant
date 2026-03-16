package com.legalaid.backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AppointmentAnalyticsResponse {

    private long totalAppointments;
    private long activeAppointments;
    private long completedAppointments;
    private long cancelledAppointments;
}
