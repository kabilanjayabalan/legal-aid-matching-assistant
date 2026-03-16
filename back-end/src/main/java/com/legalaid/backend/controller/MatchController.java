package com.legalaid.backend.controller;


import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.legalaid.backend.dto.CaseSummaryDTO;
import com.legalaid.backend.dto.CitizenMatchDTO;
import com.legalaid.backend.dto.MatchResultDTO;
import com.legalaid.backend.dto.MatchSummaryDTO;
import com.legalaid.backend.dto.ProviderDashboardStatsDTO;
import com.legalaid.backend.model.Case;
import com.legalaid.backend.model.CaseStatus;
import com.legalaid.backend.model.Match;
import com.legalaid.backend.model.MatchStatus;
import com.legalaid.backend.model.ProviderType;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.repository.CaseRepository;
import com.legalaid.backend.repository.LawyerProfileRepository;
import com.legalaid.backend.repository.MatchRepository;
import com.legalaid.backend.repository.NGOProfileRepository;
import com.legalaid.backend.repository.UserRepository;
import com.legalaid.backend.service.MatchingService;
import com.legalaid.backend.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchingService matchingService;
    private final MatchRepository matchRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final LawyerProfileRepository lawyerProfileRepository;  
    private final NGOProfileRepository ngoProfileRepository;  
    private final NotificationService notificationService;


    @PreAuthorize("hasAuthority('CITIZEN')")
    @PostMapping("/generate/{caseId}")
    public MatchResultDTO generate(@PathVariable Integer caseId, Authentication auth,
        @RequestParam(required = false) Integer sensitivity) {
        log.info("Generate matches | caseId={}", caseId);
        Case caseObj = caseRepository.findById(caseId).orElseThrow();
        MatchResultDTO result = matchingService.generateMatches(caseObj, sensitivity);
        log.info("Matches generated for case {}", caseId);
        
        // 🔔 Notify citizen about new pending matches
        try {
            User citizen = getLoggedInUser(auth);
            int matchCount = result.results().size();
            if (matchCount > 0) {
                notificationService.notifyUser(
                    citizen.getId().longValue(),
                    "MATCH",
                    String.format("Found %d new match%s for your case: %s", 
                        matchCount, matchCount == 1 ? "" : "es", caseObj.getTitle()),
                    String.valueOf(caseId)
                );
            }
        } catch (Exception e) {
            log.warn("Failed to send notification for generated matches [{}]: {}", caseId, e.getMessage());
        }
        
        return result;
    }


    @PreAuthorize("hasAuthority('CITIZEN')")
    @GetMapping("/my-cases")
    public Page<CitizenMatchDTO> citizenMatches(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User citizen = getLoggedInUser(auth);
        Pageable pageable = PageRequest.of(page, size);

        return matchRepository.findCitizenMatches(citizen.getId(), pageable);
    }


    @PreAuthorize("hasAuthority('CITIZEN')")
    @PutMapping("/{matchId}/citizen-accept")
    public ResponseEntity<?> citizenAccept(
            @PathVariable Integer matchId,
            Authentication auth) {
        log.info("Citizen [{}] attempting to ACCEPT match [{}]", auth.getName(), matchId);
        Match match = matchRepository.findById(matchId).orElseThrow();
        User citizen = getLoggedInUser(auth);

        MatchStatus status = match.getStatus();
        CaseStatus caseStatus = match.getCaseObj().getStatus();

        if (status == MatchStatus.CITIZEN_ACCEPTED && caseStatus == CaseStatus.OPEN) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", "You have already accepted this match",
                "currentStatus", status
            ));
        }
        if (status == MatchStatus.REJECTED) {
            return ResponseEntity.badRequest().body(Map.of(
            "message", "This match was rejected and cannot be accepted",
            "currentStatus", status
            ));
        }

        if (status == MatchStatus.PROVIDER_CONFIRMED && caseStatus == CaseStatus.IN_PROGRESS) {
            return ResponseEntity.badRequest().body(Map.of(
            "message", "Your request has already been confirmed by this provider",
            "currentStatus", status
            ));
        }
        if (caseStatus == CaseStatus.IN_PROGRESS) {
            return ResponseEntity.badRequest().body(Map.of(
            "message", "Your request has already been confirmed by the provider.YOU CANNOT ACCEPT IT NOW",
            "currentStatus", status
            ));
        }

        if ( caseStatus == CaseStatus.CLOSED) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", "Match cannot be accepted. Case is already resolved",
                "currentStatus", status
            ));
        }
        if(status != MatchStatus.PENDING && caseStatus != CaseStatus.OPEN) {
            return ResponseEntity.badRequest().body(Map.of(
            "message", "Match is not in a valid state to be accepted",
            "currentStatus", status
            ));
        }

        match.setStatus(MatchStatus.CITIZEN_ACCEPTED);
        matchRepository.save(match);
        log.info("Match [{}] ACCEPTED successfully by citizen [{}]",
            matchId, auth.getName());

        // 🔔 Notify provider that match was accepted
        try {
            String providerUserId = getProviderUserId(match);
            if (providerUserId != null) {
                String providerName = getProviderName(match);
                notificationService.notifyUser(
                    Long.valueOf(providerUserId),
                    "MATCH",
                    String.format("Your match request has been accepted by the citizen. Case: %s", match.getCaseObj().getTitle()),
                    String.valueOf(matchId)
                );
            }
        } catch (Exception e) {
            log.warn("Failed to send notification for accepted match [{}]: {}", matchId, e.getMessage());
        }

        return ResponseEntity.ok(
            Map.of(
                "status", "SUCCESS",
                "message", "Match request sent successfully",
                "matchId", matchId
            )
        );
    }

    @PreAuthorize("hasAuthority('CITIZEN')")
    @PutMapping("/{matchId}/citizen-reject")
    public ResponseEntity<?> citizenReject(
            @PathVariable Integer matchId,
            Authentication auth) {
        log.info("Citizen [{}] attempting to REJECT match [{}]", auth.getName(), matchId);
        Match match = matchRepository.findById(matchId).orElseThrow();
        User citizen = getLoggedInUser(auth);

        if (!match.getCaseObj().getCreatedBy().getId()
            .equals(citizen.getId())) {
            log.warn("Unauthorized reject attempt by citizen [{}] on match [{}]",
                auth.getName(), matchId);
                return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Not your case"));
        }
        MatchStatus previousStatus = match.getStatus();
        match.setStatus(MatchStatus.REJECTED);
        matchRepository.save(match);
        log.info("Match [{}] REJECTED successfully by citizen [{}]",
            matchId, auth.getName());
        if (previousStatus == MatchStatus.PROVIDER_CONFIRMED) {
            Case c = match.getCaseObj();

            c.setStatus(CaseStatus.OPEN);
            c.setAssignedTo(null);
            c.setUpdatedAt(LocalDateTime.now());

            caseRepository.save(c);
            log.info(
                "Case [{}] reverted to SUBMITTED after citizen rejected confirmed match [{}]",
                c.getId(), matchId
            );
        }

        return ResponseEntity.ok(
            Map.of(
                "status", "SUCCESS",
                "message", "Match rejected successfully",
                "matchId", matchId
            )
        );
    }
    @PreAuthorize("hasAuthority('CITIZEN')")
    @PutMapping("/{matchId}/save")
    public ResponseEntity<?> saveProfile(
        @PathVariable Integer matchId,
        Authentication auth) {

        User citizen = getLoggedInUser(auth);
        Match match = matchRepository.findById(matchId).orElseThrow();

        // ownership check
        if (!match.getCaseObj().getCreatedBy().getId().equals(citizen.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Not your case"));
        }

        match.setSaved(true);
        matchRepository.save(match);
        log.info("Profile for match [{}] SAVED by citizen [{}]",
            matchId, auth.getName());

        return ResponseEntity.ok(
            Map.of(
                "status", "SUCCESS",
                "message", "Profile saved",
                "matchId", matchId
            )
        );
    }
    @PreAuthorize("hasAuthority('CITIZEN')")
    @GetMapping("/my/saved")
    public Page<CitizenMatchDTO> getSavedProfiles(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User citizen = getLoggedInUser(auth);
        Pageable pageable = PageRequest.of(page, size);

        return matchRepository.findSavedCitizenMatches(
            citizen.getId(),
            pageable
        );
    }


    @PreAuthorize("hasAuthority('CITIZEN')")
    @PutMapping("/{matchId}/unsave")
    public ResponseEntity<?> unsaveProfile(
            @PathVariable Integer matchId,
            Authentication auth) {

        User citizen = getLoggedInUser(auth);
        Match match = matchRepository.findById(matchId).orElseThrow();

        if (!match.getCaseObj().getCreatedBy().getId().equals(citizen.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Not your case"));
        }

        match.setSaved(false);
        matchRepository.save(match);
        log.info("Profile for match [{}] UNSAVED by citizen [{}]",
            matchId, auth.getName());

        return ResponseEntity.ok(
            Map.of(
                "status", "SUCCESS",
                "message", "Profile unsaved",
                "matchId", matchId
            )
        );
    }



    /* ================= LAWYER / NGO ================= */

    @GetMapping("/my/requests")
    @PreAuthorize("hasAnyAuthority('LAWYER','NGO')")
    public Page<MatchSummaryDTO> providerMatches(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
        ) {
        User user = getLoggedInUser(auth);

        ProviderType providerType =
            user.getRole() == Role.LAWYER ? ProviderType.LAWYER : ProviderType.NGO;

        Integer providerProfileId =
            providerType == ProviderType.LAWYER
                ? lawyerProfileRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Lawyer profile not found"))
                    .getId()
                : ngoProfileRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("NGO profile not found"))
                    .getId();

        Pageable pageable = PageRequest.of(page, size);


        return matchRepository.findProviderRequests(
            providerType,
            providerProfileId,
            MatchStatus.CITIZEN_ACCEPTED,
            CaseStatus.OPEN,
            pageable
        );
    }

    @GetMapping("/my/assigned-cases")
    @PreAuthorize("hasAnyAuthority('LAWYER','NGO')")
    public Page<CaseSummaryDTO> myAssignedCases(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = getLoggedInUser(auth);

        ProviderType providerType =
            user.getRole() == Role.LAWYER ? ProviderType.LAWYER : ProviderType.NGO;

        Integer providerProfileId =
            providerType == ProviderType.LAWYER
                ? lawyerProfileRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Lawyer profile not found"))
                    .getId()
                : ngoProfileRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("NGO profile not found"))
                    .getId();
        Pageable pageable = PageRequest.of(page, size);

        return matchRepository.findAssignedCasesDTO(
            providerType,
            providerProfileId,
            MatchStatus.PROVIDER_CONFIRMED,
            pageable
        );
    }

    @GetMapping("/provider/dashboard-stats")
    @PreAuthorize("hasAnyAuthority('LAWYER','NGO')")
    public ResponseEntity<ProviderDashboardStatsDTO> getProviderDashboardStats(Authentication auth) {
        log.info("Fetching dashboard stats for provider: {}", auth.getName());
        ProviderDashboardStatsDTO stats = matchingService.getProviderDashboardStats(auth.getName());
        return ResponseEntity.ok(stats);
    }

    @PreAuthorize("hasAnyAuthority('LAWYER','NGO')")
    @PutMapping("/{matchId}/confirm")
    public ResponseEntity<?> confirmMatch(
        @PathVariable Integer matchId,
        Authentication auth) {

        Match match = matchRepository.findById(matchId).orElseThrow();
        User user = getLoggedInUser(auth);

        Integer providerProfileId;
        ProviderType providerType;

        if (user.getRole() == Role.LAWYER) {
            providerProfileId = lawyerProfileRepository
                    .findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Lawyer profile not found"))
                    .getId();
            providerType = ProviderType.LAWYER;
        } else {
            providerProfileId = ngoProfileRepository
                    .findByUser(user)
                    .orElseThrow(() -> new RuntimeException("NGO profile not found"))
                    .getId();
            providerType = ProviderType.NGO;
        }
        // AUTH CHECK (CORRECT)
        if (!match.getProviderType().equals(providerType)
            || !match.getProviderId().equals(providerProfileId)) {

            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Unauthorized"));
        }

        if (match.getStatus() != MatchStatus.CITIZEN_ACCEPTED) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Match is not ready for confirmation")
            );
        }
        if(match.getCaseObj().getStatus() == CaseStatus.IN_PROGRESS) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Case is already assigned to a provider")
            );
        }

        match.setStatus(MatchStatus.PROVIDER_CONFIRMED);

        Case c = match.getCaseObj();
        if (c.getAssignedAt() == null) {
            c.setAssignedAt(LocalDateTime.now());
        }
        c.setStatus(CaseStatus.IN_PROGRESS);
        c.setAssignedTo(user);
        c.setUpdatedAt(LocalDateTime.now());

        caseRepository.save(c);
        matchRepository.save(match);
        log.info("Match [{}] CONFIRMED successfully by provider [{}]",
            matchId, auth.getName());

        //  Notify citizen that match was confirmed (soon to be assigned)
        try {
            String citizenUserId = match.getCaseObj().getCreatedBy().getId().toString();
            String providerName = getProviderName(match);
            notificationService.notifyUser(
                Long.valueOf(citizenUserId),
                "CASE",
                String.format("Your match has been confirmed. Your case is assigned to %s", providerName),
                String.valueOf(matchId)
            );
        } catch (Exception e) {
            log.warn("Failed to send notification for confirmed match [{}]: {}", matchId, e.getMessage());
        }

        return ResponseEntity.ok(
            Map.of(
                "status", "SUCCESS",
                "message", "Match confirmed",
                "matchId", matchId
            )
        );
    }

    @PreAuthorize("hasAnyAuthority('LAWYER','NGO')")
