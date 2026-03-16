package com.legalaid.backend.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.legalaid.backend.dto.DirectorySearchRequest;
import com.legalaid.backend.dto.ImportSummary;
import com.legalaid.backend.dto.ProviderDetailsDTO;
import com.legalaid.backend.model.DirectoryLawyer;
import com.legalaid.backend.model.DirectoryNgo;
import com.legalaid.backend.model.ImportMode;
import com.legalaid.backend.model.Profile;
import com.legalaid.backend.model.User;
import com.legalaid.backend.repository.ProfileRepository;
import com.legalaid.backend.repository.UserRepository;
import com.legalaid.backend.service.DirectoryService;

@RestController
@RequestMapping("/api/directory")
public class DirectoryController {

    private final DirectoryService directoryService;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public DirectoryController(DirectoryService directoryService,
                               ProfileRepository profileRepository,
                               UserRepository userRepository) {
        this.directoryService = directoryService;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/import/lawyers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ImportSummary importLawyers(
            @RequestBody List<DirectoryLawyer> lawyers,
            @RequestParam(required = false, defaultValue = "SKIP") ImportMode strategy) {
        return directoryService.importLawyers(lawyers, strategy);
    }

    @PostMapping("/import/ngos")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ImportSummary importNgos(
            @RequestBody List<DirectoryNgo> ngos,
            @RequestParam(required = false, defaultValue = "SKIP") ImportMode strategy) {
        return directoryService.importNgos(ngos, strategy);
    }

    @GetMapping("/lawyers")
    @CrossOrigin
    public List<DirectoryLawyer> searchLawyers(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String specialization
    ) {
        return directoryService.searchLawyers(city, specialization);
    }

    @GetMapping("/ngos")
    @CrossOrigin
    public List<DirectoryNgo> searchNgos(
            @RequestParam(required = false) String city,
            @RequestParam(required = false, name = "focusArea") String focusArea
    ) {
        return directoryService.searchNgos(city, focusArea);
    }

    /**
     * Searches the directory with optional filtering, pagination, and sorting.
     *
     *  @param request type (LAWYER, NGO, BOTH)
     *  @param request page (zero-based, default 0)
     *  @param request size (default 10)
     *  @param request name
     *  @param request city
     *  @param request specialization (for lawyers)
     *  @param request focusArea (for NGOs)
     *  @param request sortBy (name, city, etc.)
     *  @param request sortDir (asc, desc)
     */
    @GetMapping("/search")
    @CrossOrigin
    public Object searchDirectory(@ModelAttribute DirectorySearchRequest request) {
        if (request.getRadiusKm() != null && request.getRadiusKm() > 100) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
            "Radius cannot exceed 100km"
            );
        }

        // If locationType is "PROFILE", fetch user's profile location
        if ("PROFILE".equalsIgnoreCase(request.getLocationType())) {
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
                    String email = authentication.getName();
                    Optional<User> userOpt = userRepository.findByEmail(email);

                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        Optional<Profile> profileOpt = profileRepository.findByUser(user);

                        if (profileOpt.isPresent()) {
                            Profile profile = profileOpt.get();
                            // Override request coordinates with profile location
                            if (profile.getLatitude() != null && profile.getLongitude() != null) {
                                request.setLatitude(profile.getLatitude());
                                request.setLongitude(profile.getLongitude());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // If fetching profile location fails, continue with provided coordinates
                // This ensures the search still works even if profile location is not available
            }
        }

        return directoryService.searchDirectory(request);
    }

    /**
     * Gets all unique specializations from both internal lawyer profiles and external directory lawyers.
     * @return Map containing list of specializations
     */
    @GetMapping("/specializations")
    @CrossOrigin
    public Map<String, List<String>> getAllSpecializations() {
        List<String> specializations = directoryService.getAllSpecializations();
        return Map.of("specializations", specializations);
    }

    /**
     * Gets all unique focus areas from both internal NGO profiles and external directory NGOs.
     * @return Map containing list of focus areas
     */
    @GetMapping("/focus-areas")
    @CrossOrigin
    public Map<String, List<String>> getAllFocusAreas() {
        List<String> focusAreas = directoryService.getAllFocusAreas();
        return Map.of("focusAreas", focusAreas);
    }


    @PostMapping("/lawyers/check-import")
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<Map<String, Object>> checkLawyerImports(@RequestBody List<String> barRegistrationIds) {
        return directoryService.checkLawyerImports(barRegistrationIds);
    }

    @PostMapping("/ngos/check-import")
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<Map<String, Object>> checkNgoImports(@RequestBody List<String> registrationNumbers) {
        return directoryService.checkNgoImports(registrationNumbers);
    }

    /**
     * Gets lawyer details by ID from either internal profile or external directory.
     * @param id The ID of the lawyer profile
     * @return ProviderDetailsDTO with lawyer details
     */
    @GetMapping("/lawyers/{id}")
    @CrossOrigin
    public ResponseEntity<ProviderDetailsDTO> getLawyerById(@PathVariable Integer id) {
        ProviderDetailsDTO details = directoryService.getLawyerDetailsById(id);
        if (details == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(details);
    }

    /**
     * Gets NGO details by ID from either internal profile or external directory.
     * @param id The ID of the NGO profile
     * @return ProviderDetailsDTO with NGO details
     */
    @GetMapping("/ngos/{id}")
    @CrossOrigin
    public ResponseEntity<ProviderDetailsDTO> getNgoById(@PathVariable Integer id) {
        ProviderDetailsDTO details = directoryService.getNgoDetailsById(id);
        if (details == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(details);
    }
}
