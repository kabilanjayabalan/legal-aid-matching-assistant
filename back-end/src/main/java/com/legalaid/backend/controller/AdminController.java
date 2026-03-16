package com.legalaid.backend.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.legalaid.backend.dto.AdminManagementResponse;
import com.legalaid.backend.dto.AppointmentDTO;
import com.legalaid.backend.dto.AppointmentStatsDTO;
import com.legalaid.backend.dto.CaseMonitoringResponse;
import com.legalaid.backend.dto.MatchMonitoringDTO;
import com.legalaid.backend.dto.MatchStatsDTO;
import com.legalaid.backend.dto.analytics.CaseStatsResponse;
import com.legalaid.backend.model.Appointment;
import com.legalaid.backend.model.AppointmentStatus;
import com.legalaid.backend.model.LawyerProfile;
import com.legalaid.backend.model.NGOProfile;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.model.UserStatus;
import com.legalaid.backend.repository.LawyerProfileRepository;
import com.legalaid.backend.repository.NGOProfileRepository;
import com.legalaid.backend.repository.UserRepository;
import com.legalaid.backend.service.ActuatorMetricsService;
import com.legalaid.backend.service.AdminUserService;
import com.legalaid.backend.service.AuthService;
import com.legalaid.backend.service.LogCleanupService;
import com.legalaid.backend.service.SystemHealthService;
import com.legalaid.backend.service.analytics.CaseAnalyticsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final LawyerProfileRepository lawyerProfileRepository;
    private final NGOProfileRepository ngoProfileRepository;
    private final CaseAnalyticsService caseAnalyticsService;
    private final SystemHealthService systemHealthService;
    private final ActuatorMetricsService actuatorMetricsService;
    private final AdminUserService adminUserService;
    private final com.legalaid.backend.repository.AppointmentRepository appointmentRepository;
    private final com.legalaid.backend.repository.MatchRepository matchRepository;
    private final com.legalaid.backend.service.analytics.MatchAnalyticsService matchAnalyticsService;
    private final LogCleanupService logCleanupService;

    public AdminController(AuthService authService, UserRepository userRepository,
                           LawyerProfileRepository lawyerProfileRepository,
                           NGOProfileRepository ngoProfileRepository,
                           CaseAnalyticsService caseAnalyticsService,
                           SystemHealthService systemHealthService,
                           ActuatorMetricsService actuatorMetricsService,
                        AdminUserService adminUserService,
                        com.legalaid.backend.repository.AppointmentRepository appointmentRepository,
                        com.legalaid.backend.repository.MatchRepository matchRepository,
                        com.legalaid.backend.service.analytics.MatchAnalyticsService matchAnalyticsService,
                        LogCleanupService logCleanupService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.lawyerProfileRepository = lawyerProfileRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.caseAnalyticsService = caseAnalyticsService;
        this.systemHealthService = systemHealthService;
        this.actuatorMetricsService = actuatorMetricsService;
        this.adminUserService = adminUserService;
        this.appointmentRepository = appointmentRepository;
        this.matchRepository = matchRepository;
        this.matchAnalyticsService = matchAnalyticsService;
        this.logCleanupService = logCleanupService;
    }
    // user approval endpoints
    @PostMapping("/approve/{username}")
    public ResponseEntity<?> approveUser(@PathVariable String username) {
        log.info("Approving user with username: {}", username);
        User user = authService.approveUserByUsername(username);
        log.info("User approved successfully: {}", user.getEmail());
        return ResponseEntity.ok().body(new MessageResponse("User approved: " + user.getEmail()));
    }

    @PostMapping("/reject/{username}")
    public ResponseEntity<?> rejectUser(@PathVariable String username) {
        log.info("Rejecting user with username: {}", username);
        User user = authService.rejectUserByUsername(username);
        log.info("User rejected successfully: {}", user.getEmail());
        return ResponseEntity.ok().body(new MessageResponse("User rejected: " + user.getEmail()));
    }

    @PutMapping("/lawyers-ngos/{username}")
    public ResponseEntity<?> updateLawyerOrNgoProfile(@PathVariable String username,
                                                      @RequestBody LawyerNgoProfileUpdate payload) {
        log.info("Updating lawyer/NGO profile for username: {}", username);
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            log.warn("User not found for username: {}", username);
            return ResponseEntity.badRequest().body(new MessageResponse("User not found: " + username));
        }

        if (user.getRole() == Role.LAWYER) {
            log.info("Updating lawyer profile for username: {}", username);
            LawyerProfile profile = lawyerProfileRepository.findByUser(user).orElse(new LawyerProfile());
            profile.setUser(user);
            if (payload.name != null) profile.setName(payload.name);
            if (payload.expertise != null) profile.setExpertise(payload.expertise);
            if (payload.location != null) profile.setLocation(payload.location);
            if (payload.contactInfo != null) profile.setContactInfo(payload.contactInfo);
            if (payload.language != null) profile.setLanguage(payload.language);
            LawyerProfile saved = lawyerProfileRepository.save(profile);
            log.info("Lawyer profile updated successfully for username: {}", username);
            return ResponseEntity.ok(saved);
        }

        if (user.getRole() == Role.NGO) {
            log.info("Updating NGO profile for username: {}", username);
            NGOProfile profile = ngoProfileRepository.findByUser(user).orElse(new NGOProfile());
            profile.setUser(user);
            if (payload.organization != null) profile.setOrganization(payload.organization);
            if (payload.contactInfo != null) profile.setContactInfo(payload.contactInfo);
            if (payload.location != null) profile.setLocation(payload.location);
            if (payload.language != null) profile.setLanguage(payload.language);
            NGOProfile saved = ngoProfileRepository.save(profile);
            log.info("NGO profile updated successfully for username: {}", username);
            return ResponseEntity.ok(saved);
        }

        log.warn("Invalid role for profile update. Username: {}", username);
        return ResponseEntity.badRequest().body(new MessageResponse("Only lawyers and NGOs can have profiles updated by admin."));
    }
    //profile verification endpoints
    @PutMapping("/verify/{username}/true")
    public ResponseEntity<?> markProfileVerified(@PathVariable String username) {
        log.info("Marking profile as verified for username: {}", username);
        return setVerificationStatus(username, true);
    }

    @PutMapping("/verify/{username}/false")
    public ResponseEntity<?> markProfileUnverified(@PathVariable String username) {
        log.info("Marking profile as unverified for username: {}", username);
        return setVerificationStatus(username, false);
    }

    
    @GetMapping("/users")
    public ResponseEntity<Page<AdminManagementResponse>> getAllUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) UserStatus status
        ) {

    org.springframework.data.domain.Page<AdminManagementResponse> result =
            adminUserService.getUsers(page, size, search, role, status);

    return ResponseEntity.ok(result);
}


    @PutMapping("/users/{userId}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable Integer userId) {
        adminUserService.changeStatus(userId, UserStatus.ACTIVE);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{userId}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Integer userId) {
        adminUserService.changeStatus(userId, UserStatus.INACTIVE);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{userId}/suspend")
    public ResponseEntity<Void> suspendUser(@PathVariable Integer userId) {
        adminUserService.changeStatus(userId, UserStatus.SUSPENDED);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{userId}/block")
    public ResponseEntity<Void> blockUser(@PathVariable Integer userId) {
        adminUserService.changeStatus(userId, UserStatus.BLOCKED);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/cases/stats")
    public CaseStatsResponse getCaseStats() {
        return caseAnalyticsService.getCaseStats();
    }
    @GetMapping("cases/monitoring")
    public Page<CaseMonitoringResponse> getCasesForMonitoring(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {

        Pageable pageable = PageRequest.of(page, size);

        return caseAnalyticsService.getPaginatedCases(search, status, pageable);
    }

    @GetMapping("/users/lawyers-ngos")
    public ResponseEntity<Page<UserResponse>> getLawyersAndNgos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Role role,
            
            @RequestParam(required = false) String status
    ) {

        // Convert status → approved(Boolean)
        Boolean approved = null;
        boolean pendingOnly = false;

        if (status != null && !status.isBlank()) {
            switch (status.toUpperCase()) {
                case "APPROVED" -> approved = true;
                case "REJECTED" -> approved = false;
                case "PENDING" -> pendingOnly = true;
            }
        }
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<UserResponse> response =
                userRepository
                        .findLawyersAndNgos(role, approved,pendingOnly, pageable)
                        .map(user -> new UserResponse(
                                user.getUsername(),
                                user.getEmail(),
                                user.getFullName(),
                                user.getRole().name(),
                                user.getApproved(),
                                user.getCreatedAt().toString()
                        ));

        return ResponseEntity.ok(response);
    }


 @GetMapping("/lawyer-ngo")
public ResponseEntity<?> getLawyersAndNgos(
        @RequestParam(required = false) Role role,
        @RequestParam(defaultValue = "false") boolean pendingOnly,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
) {

    Pageable pageable =
            PageRequest.of(page, size, Sort.by("createdAt").descending());

    Page<LawyerProfile> lawyerPage = Page.empty();
    Page<NGOProfile> ngoPage = Page.empty();

    // ---------- LAWYERS ----------
    if (role == null || role == Role.LAWYER) {
        lawyerPage = pendingOnly
                ? lawyerProfileRepository.findByVerifiedIsNull(pageable)
                : lawyerProfileRepository.findAll(pageable);
    }

    // ---------- NGOs ----------
    if (role == null || role == Role.NGO) {
        ngoPage = pendingOnly
                ? ngoProfileRepository.findByVerifiedIsNull(pageable)
                : ngoProfileRepository.findAll(pageable);
    }

    List<Object> combined = new ArrayList<>();

    lawyerPage.forEach(profile -> {
        User u = profile.getUser();
        combined.add(new LawyerProfileResponse(
                u.getUsername(),
                u.getEmail(),
                u.getFullName(),
                "LAWYER",
                u.getCreatedAt().toString(),
                profile.getName(),
                profile.getExpertise(),
                profile.getLocation(),
                profile.getContactInfo(),
                profile.getBarRegistrationNo(),
                profile.getSpecialization(),
                profile.getExperienceYears(),
                profile.getCity(),
                profile.getBio(),
                profile.getLanguage(),
                profile.getVerified()
        ));
    });

    ngoPage.forEach(profile -> {
        User u = profile.getUser();
        combined.add(new NGOProfileResponse(
                u.getUsername(),
                u.getEmail(),
                u.getFullName(),
                "NGO",
                u.getCreatedAt().toString(),
                profile.getOrganization(),
                profile.getContactInfo(),
                profile.getLocation(),
                profile.getNgoName(),
                profile.getRegistrationNo(),
                profile.getCity(),
                profile.getWebsite(),
                profile.getDescription(),
                profile.getLanguage(),
                profile.getVerified()
        ));
    });

    return ResponseEntity.ok(
            Map.of(
                    "content", combined,
                    "page", page,
                    "size", size,
                    "totalElements",
                        lawyerPage.getTotalElements() + ngoPage.getTotalElements(),
                    "totalPages",
                        Math.max(lawyerPage.getTotalPages(), ngoPage.getTotalPages())
            )
    );
}

    @GetMapping("/system-health")
    public ResponseEntity<?> getSystemHealth() {
        log.info("Fetching system health metrics");
        SystemHealthService.SystemHealthDTO health = systemHealthService.getSystemHealth();
        log.info("System health metrics retrieved successfully");
        return ResponseEntity.ok(health);
    }

    @GetMapping("/system-load-over-time")
    public ResponseEntity<List<SystemHealthService.SystemLoadPointDTO>> getSystemLoadOverTime() {
        log.info("Fetching system load over time");
        List<SystemHealthService.SystemLoadPointDTO> data = systemHealthService.getSystemLoadOverTime();
        log.info("System load over time retrieved successfully, {} points", data.size());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/service-activity")
    public ResponseEntity<List<SystemHealthService.ServiceActivityDTO>> getServiceActivityBreakdown() {
        log.info("Fetching service activity breakdown");
        List<SystemHealthService.ServiceActivityDTO> data = systemHealthService.getServiceActivityBreakdown();
        log.info("Service activity breakdown retrieved successfully, {} entries", data.size());
        return ResponseEntity.ok(data);
    }

    /**
     * Get comprehensive actuator-based system metrics
     * Includes CPU, Memory, JVM, Disk, Threads, GC, HTTP, and Health information
     */
    @GetMapping("/actuator-metrics")
    public ResponseEntity<ActuatorMetricsService.ActuatorSystemMetrics> getActuatorMetrics() {
        log.info("Fetching comprehensive actuator system metrics");
        ActuatorMetricsService.ActuatorSystemMetrics metrics = actuatorMetricsService.getSystemMetrics();
        log.info("Actuator system metrics retrieved successfully");
        return ResponseEntity.ok(metrics);
    }

    // DTO for detailed lawyer profile response
    static record LawyerProfileResponse(
            String username,
            String email,
            String fullName,
            String role,
            String createdAt,
            String name,
            String expertise,
            String location,
            String contactInfo,
            String barRegistrationNo,
            String specialization,
            Integer experienceYears,
            String city,
            String bio,
            String language,
            Boolean verified
    ) {}

    // DTO for detailed NGO profile response
    static record NGOProfileResponse(
            String username,
            String email,
            String fullName,
            String role,
            String createdAt,
            String organization,
            String contactInfo,
            String location,
            String ngoName,
            String registrationNo,
            String city,
            String website,
            String description,
            String language,
            Boolean verified
    ) {}

    // DTO for summary of lawyer/NGO profile (excluding sensitive fields)
    static record UserProfileSummary(
            String username,
            String email,
            String fullName,
            String role,
            String lawyerName,
            String location,
            String contactInfo,
            String organization,
            String ngoName
    ) {}

    // DTO for exposing user data without password
    static record UserResponse(
            String username,
            String email,
            String fullName,
            String role,
            Boolean approved,
            String createdAt
    ) {}



    record MessageResponse(String message) {}

    /**
     * DTO for updating lawyer/NGO profiles. Fields are optional; only provided ones are applied.
     */
    record LawyerNgoProfileUpdate(
            String name,
            String expertise,
            String location,
            String contactInfo,
            String organization,
            String language
    ) {}
    record PaginatedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}


    private ResponseEntity<?> setVerificationStatus(String username, boolean status) {
        log.info("Setting verification status to {} for username: {}", status, username);
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            log.warn("User not found for username: {}", username);
            return ResponseEntity.badRequest().body(new MessageResponse("User not found: " + username));
        }

        if (user.getRole() == Role.LAWYER) {
            log.info("Setting lawyer profile verification to {} for username: {}", status, username);
            var profileOpt = lawyerProfileRepository.findByUser(user);
            if (profileOpt.isEmpty()) {
                log.warn("Lawyer profile not found for username: {}", username);
                return ResponseEntity.badRequest().body(new MessageResponse("Lawyer profile not found for: " + username));
            }
            var profile = profileOpt.get();
            profile.setVerified(status);
            lawyerProfileRepository.save(profile);
            log.info("Lawyer profile verification updated successfully for username: {}", username);
            return ResponseEntity.ok(new MessageResponse("Lawyer profile verification set to " + status + " for " + username));
        }

        if (user.getRole() == Role.NGO) {
            log.info("Setting NGO profile verification to {} for username: {}", status, username);
            var profileOpt = ngoProfileRepository.findByUser(user);
            if (profileOpt.isEmpty()) {
                log.warn("NGO profile not found for username: {}", username);
                return ResponseEntity.badRequest().body(new MessageResponse("NGO profile not found for: " + username));
            }
            var profile = profileOpt.get();
            profile.setVerified(status);
            ngoProfileRepository.save(profile);
            log.info("NGO profile verification updated successfully for username: {}", username);
            return ResponseEntity.ok(new MessageResponse("NGO profile verification set to " + status + " for " + username));
        }

        log.warn("Verification requested for non-lawyer/NGO user. Username: {}", username);
        return ResponseEntity.badRequest().body(new MessageResponse("Verification applies only to lawyer or NGO profiles."));
    }
    @GetMapping("/appointments/stats")
    public ResponseEntity<AppointmentStatsDTO> getAppointmentStats() {
            return ResponseEntity.ok(adminUserService.getAppointmentStats());
    }
    @GetMapping("/appointments")
    public ResponseEntity<Page<AppointmentDTO>> getAllAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 1️⃣ Convert String → Enum
        AppointmentStatus appointmentStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                appointmentStatus = AppointmentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid appointment status: " + status
                );
            }
        }

        // 2️⃣ Fetch filtered ENTITY page
        Page<Appointment> appointments;
        if (appointmentStatus != null && search != null && !search.equals("")) {
            appointments = appointmentRepository
                    .findByStatusAndMatchIdContainingIgnoreCase(
                            appointmentStatus, search, pageable);

        } else if (appointmentStatus != null) {
            appointments = appointmentRepository
                    .findByStatus(appointmentStatus, pageable);

        } else if (search != null && !search.equals("")) {
            appointments = appointmentRepository
                    .findByMatchIdContainingIgnoreCase(search, pageable);

        } else {
            appointments = appointmentRepository.findAll(pageable);
        }

        // 3️⃣ Map ENTITY → DTO
        Page<AppointmentDTO> appointmentDTOs = appointments.map(appt -> {
            String requesterName = userRepository
                .findById(appt.getRequesterId().intValue())
                    .map(User::getFullName)
                    .orElse("Unknown");
            String receiverName = userRepository
                .findById(appt.getReceiverId().intValue())
                    .map(User::getFullName)
                    .orElse("Unknown");
            String caseTitle = null;
            String caseNumber = null;
            if (appt.getMatchId() != null) {
                try {
                    Integer matchPk = appt.getMatchId().intValue();
                    var match = matchRepository.findById(matchPk).orElse(null);
                    if (match != null && match.getCaseObj() != null) {
                        caseTitle = match.getCaseObj().getTitle();
                        caseNumber = match.getCaseObj().getCaseNumber();
                    }
                } catch (NumberFormatException ignored) {}
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
        });

        return ResponseEntity.ok(appointmentDTOs);
    }

    @GetMapping("/matches/stats")
    public ResponseEntity<MatchStatsDTO> getMatchStats() {
        log.info("Fetching match statistics for admin");
        MatchStatsDTO stats = matchAnalyticsService.getMatchStats();
        log.info("Match statistics retrieved successfully");
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/matches/monitoring")
    public ResponseEntity<Page<MatchMonitoringDTO>> getMatchesForMonitoring(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {

        log.info("Fetching matches for monitoring - page: {}, size: {}, search: {}, status: {}",
                 page, size, search, status);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<MatchMonitoringDTO> matches = matchAnalyticsService.getMatchesForMonitoring(search, status, pageable);

        log.info("Matches retrieved successfully - total elements: {}", matches.getTotalElements());
        return ResponseEntity.ok(matches);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/matches/{matchId}")
    public ResponseEntity<?> deleteMatch(@PathVariable Integer matchId) {
        log.info("Deleting match with ID: {}", matchId);

        var match = matchRepository.findById(matchId);
        if (match.isEmpty()) {
            log.warn("Match not found with ID: {}", matchId);
            return ResponseEntity.notFound().build();
        }

        matchRepository.deleteById(matchId);
        log.info("Match deleted successfully with ID: {}", matchId);
        return ResponseEntity.ok(new MessageResponse("Match deleted successfully"));
    }

    @PostMapping("/logs/cleanup")
    public ResponseEntity<?> cleanupLogs(@RequestParam(defaultValue = "30") int days) {
        log.info("Manual log cleanup requested for logs older than {} days", days);
        logCleanupService.cleanupLogsManually(days);
        return ResponseEntity.ok(new MessageResponse("Logs older than " + days + " days have been deleted."));
    }
}