@PutMapping("/{matchId}/provider-reject")
public ResponseEntity<?> providerReject(
        @PathVariable Integer matchId,
        Authentication auth
) {
    log.info("Provider [{}] attempting to REJECT match [{}]", auth.getName(), matchId);

    Match match = matchRepository.findById(matchId).orElseThrow();
    User user = getLoggedInUser(auth);

    ProviderType providerType;
    Integer providerProfileId;

    if (user.getRole() == Role.LAWYER) {
        providerType = ProviderType.LAWYER;
        providerProfileId = lawyerProfileRepository.findByUser(user)
            .orElseThrow(() -> new RuntimeException("Lawyer profile not found"))
            .getId();
    } else {
        providerType = ProviderType.NGO;
        providerProfileId = ngoProfileRepository.findByUser(user)
            .orElseThrow(() -> new RuntimeException("NGO profile not found"))
            .getId();
    }

    // 🔒 AUTH CHECK
    if (!match.getProviderType().equals(providerType)
        || !match.getProviderId().equals(providerProfileId)) {

        log.warn("Unauthorized provider reject attempt [{}]", auth.getName());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(Map.of("message", "Unauthorized"));
    }

    // ❌ Invalid states
    if (match.getStatus() == MatchStatus.PROVIDER_CONFIRMED) {
        return ResponseEntity.badRequest().body(
            Map.of("message", "Cannot reject a confirmed match")
        );
    }

    if (match.getStatus() == MatchStatus.REJECTED) {
        return ResponseEntity.badRequest().body(
            Map.of("message", "Match already rejected")
        );
    }

    // ✅ Reject
    match.setStatus(MatchStatus.REJECTED);
    matchRepository.save(match);

    log.info("Match [{}] rejected successfully by provider [{}]", matchId, auth.getName());

    // 🔔 Notify citizen
    try {
        String citizenUserId = match.getCaseObj().getCreatedBy().getId().toString();
        String providerName = getProviderName(match);

        notificationService.notifyUser(
            Long.valueOf(citizenUserId),
            "MATCH",
            String.format(
                "%s has declined your match request for case: %s",
                providerName,
                match.getCaseObj().getTitle()
            ),
            matchId.toString()
        );
    } catch (Exception e) {
        log.warn("Failed to notify citizen for rejected match [{}]: {}", matchId, e.getMessage());
    }

    return ResponseEntity.ok(
        Map.of(
            "status", "SUCCESS",
            "message", "Match rejected successfully",
            "matchId", matchId
        )
    );
}


    /* ================= HELPER ================= */

    private User getLoggedInUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
        .orElseThrow(() ->
            new RuntimeException("User not found: " + auth.getName())
        );
    }

    /**
     * Get provider user ID from match
     */
    private String getProviderUserId(Match match) {
        try {
            if (match.getProviderType() == ProviderType.LAWYER) {
                return lawyerProfileRepository.findById(match.getProviderId())
                    .map(lp -> lp.getUser().getId().toString())
                    .orElse(null);
            } else {
                return ngoProfileRepository.findById(match.getProviderId())
                    .map(np -> np.getUser().getId().toString())
                    .orElse(null);
            }
        } catch (Exception e) {
            log.warn("Failed to get provider user ID for match [{}]: {}", match.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Get provider name from match
     */
    private String getProviderName(Match match) {
        try {
            if (match.getProviderType() == ProviderType.LAWYER) {
                return lawyerProfileRepository.findById(match.getProviderId())
                    .map(lp -> lp.getName() != null ? lp.getName() : "Lawyer")
                    .orElse("Lawyer");
            } else {
                return ngoProfileRepository.findById(match.getProviderId())
                    .map(np -> np.getNgoName() != null ? np.getNgoName() : "NGO")
                    .orElse("NGO");
            }
        } catch (Exception e) {
            log.warn("Failed to get provider name for match [{}]: {}", match.getId(), e.getMessage());
            return "Provider";
        }
    }
}
