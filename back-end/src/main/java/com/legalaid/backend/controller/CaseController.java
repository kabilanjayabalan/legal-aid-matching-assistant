package com.legalaid.backend.controller;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.legalaid.backend.model.Case;
import com.legalaid.backend.model.CasePriority;
import com.legalaid.backend.model.CaseStatus;
import com.legalaid.backend.model.CaseType;
import com.legalaid.backend.model.EvidenceFile;
import com.legalaid.backend.model.Notification;
import com.legalaid.backend.model.User;
import com.legalaid.backend.repository.CaseRepository;
import com.legalaid.backend.repository.EvidenceFileRepository;
import com.legalaid.backend.repository.NotificationRepository;
import com.legalaid.backend.repository.UserRepository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/cases")
public class CaseController {

    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final EvidenceFileRepository evidenceFileRepository;
    private final com.legalaid.backend.service.CaseNumberGeneratorService caseNumberGeneratorService;
    private final NotificationRepository notificationRepository;

    public CaseController(
            CaseRepository caseRepository,
            UserRepository userRepository,
            EvidenceFileRepository evidenceFileRepository,
            com.legalaid.backend.service.CaseNumberGeneratorService caseNumberGeneratorService,
            NotificationRepository notificationRepository
        ) {
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.evidenceFileRepository = evidenceFileRepository;
        this.caseNumberGeneratorService = caseNumberGeneratorService;
        this.notificationRepository = notificationRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createCase(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam("location") String location,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "expertiseTags", required = false) List<String> expertiseTags,
            @RequestParam(value = "preferredLanguage", required = false) String preferredLanguage,
            @RequestParam(value = "parties", required = false) String parties,
            @RequestParam(value = "isUrgent", defaultValue = "false") Boolean isUrgent,
            @RequestParam("contactInfo") String contactInfo,
            @RequestParam(value = "evidenceFiles", required = false) MultipartFile[] evidenceFiles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthorized attempt to create case");
            return ResponseEntity.status(401).body(new MessageResponse("Unauthorized"));
        }
        String email = authentication.getName();
        log.info("Creating case for user: {}", email);

        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("Authenticated user not found in DB: {}", email);
            return ResponseEntity.status(401).body(new MessageResponse("Unauthorized"));
        }
        User user = userOpt.get();

        // Only citizens can create cases
        if (user.getRole() != com.legalaid.backend.model.Role.CITIZEN) {
            log.warn("User {} with role {} attempted to create a case", email, user.getRole());
            return ResponseEntity.status(403).body(new MessageResponse("Only citizens can create cases"));
        }

        Case c = new Case();
        c.setTitle(title != null ? title.trim() : null);
        c.setDescription(description);
        c.setPriority(isUrgent ? CasePriority.HIGH : CasePriority.LOW);
        c.setLocation(location);
        c.setCity(city);
        c.setLatitude(latitude);
        c.setLongitude(longitude);
        c.setContactInfo(contactInfo);
        c.setIsUrgent(isUrgent);
        c.setCategory(category);
        c.setExpertiseTags(expertiseTags != null ? expertiseTags : new ArrayList<>());
        c.setPreferredLanguage(preferredLanguage);
        c.setParties(parties);

        // Handle file uploads - save to database
        List<EvidenceFile> evidenceFileList = new ArrayList<>();
        if (evidenceFiles != null) {
            for (MultipartFile file : evidenceFiles) {
                if (!file.isEmpty()) {
                    try {
                        EvidenceFile evidenceFile = new EvidenceFile();
                        evidenceFile.setFileName(file.getOriginalFilename());
                        evidenceFile.setFileType(file.getContentType());
                        evidenceFile.setData(file.getBytes());
                        evidenceFile.setCaseObj(c);
                        evidenceFileList.add(evidenceFile);
                    } catch (IOException e) {
                        log.error("Failed to read file: {}", file.getOriginalFilename(), e);
                        // Continue with other files or handle error
                    }
                }
            }
        }
        c.setEvidenceFiles(evidenceFileList);

        c.setCreatedBy(user);
        c.setStatus(CaseStatus.OPEN);
        log.debug("Defaulting case status to OPEN for new case");
        // createdAt and updatedAt are handled by @PrePersist

        // Generate case number and set case type
        LocalDateTime now = LocalDateTime.now();
        c.setCreatedAt(now);
        String caseNumber = caseNumberGeneratorService.generateCaseNumberFromCategory(category, now);
        c.setCaseNumber(caseNumber);
        c.setCaseType(com.legalaid.backend.model.CaseType.fromCategory(category));
        log.info("Generated case number: {} for category: {}", caseNumber, category);

        Case saved = caseRepository.save(c);
        log.info("Case created with id: {}, case number: {} by user: {}", saved.getId(), saved.getCaseNumber(), email);

        URI caseUri = URI.create(String.format("/cases/%d", saved.getId()));
        if (user != null) {
            Notification notification = new Notification();
            notification.setUserId(Long.valueOf(user.getId()));
            notification.setType("CASE");
            notification.setMessage("Your case has been submitted successfully.");
            notification.setReferenceId(String.valueOf(saved.getId()));
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            notificationRepository.save(notification);
        }
        return ResponseEntity.created(caseUri).body(toResponse(saved));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyCases() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthorized attempt to fetch cases");
            return ResponseEntity.status(401).body(new MessageResponse("Unauthorized"));
        }
        String email = authentication.getName();
        log.info("Fetching cases for user: {}", email);
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("Authenticated user not found in DB: {}", email);
            return ResponseEntity.status(401).body(new MessageResponse("Unauthorized"));
        }
        User user = userOpt.get();

        List<Case> cases = caseRepository.findByCreatedBy(user);
        List<CaseResponse> res = cases.stream().map(this::toResponse).collect(Collectors.toList());
        log.info("Found {} cases for user: {}", res.size(), email);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCaseById(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthorized attempt to fetch case id: {}", id);
            return ResponseEntity.status(401).body(new MessageResponse("Unauthorized"));
        }
        log.info("Fetching case by id: {}", id);
        Case c = caseRepository.findById(id).orElse(null);
        if (c == null) {
            log.warn("Case not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(c));
    }
    @GetMapping("/{id}/timeline")
    public ResponseEntity<?> getCaseTimeline(@PathVariable Integer id) {

        Case c = caseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Case not found"));

        List<CaseTimelineResponse> timeline = new ArrayList<>();
        int step = 1;

        // Case created
        timeline.add(new CaseTimelineResponse(
            step++,
            CaseStatus.OPEN,
            "Case Submitted",
            c.getCreatedAt(),
            c.getCreatedBy().getEmail(),
            null
        ));

        // Case assigned
        if (c.getAssignedAt() != null) {
            timeline.add(new CaseTimelineResponse(
                step++,
                CaseStatus.IN_PROGRESS,
                "Assigned to Lawyer",
                c.getAssignedAt(),
                c.getAssignedTo() != null ? c.getAssignedTo().getEmail() : null,
                null
            ));
        }

        // Case closed
        if (c.getClosedAt() != null) {
            timeline.add(new CaseTimelineResponse(
                step,
                CaseStatus.CLOSED,
                "Case Closed",
                c.getClosedAt(),
                c.getClosedBy() != null ? c.getClosedBy().getEmail() : null,
                c.getClosureReason()
            ));
        }

        return ResponseEntity.ok(timeline);
    }

    @PostMapping("/{id}/update")
    public ResponseEntity<?> updateCase(
            @PathVariable Integer id,
            @RequestBody UpdateCaseRequest req
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();

        Case c = caseRepository.findById(id).orElseThrow();

        if (!c.getCreatedBy().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(new MessageResponse("Forbidden"));
        }
        if (c.getStatus() == CaseStatus.CLOSED) {
            throw new IllegalStateException("Closed cases cannot be modified");
        }

        c.setTitle(req.title() != null ? req.title().trim() : null);
        c.setDescription(req.description());
        c.setCategory(req.category());
        c.setLocation(req.location());
        c.setCity(req.city());
        c.setLatitude(req.latitude());
        c.setLongitude(req.longitude());
        c.setContactInfo(req.contactInfo());
        c.setIsUrgent(Boolean.TRUE.equals(req.isUrgent()));
        c.setPriority(Boolean.TRUE.equals(req.isUrgent())
            ? CasePriority.HIGH
            : CasePriority.LOW
        );
        c.setExpertiseTags(req.expertiseTags());
        c.setPreferredLanguage(req.preferredLanguage());
        c.setParties(req.parties());
        if (user != null) {
            Notification notification = new Notification();
            notification.setUserId(Long.valueOf(user.getId()));
            notification.setType("CASE");
            notification.setMessage("Your case has been updated successfully.");
            notification.setReferenceId(String.valueOf(id));
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            notificationRepository.save(notification);
        }
        return ResponseEntity.ok(toResponse(caseRepository.save(c)));
    }


    @GetMapping("/evidence/{fileId}")
    public ResponseEntity<?> getEvidenceFile(@PathVariable Integer fileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Unauthorized attempt to fetch evidence file id: {}", fileId);
            return ResponseEntity.status(401).body(new MessageResponse("Unauthorized"));
        }
        log.info("Fetching evidence file by id: {}", fileId);
        
        EvidenceFile evidenceFile = evidenceFileRepository.findById(fileId).orElse(null);
        if (evidenceFile == null) {
            log.warn("Evidence file not found with id: {}", fileId);
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(evidenceFile.getFileType() != null ? evidenceFile.getFileType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.setContentDispositionFormData("attachment", evidenceFile.getFileName());

        return ResponseEntity.ok()
                .headers(headers)
                .body(evidenceFile.getData());
    }
    @PostMapping("/{id}/close")
    public ResponseEntity<?> closeCase(
        @PathVariable Integer id,
        @RequestBody(required = false) CloseCaseRequest request
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(new MessageResponse("Unauthorized"));
        }

        User user = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Case c = caseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Case not found"));

        //  Authorization rules
        boolean isCreator = c.getCreatedBy().getId().equals(user.getId());
        boolean isAssignedLawyer = c.getAssignedTo() != null
            && c.getAssignedTo().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == com.legalaid.backend.model.Role.ADMIN;

        if (!isCreator && !isAssignedLawyer && !isAdmin) {
            return ResponseEntity.status(403)
                .body(new MessageResponse("You are not allowed to close this case"));
        }

        //  Already closed
        if (c.getStatus() == CaseStatus.CLOSED) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Case is already closed"));
        }

        //  Close case
        c.setStatus(CaseStatus.CLOSED);
        c.setClosedAt(LocalDateTime.now());
        c.setClosedBy(user);
        c.setClosureReason(request != null ? request.reason() : null);

        caseRepository.save(c);

        log.info("Case {} closed by {}", id, user.getEmail());
        if (user != null) {
            Notification notification = new Notification();
            notification.setUserId(Long.valueOf(user.getId()));
            notification.setType("CASE");
            notification.setMessage("Your case has been closed by " + user.getEmail() + ".Because: " + (request != null ? request.reason() : "No reason provided"));
            notification.setReferenceId(String.valueOf(id));
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            notificationRepository.save(notification);
        }

        return ResponseEntity.ok(new MessageResponse("Case closed successfully"));
    }

    private CasePriority parsePriority(String p) {
        if (p == null || p.isBlank()) return CasePriority.MEDIUM;
        try {
            return CasePriority.valueOf(p.toUpperCase());
        } catch (Exception e) {
            log.warn("Invalid priority value: {}. Defaulting to MEDIUM", p);
            return CasePriority.MEDIUM;
        }
    }

    private CaseResponse toResponse(Case c) {
        List<EvidenceFileInfo> evidenceFileInfos = new ArrayList<>();
        if (c.getEvidenceFiles() != null) {
            evidenceFileInfos = c.getEvidenceFiles().stream()
                    .map(file -> new EvidenceFileInfo(
                            file.getId(),
                            file.getFileName(),
                            "/cases/evidence/" + file.getId()
                    ))
                    .collect(Collectors.toList());
        }
        
        return new CaseResponse(
                c.getId(),
                c.getCaseNumber(),
                c.getCaseType(),
                c.getTitle(),
                c.getDescription(),
                c.getStatus(),
                c.getPriority(),
                c.getCreatedBy() != null ? c.getCreatedBy().getEmail() : null,
                c.getAssignedTo() != null ? c.getAssignedTo().getEmail() : null,
                c.getLocation(),
                c.getCity(),
                c.getLongitude(),
                c.getLatitude(),
                c.getIsUrgent(),
                c.getContactInfo(),
                c.getCategory(),
                c.getExpertiseTags(),
                c.getPreferredLanguage(),
                c.getParties(),
                evidenceFileInfos,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    public record CaseCreateRequest(
            @NotBlank @Size(max = 255) String title,
            String description,
            String priority,
            String location,
            String city,
            Double latitude,
            Double longitude,
            String contactInfo,
            Boolean isUrgent,
            String category,
            List<String> expertiseTags,
            String preferredLanguage,
            String parties,
            List<String> evidenceFiles
    ) {}

    public record EvidenceFileInfo(
            Integer fileId,
            String fileName,
            String downloadUrl
    ) {}

    public record CaseResponse(
            Integer id,
            String caseNumber,
            CaseType caseType,
            String title,
            String description,
            CaseStatus status,
            CasePriority priority,
            String createdByEmail,
            String assignedToEmail,
            String location,
            String city,
            Double latitude,
            Double longitude,
            Boolean isUrgent,
            String contactInfo,
            String category,
            List<String> expertiseTags,
            String preferredLanguage,
            String parties,
            List<EvidenceFileInfo> evidenceFiles,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
    public record UpdateCaseRequest(
        String title,
        String description,
        String category,
        String location,
        String city,
        Double latitude,
        Double longitude,
        String contactInfo,
        Boolean isUrgent,
        List<String> expertiseTags,
        String preferredLanguage,
        String parties
    ) {}

    public record CloseCaseRequest(
        String reason
    ) {}
    public record CaseTimelineResponse(
        int step,
        CaseStatus status,
        String label,
        LocalDateTime timestamp,
        String performedBy,
        String reason
    ) {}

    public record MessageResponse(String message) {}
}