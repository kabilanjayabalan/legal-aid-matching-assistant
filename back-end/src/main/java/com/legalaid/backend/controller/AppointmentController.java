package com.legalaid.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.legalaid.backend.dto.AppointmentDTO;
import com.legalaid.backend.model.Appointment;
import com.legalaid.backend.model.AppointmentStatus;
import com.legalaid.backend.model.LawyerProfile;
import com.legalaid.backend.model.Match;
import com.legalaid.backend.model.NGOProfile;
import com.legalaid.backend.model.Notification;
import com.legalaid.backend.model.ProviderType;
import com.legalaid.backend.model.User;
import com.legalaid.backend.repository.AppointmentRepository;
import com.legalaid.backend.repository.LawyerProfileRepository;
import com.legalaid.backend.repository.MatchRepository;
import com.legalaid.backend.repository.NGOProfileRepository;
import com.legalaid.backend.repository.NotificationRepository;
import com.legalaid.backend.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentRepository repo;
    private final MatchRepository matchRepository;
    private final NotificationRepository notificationRepository;
    private final NGOProfileRepository ngoProfileRepository;
    private final LawyerProfileRepository lawyerProfileRepository;
    private final UserRepository userRepository;

    public AppointmentController(
            AppointmentRepository repo,
            MatchRepository matchRepository,
            NotificationRepository notificationRepository,
            LawyerProfileRepository lawyerProfileRepository,
            NGOProfileRepository ngoProfileRepository,
            UserRepository userRepository
        ) {
        this.repo = repo;
        this.matchRepository = matchRepository;
        this.notificationRepository = notificationRepository;
        this.lawyerProfileRepository = lawyerProfileRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public Appointment create(@RequestBody Appointment appointment) {

        // Always set backend-controlled fields
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setCreatedAt(LocalDateTime.now());
        // Set lastModifiedBy to the requester (citizen who created it)
        appointment.setLastModifiedBy(appointment.getRequesterId());

        // Derive receiverId from match (lawyer / NGO)
        Long matchId = appointment.getMatchId();
        if (matchId != null && !matchId.equals(0L)) {
            try {
                Integer matchPk = matchId.intValue();
                Optional<Match> matchOpt = matchRepository.findById(matchPk);
                if (matchOpt.isPresent()) {
                    Match match = matchOpt.get();
                    Long receiverUserId = null;
                    if (match.getProviderId() != null) {
                        if (match.getProviderType() == ProviderType.LAWYER) {
                            LawyerProfile lawyer = lawyerProfileRepository
                                .findById(match.getProviderId())
                                .orElseThrow(() -> new RuntimeException("Lawyer profile not found"));

                            receiverUserId = Long.valueOf(lawyer.getUser().getId());
                        } else if (match.getProviderType() == ProviderType.NGO) {
                            NGOProfile ngo = ngoProfileRepository
                                .findById(match.getProviderId())
                                .orElseThrow(() -> new RuntimeException("NGO profile not found"));

                            receiverUserId = Long.valueOf(ngo.getUser().getId());
                        }
                    }
                    appointment.setReceiverId(receiverUserId);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // 1️⃣ Save appointment
        Appointment savedAppointment = repo.save(appointment);
        String requesterName = userRepository
            .findById(savedAppointment.getRequesterId().intValue())
            .map(User::getFullName)
            .orElse("Unknown User");

        // Fetch receiver name
        String receiverName = userRepository
            .findById(savedAppointment.getReceiverId().intValue())
            .map(User::getFullName)
            .orElse("Unknown Provider");
        log.info("Appointment scheduled: {} scheduled a call with {} on {} at {}", requesterName, receiverName,
            savedAppointment.getAppointmentDate(), savedAppointment.getTimeSlot());

        // 2️⃣ CREATE NOTIFICATION (THIS WAS MISSING ❌)
        if (savedAppointment.getReceiverId() != null) {
            Notification notification = new Notification();
            notification.setUserId(savedAppointment.getReceiverId());
            notification.setType("APPOINTMENT");
            notification.setMessage("New appointment request received");
            notification.setReferenceId(savedAppointment.getId().toString());
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            notificationRepository.save(notification);
        }

        return savedAppointment;
    }

    @GetMapping("/my")
    public List<AppointmentDTO> myAppointments(@RequestParam Long userId) {

        List<Appointment> appointments =
                    repo.findByRequesterIdOrReceiverId(userId, userId);

        return appointments.stream()
                .map(appt -> {
                    // Safely resolve requester name – fallback to "Unknown" if ID is non‑numeric or missing
                    String requesterName = "Unknown";
                    if (appt.getRequesterId() != null) {
                        try {
                            Integer requesterPk = appt.getRequesterId().intValue();
                            requesterName = userRepository
                                    .findById(requesterPk)
                                    .map(User::getFullName)
                                    .orElse("Unknown");
                        } catch (NumberFormatException ignored) {
                            // If requesterId is something like "temp-0", skip lookup and keep "Unknown"
                        }
                    }

                    // Safely resolve receiver name
                    String receiverName = "Unknown";
                    if (appt.getReceiverId() != null) {
                        try {
                            Integer receiverPk = appt.getReceiverId().intValue();
                            receiverName = userRepository
                                    .findById(receiverPk)
                                    .map(User::getFullName)
                                    .orElse("Unknown");
                        } catch (NumberFormatException ignored) {
                            // Non‑numeric receiverId – leave as "Unknown"
                        }
                    }

                    String caseTitle = null;
                    String caseNumber = null;

                    if (appt.getMatchId() != null) {
                        try {
                            Integer matchPk = appt.getMatchId().intValue();
                            Match match = matchRepository.findById(matchPk).orElse(null);

                            if (match != null && match.getCaseObj() != null) {
                                caseTitle = match.getCaseObj().getTitle();
                                caseNumber = match.getCaseObj().getCaseNumber();
                            }

                        } catch (NumberFormatException ignored) {
                        }
                    }

                    return new AppointmentDTO(
                            appt.getId(),
                            appt.getMatchId(),
                            appt.getStatus(),
                            appt.getAppointmentDate(),
                            appt.getTimeSlot(),
                            appt.getTimeZone(),
                            appt.getDurationMinutes(),
                            caseTitle,
                            caseNumber,
                            appt.getRequesterId(),
                            appt.getReceiverId(),
                            requesterName,
                            receiverName,
                            appt.getLastModifiedBy(),
                            appt.getCreatedAt()
                    );
                })
                .toList();
    }

    @PutMapping("/{matchId}/update")
    public Appointment updateStatusByMatchId(
            @PathVariable Long matchId,
            @RequestParam AppointmentStatus status) {

        List<Appointment> appointments = repo.findByMatchId(matchId);

        if (appointments.isEmpty()) {
            throw new RuntimeException("Appointment not found for matchId: " + matchId);
        }

        Appointment appt = appointments.get(appointments.size() - 1);
        appt.setStatus(status);

        return repo.save(appt);
    }

    @PutMapping("/{matchId}/reschedule")
    public Appointment rescheduleAppointment(
            @PathVariable Long matchId,
            @RequestBody Appointment updatedAppointment) {

        List<Appointment> appointments = repo.findByMatchId(matchId);

        if (appointments.isEmpty()) {
            throw new RuntimeException("Appointment not found for matchId: " + matchId);
        }

        Appointment appt = appointments.get(appointments.size() - 1);

        if (updatedAppointment.getAppointmentDate() != null)
            appt.setAppointmentDate(updatedAppointment.getAppointmentDate());
        if (updatedAppointment.getTimeSlot() != null)
            
            appt.setTimeSlot(updatedAppointment.getTimeSlot());
        if (updatedAppointment.getTimeZone() != null)
            appt.setTimeZone(updatedAppointment.getTimeZone());
        if (updatedAppointment.getDurationMinutes() != null)
            appt.setDurationMinutes(updatedAppointment.getDurationMinutes());
        if (updatedAppointment.getRemind15Min() != null)
            appt.setRemind15Min(updatedAppointment.getRemind15Min());
        if (updatedAppointment.getRemind1Hour() != null)
            appt.setRemind1Hour(updatedAppointment.getRemind1Hour());

        // Set status back to PENDING when rescheduled
        appt.setStatus(AppointmentStatus.PENDING);

        // Determine who is rescheduling: if we're notifying requester, receiver is rescheduling and vice versa
        // For now, let's toggle: if lastModifiedBy was requester, now it's receiver
        if (appt.getLastModifiedBy() != null && appt.getLastModifiedBy().equals(appt.getRequesterId())) {
            appt.setLastModifiedBy(appt.getReceiverId());
        } else {
            appt.setLastModifiedBy(appt.getRequesterId());
        }

        Appointment savedAppointment = repo.save(appt);
        log.info("Appointment rescheduled: ID {} to {} at {}", savedAppointment.getId(),
            savedAppointment.getAppointmentDate(), savedAppointment.getTimeSlot());
        // Send notification to requester about rescheduling
        if (savedAppointment.getRequesterId() != null) {
            Notification notification = new Notification();
            notification.setUserId(savedAppointment.getRequesterId());
            notification.setType("APPOINTMENT");
            notification.setMessage("Your appointment has been rescheduled");
            notification.setReferenceId(savedAppointment.getId().toString());
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            notificationRepository.save(notification);
        }

        return savedAppointment;
    }

    @PutMapping("/{matchId}/accept")
    public Appointment acceptAppointment(@PathVariable Long matchId) {

        List<Appointment> appointments = repo.findByMatchId(matchId);

        if (appointments.isEmpty()) {
            throw new RuntimeException("Appointment not found for matchId: " + matchId);
        }

        Appointment appt = appointments.get(appointments.size() - 1);
        appt.setStatus(AppointmentStatus.CONFIRMED);

        Appointment savedAppointment = repo.save(appt);
        log.info("Appointment accepted: ID {}", savedAppointment.getId());
        // Send notification to requester about acceptance
        if (savedAppointment.getRequesterId() != null) {
            Notification notification = new Notification();
            notification.setUserId(savedAppointment.getRequesterId());
            notification.setType("APPOINTMENT");
            notification.setMessage("Your appointment has been accepted");
            notification.setReferenceId(savedAppointment.getId().toString());
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            notificationRepository.save(notification);
        }

        return savedAppointment;
    }

    @PutMapping("/{matchId}/cancel")
    public Appointment cancelAppointment(@PathVariable Long matchId) {

        List<Appointment> appointments = repo.findByMatchId(matchId);

        if (appointments.isEmpty()) {
            throw new RuntimeException("Appointment not found for matchId: " + matchId);
        }

        Appointment appt = appointments.get(appointments.size() - 1);
        appt.setStatus(AppointmentStatus.CANCELLED);

        Appointment savedAppointment = repo.save(appt);
        log.info("Appointment cancelled: ID {}", savedAppointment.getId());

        // Send notification to requester about cancellation
        if (savedAppointment.getRequesterId() != null) {
            Notification notification = new Notification();
            notification.setUserId(savedAppointment.getRequesterId());
            notification.setType("APPOINTMENT");
            notification.setMessage("Your appointment has been cancelled");
            notification.setReferenceId(savedAppointment.getId().toString());
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            notificationRepository.save(notification);
        }

        return savedAppointment;
    }
}
