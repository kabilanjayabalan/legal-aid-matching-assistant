package com.legalaid.backend.service.analytics;

import org.springframework.stereotype.Service;

import com.legalaid.backend.dto.analytics.AppointmentAnalyticsResponse;
import com.legalaid.backend.model.AppointmentStatus;
import com.legalaid.backend.repository.AppointmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentAnalyticsService {

    private final AppointmentRepository appointmentRepository;

    public AppointmentAnalyticsResponse getAppointmentAnalytics() {

        long total = appointmentRepository.count();

        long active =
                appointmentRepository.countByStatus(AppointmentStatus.CONFIRMED);

        long completed =
                appointmentRepository.countByStatus(AppointmentStatus.COMPLETED);

        long cancelled =
                appointmentRepository.countByStatus(AppointmentStatus.CANCELLED);

        return new AppointmentAnalyticsResponse(
                total,
                active,
                completed,
                cancelled
        );
    }
}
