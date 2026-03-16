package com.legalaid.backend.service;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.legalaid.backend.model.Appointment;
import com.legalaid.backend.model.AppointmentStatus;
import com.legalaid.backend.repository.AppointmentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentAutoCompleteService {

    private final AppointmentRepository appointmentRepository;

    @Scheduled(fixedRate = 5 * 60 * 1000) // every 5 minutes
    @Transactional
    public void autoCompleteAppointments() {

        List<Appointment> confirmedAppointments =
                appointmentRepository.findByStatus(AppointmentStatus.CONFIRMED);

        ZonedDateTime now = ZonedDateTime.now();

        for (Appointment appointment : confirmedAppointments) {

            try {
                ZonedDateTime appointmentEndTime = calculateAppointmentEndTime(appointment);

                if (appointmentEndTime.isBefore(now)) {
                    appointment.setStatus(AppointmentStatus.COMPLETED);
                    log.info("Auto-completed appointment ID {}", appointment.getId());
                }

            } catch (Exception e) {
                log.error("Failed to process appointment ID {}", appointment.getId(), e);
            }
        }
    }

    private ZonedDateTime calculateAppointmentEndTime(Appointment appointment) {

        // Parse time slot (e.g. "5:00 PM")
        DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

LocalTime startTime = LocalTime.parse(
        appointment.getTimeSlot().trim(),
        formatter
);

        ZoneId zoneId = resolveZoneId(appointment.getTimeZone());

        ZonedDateTime startDateTime = ZonedDateTime.of(
                appointment.getAppointmentDate(),
                startTime,
                zoneId
        );

        return startDateTime.plusMinutes(appointment.getDurationMinutes());
    }
    private ZoneId resolveZoneId(String rawTimeZone) {

    if (rawTimeZone == null || rawTimeZone.isBlank()) {
        return ZoneId.systemDefault();
    }

    // Example: "America/New_York (EST)" → "America/New_York"
    String zoneId = rawTimeZone.split(" ")[0];

    try {
        return ZoneId.of(zoneId);
    } catch (DateTimeException ex) {
        log.warn("Invalid timezone '{}', falling back to system default", rawTimeZone);
        return ZoneId.systemDefault();
    }
}

}

