package com.legalaid.backend.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.legalaid.backend.dto.CitizenProfileUpdateDTO;
import com.legalaid.backend.model.LawyerProfile;
import com.legalaid.backend.model.NGOProfile;
import com.legalaid.backend.model.Profile;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.repository.LawyerProfileRepository;
import com.legalaid.backend.repository.NGOProfileRepository;
import com.legalaid.backend.repository.ProfileRepository;
import com.legalaid.backend.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final LawyerProfileRepository lawyerProfileRepository;
    private final NGOProfileRepository ngoProfileRepository;

    public ProfileController(UserRepository userRepository,
                             ProfileRepository profileRepository,
                             LawyerProfileRepository lawyerProfileRepository,
                             NGOProfileRepository ngoProfileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.lawyerProfileRepository = lawyerProfileRepository;
        this.ngoProfileRepository = ngoProfileRepository;
    }

    // As per Doc Section 4.E: GET /profile/me
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        log.info("Fetching profile for current user");
        // 1. Get the email from the Security Context (set by the JWT Filter)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        log.debug("Retrieved email from security context: {}", email);

        // 2. Fetch user details from DB
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        log.info("Profile fetched successfully for email: {}", email);

        // 3. Create a response that includes profile location if available
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("role", user.getRole());
        response.put("profileId", user.getProfileId());
        response.put("approved", user.getApproved());
        response.put("status", user.getStatus());
        response.put("createdAt", user.getCreatedAt());

        // Fetch profile details if profileId exists
        if (user.getProfileId() != null) {
            Optional<Profile> profileOpt = profileRepository.findByUser(user);
            if (profileOpt.isPresent()) {
                Profile profile = profileOpt.get();
                response.put("location", profile.getLocation());
                response.put("latitude", profile.getLatitude());
                response.put("longitude", profile.getLongitude());
                response.put("contactInfo", profile.getContactInfo());
            }
        }

        return ResponseEntity.ok(response);
    }

    @Transactional
    @PutMapping("/update/citizen")
    public ResponseEntity<?> updateCitizenProfile(@RequestBody CitizenProfileUpdateDTO dto) throws Exception{

        log.info("Updating citizen profile for current user");
        User user = currentUser();
        if (user.getRole() != Role.CITIZEN) {
            log.error("User role {} not allowed for citizen endpoint", user.getRole());
            throw new Exception("Only citizens can update this profile");
        }

        validateCitizen(dto);
        // ✅ update USER.fullName
        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            user.setFullName(dto.getFullName());
        }


        Profile profile = profileRepository.findByUser(user).orElse(null);

        if (profile == null) {
            profile = new Profile();
            profile.setUser(user);

             // 🔥 FIRST SAVE → ID GENERATED
            profile = profileRepository.save(profile);

            // 🔥 SET role_id ONLY ON FIRST CREATION
            if (user.getProfileId() == null) {
                user.setProfileId(profile.getId());
                userRepository.save(user);
            }
        }

        if (dto.getContactInfo() != null) profile.setContactInfo(dto.getContactInfo());
        if (dto.getLocation() != null) profile.setLocation(dto.getLocation());
        if (dto.getLatitude() != null) profile.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) profile.setLongitude(dto.getLongitude());
        profile.setUser(user);
        Profile saved = profileRepository.save(profile);
        log.info("Citizen profile updated for {}", user.getEmail());
        log.info("Returning fullName = {}", saved.getUser().getFullName());

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/update/lawyer")
    public ResponseEntity<?> updateLawyerProfile(@RequestBody LawyerProfileUpdate payload) throws Exception {
        log.info("Updating lawyer profile for current user");
        User user = currentUser();
        if (user.getRole() != Role.LAWYER) {
            log.error("User role {} not allowed for lawyer endpoint", user.getRole());
            throw new Exception("Only lawyers can update this profile");
        }

        validateLawyer(payload);
        if (payload.name != null && !payload.name.isBlank()) {
            user.setFullName(payload.name);
        }

        LawyerProfile profile = lawyerProfileRepository.findByUser(user).orElse(null);

        if (profile == null) {
            profile = new LawyerProfile();
            profile.setUser(user);

            profile = lawyerProfileRepository.save(profile);

            if (user.getProfileId() == null) {
                user.setProfileId(profile.getId());
                userRepository.save(user);
            }
        }

        boolean requiresReverify = false;
        if (payload.name != null) profile.setName(payload.name);
        if (payload.expertise != null) profile.setExpertise(payload.expertise);
        if (payload.location != null) profile.setLocation(payload.location);
        if(payload.latitude != null) profile.setLatitude(payload.latitude);
        if(payload.longitude != null) profile.setLongitude(payload.longitude);
        if (payload.contactInfo != null) profile.setContactInfo(payload.contactInfo);
        if (payload.barRegistrationNo != null) {
            profile.setBarRegistrationNo(payload.barRegistrationNo);
            requiresReverify = true;
        }
        if (payload.specialization != null) {
            profile.setSpecialization(payload.specialization);
            requiresReverify = true;
        }
        if (payload.experienceYears != null) profile.setExperienceYears(payload.experienceYears);
        if (payload.city != null) {
            profile.setCity(payload.city);
            requiresReverify = true;
        }
        if (payload.bio != null) profile.setBio(payload.bio);
        if (payload.language != null) profile.setLanguage(payload.language);
        if (payload.isAvailable != null) profile.setIsAvailable(payload.isAvailable);
        if (requiresReverify) {
            profile.setVerified(null);
        }

        LawyerProfile saved = lawyerProfileRepository.save(profile);
        log.info("Lawyer profile updated for {}", user.getEmail());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/update/ngo")
    public ResponseEntity<?> updateNgoProfile(@RequestBody NgoProfileUpdate payload) throws Exception {
        log.info("Updating NGO profile for current user");
        User user = currentUser();
        if (user.getRole() != Role.NGO) {
            log.error("User role {} not allowed for NGO endpoint", user.getRole());
            throw new Exception("Only NGOs can update this profile");
        }

        validateNgo(payload);

        if (payload.ngoName != null && !payload.ngoName.isBlank()) {
            user.setFullName(payload.ngoName);
        }

        NGOProfile profile = ngoProfileRepository.findByUser(user).orElse(null);

        if (profile == null) {
            profile = new NGOProfile();
            profile.setUser(user);

            profile = ngoProfileRepository.save(profile);

            if (user.getProfileId() == null) {
                user.setProfileId(profile.getId());
                userRepository.save(user);
            }
        }

        boolean requiresReverify = false;
        if (payload.organization != null) profile.setOrganization(payload.organization);
        if (payload.contactInfo != null) profile.setContactInfo(payload.contactInfo);
        if (payload.location != null) profile.setLocation(payload.location);
        if(payload.latitude != null) profile.setLatitude(payload.latitude);
        if(payload.longitude != null) profile.setLongitude(payload.longitude);
        if (payload.ngoName != null) {
            profile.setNgoName(payload.ngoName);
            requiresReverify = true;
        }
        if (payload.registrationNo != null) {
            profile.setRegistrationNo(payload.registrationNo);
            requiresReverify = true;
        }
        if (payload.city != null) {
            profile.setCity(payload.city);
            requiresReverify = true;
        }
        if (payload.website != null) profile.setWebsite(payload.website);
        if (payload.description != null) profile.setDescription(payload.description);
        if (payload.language != null) profile.setLanguage(payload.language);
        if (payload.isAvailable != null) {
            profile.setIsAvailable(payload.isAvailable);
        }

        if (requiresReverify) {
            profile.setVerified(null);
        }

        NGOProfile saved = ngoProfileRepository.save(profile);
        log.info("NGO profile updated for {}", user.getEmail());
        return ResponseEntity.ok(saved);
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private void validateCitizen(CitizenProfileUpdateDTO dto) throws Exception {
        if (dto.getFullName() != null && dto.getFullName().isBlank()) {
            throw new Exception("Citizen name cannot be empty");
        }
    }


    private void validateLawyer(LawyerProfileUpdate profileUpdate) throws Exception {
        if (profileUpdate.name != null && profileUpdate.name.isEmpty()) {
            log.warn("Lawyer name validation failed - cannot be empty");
            throw new Exception("Lawyer name cannot be empty");
        }
        if (profileUpdate.expertise != null && profileUpdate.expertise.isEmpty()) {
            log.warn("Lawyer expertise validation failed - cannot be empty");
            throw new Exception("Lawyer expertise cannot be empty");
        }
        if (profileUpdate.location != null && profileUpdate.location.isEmpty()) {
            log.warn("Lawyer location validation failed - cannot be empty");
            throw new Exception("Lawyer location cannot be empty");
        }
    }

    private void validateNgo(NgoProfileUpdate profileUpdate) throws Exception {
        if (profileUpdate.organization != null && profileUpdate.organization.isEmpty()) {
            log.warn("NGO organization validation failed - cannot be empty");
            throw new Exception("Organization name cannot be empty");
        }
        if (profileUpdate.contactInfo != null && profileUpdate.contactInfo.isEmpty()) {
            log.warn("NGO contact info validation failed - cannot be empty");
            throw new Exception("Contact info cannot be empty");
        }
    }

    record LawyerProfileUpdate(
            String name,
            String expertise,
            String location,
            String contactInfo,
            String barRegistrationNo,
            String specialization,
            Integer experienceYears,
            String city,
            Double latitude,
            Double longitude,   
            String bio,
            String language,
            Boolean isAvailable
    ) {}

    record NgoProfileUpdate(
            String organization,
            String contactInfo,
            String location,
            String ngoName,
            String registrationNo,
            String city,
            Double latitude,
            Double longitude,
            String website,
            String description,
            String language,
            Boolean isAvailable
    ) {}
}
