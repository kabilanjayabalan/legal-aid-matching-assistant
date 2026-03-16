package com.legalaid.backend.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.legalaid.backend.model.Appointment;
import com.legalaid.backend.model.AppointmentStatus;

public interface AppointmentRepository
                extends JpaRepository<Appointment, Long> {

        List<Appointment> findByRequesterIdOrReceiverId(
                        Long requesterId, Long receiverId);

        List<Appointment> findByRequesterIdInOrReceiverIdIn(
                        Collection<Long> requesterIds, Collection<Long> receiverIds);

        List<Appointment> findByMatchId(Long matchId);

        long countByStatus(AppointmentStatus status);
        long countByStatusAndAppointmentDateGreaterThanEqual(
            AppointmentStatus status,
            LocalDate date
        );

        Page<Appointment> findByStatus(
            AppointmentStatus status,
            Pageable pageable
        );

        Page<Appointment> findByStatusAndMatchIdContainingIgnoreCase(
            AppointmentStatus status,
            Long matchId,
            Pageable pageable
        );

        Page<Appointment> findByMatchIdContainingIgnoreCase(
            long matchId, Pageable pageable);
        List<Appointment> findByStatus(AppointmentStatus status);
}
