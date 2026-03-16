package com.legalaid.backend.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.legalaid.backend.dto.LawyerProfileDTO;
import com.legalaid.backend.dto.NGOProfileDTO;
import com.legalaid.backend.dto.UserProfileDTO;
import com.legalaid.backend.model.LawyerProfile;
import com.legalaid.backend.model.NGOProfile;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.repository.LawyerProfileRepository;
import com.legalaid.backend.repository.NGOProfileRepository;
import com.legalaid.backend.repository.UserRepository;
import com.legalaid.backend.service.GeocodingService;
import com.legalaid.backend.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LawyerProfileRepository lawyerProfileRepository;
    private final NGOProfileRepository ngoProfileRepository;
    private final UserService userService;
    private final GeocodingService geocodingService;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          LawyerProfileRepository lawyerProfileRepository,
                          NGOProfileRepository ngoProfileRepository,
                          UserService userService,
                          GeocodingService geocodingService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.lawyerProfileRepository = lawyerProfileRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.userService = userService;
        this.geocodingService = geocodingService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return ResponseEntity.ok(userService.getUserProfile(email));
    }

    @PutMapping("/profile/lawyer")
    public ResponseEntity<UserProfileDTO> updateLawyerProfile(@RequestBody LawyerProfileDTO profileDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return ResponseEntity.ok(userService.updateLawyerProfile(email, profileDTO));
    }

    @PutMapping("/profile/ngo")
    public ResponseEntity<UserProfileDTO> updateNGOProfile(@RequestBody NGOProfileDTO profileDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return ResponseEntity.ok(userService.updateNGOProfile(email, profileDTO));
    }

    /**
     * Update user with role-based field validation
     * Different roles have different restrictions on field updates
     * Email cannot be updated once logged in
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody User updateUser) {
        log.info("Updating user with id: {}", id);
        User currentUser = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        // PREVENT EMAIL UPDATE - Email cannot be changed once user is logged in
        if (updateUser.getEmail() != null && !updateUser.getEmail().equals(currentUser.getEmail())) {
            log.warn("Email update attempted for user id: {}. Email updates are not allowed.", id);
            throw new IllegalArgumentException("Email cannot be updated after account creation");
        }

        // Role-based field validation
        log.debug("Validating update fields for user role: {}", currentUser.getRole());
        validateUpdateFieldsByRole(currentUser, updateUser);

        // Update fields (excluding email)
        if (updateUser.getUsername() != null && !updateUser.getUsername().isEmpty()) {
            log.debug("Updating username for user id: {}", id);
            currentUser.setUsername(updateUser.getUsername());
        }
        // Email is NOT updated - intentionally skipped
        if (updateUser.getPassword() != null && !updateUser.getPassword().isEmpty()) {
            log.debug("Updating password for user id: {}", id);
            // Always encode new password before saving
            currentUser.setPassword(passwordEncoder.encode(updateUser.getPassword()));
        }
        if (updateUser.getRole() != null) {
            log.debug("Updating role for user id: {} to {}", id, updateUser.getRole());
            currentUser.setRole(updateUser.getRole());
        }

        User updatedUser = userRepository.save(currentUser);
        log.info("User with id: {} updated successfully", id);
        return ResponseEntity.ok().body(updatedUser);
    }

    @PutMapping("/lawyers/{username}/profile")
    public ResponseEntity<?> updateLawyerProfile(@PathVariable String username,
                                                 @RequestBody LawyerProfileUpdate payload) {
        log.info("Updating lawyer profile for username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

        if (user.getRole() != Role.LAWYER) {
            log.warn("Non-lawyer user attempted to update lawyer profile. Username: {}", username);
            throw new IllegalArgumentException("Only users with LAWYER role can update lawyer profiles");
        }

        LawyerProfile profile = lawyerProfileRepository.findByUser(user).orElse(new LawyerProfile());
        profile.setUser(user);

        // legacy fields
        if (payload.name != null) profile.setName(payload.name);
        if (payload.expertise != null) profile.setExpertise(payload.expertise);
        if (payload.location != null) profile.setLocation(payload.location);
        if (payload.contactInfo != null) profile.setContactInfo(payload.contactInfo);

        // new columns from migration
        if (payload.barRegistrationNo != null) profile.setBarRegistrationNo(payload.barRegistrationNo);
        if (payload.specialization != null) profile.setSpecialization(payload.specialization);
        if (payload.experienceYears != null) profile.setExperienceYears(payload.experienceYears);
        if (payload.city != null) profile.setCity(payload.city);
        if (payload.bio != null) profile.setBio(payload.bio);
        if (payload.language != null) profile.setLanguage(payload.language);
        
        // Coordinates
        if (payload.latitude != null) profile.setLatitude(payload.latitude);
        if (payload.longitude != null) profile.setLongitude(payload.longitude);

        // Geocoding fallback if coordinates missing
        if (profile.getLatitude() == null || profile.getLongitude() == null) {
            String cityToUse = payload.city != null ? payload.city : profile.getCity();
            if (StringUtils.hasText(cityToUse)) {
                Optional<double[]> coords = geocodingService.getCoordinates(cityToUse);
                if (coords.isPresent()) {
                    profile.setLatitude(coords.get()[0]);
                    profile.setLongitude(coords.get()[1]);
                }
            }
        }

        profile.setVerified(null);

        LawyerProfile saved = lawyerProfileRepository.save(profile);
        log.info("Lawyer profile updated successfully for username: {}", username);
        return ResponseEntity.ok().body(saved);
    }

    @PutMapping("/ngos/{username}/profile")
    public ResponseEntity<?> updateNgoProfile(@PathVariable String username,
                                              @RequestBody NgoProfileUpdate payload) {
        log.info("Updating NGO profile for username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

        if (user.getRole() != Role.NGO) {
            log.warn("Non-NGO user attempted to update NGO profile. Username: {}", username);
            throw new IllegalArgumentException("Only users with NGO role can update NGO profiles");
        }

        NGOProfile profile = ngoProfileRepository.findByUser(user).orElse(new NGOProfile());
        profile.setUser(user);

        // legacy fields
        if (payload.organization != null) profile.setOrganization(payload.organization);
        if (payload.contactInfo != null) profile.setContactInfo(payload.contactInfo);
        if (payload.location != null) profile.setLocation(payload.location);

        // new columns from migration
        if (payload.ngoName != null) profile.setNgoName(payload.ngoName);
        if (payload.registrationNo != null) profile.setRegistrationNo(payload.registrationNo);
        if (payload.city != null) profile.setCity(payload.city);
        if (payload.website != null) profile.setWebsite(payload.website);
        if (payload.description != null) profile.setDescription(payload.description);
        if (payload.language != null) profile.setLanguage(payload.language);
        
        // Coordinates
        if (payload.latitude != null) profile.setLatitude(payload.latitude);
        if (payload.longitude != null) profile.setLongitude(payload.longitude);

        // Geocoding fallback if coordinates missing
        if (profile.getLatitude() == null || profile.getLongitude() == null) {
            String cityToUse = payload.city != null ? payload.city : profile.getCity();
            if (StringUtils.hasText(cityToUse)) {
                Optional<double[]> coords = geocodingService.getCoordinates(cityToUse);
                if (coords.isPresent()) {
                    profile.setLatitude(coords.get()[0]);
                    profile.setLongitude(coords.get()[1]);
                }
            }
        }

        profile.setVerified(null);

        NGOProfile saved = ngoProfileRepository.save(profile);
        log.info("NGO profile updated successfully for username: {}", username);
        return ResponseEntity.ok().body(saved);
    }

    /**
     * Validates update fields based on user role
     * Different roles have different restrictions on field updates
     */
    private void validateUpdateFieldsByRole(User currentUser, User updateUser) {
        Role userRole = currentUser.getRole();

        switch (userRole) {
            case CITIZEN:
                validateCitizenUpdate(currentUser, updateUser);
                break;
            case LAWYER:
                validateLawyerUpdate(currentUser, updateUser);
                break;
            case NGO:
                validateNgoUpdate(currentUser, updateUser);
                break;
            case ADMIN:
                validateAdminUpdate(currentUser, updateUser);
                break;
            default:
                throw new IllegalArgumentException("Unknown role: " + userRole);
        }
    }

    /**
     * CITIZEN role: Can update basic info but not username, email, or role
     */
    private void validateCitizenUpdate(User currentUser, User updateUser) {
        if (updateUser.getUsername() != null && !updateUser.getUsername().equals(currentUser.getUsername())) {
            throw new IllegalArgumentException("Citizens cannot change their username");
        }
        if (updateUser.getEmail() != null && !updateUser.getEmail().equals(currentUser.getEmail())) {
            throw new IllegalArgumentException("Citizens cannot change their email");
        }
        if (updateUser.getRole() != null && !updateUser.getRole().equals(currentUser.getRole())) {
            throw new IllegalArgumentException("Citizens cannot change their role");
        }
        if (updateUser.getPassword() != null && updateUser.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
    }

    /**
     * LAWYER role: Can update profile info but restricted from changing role, username, or email
     */
    private void validateLawyerUpdate(User currentUser, User updateUser) {
        if (updateUser.getRole() != null && !updateUser.getRole().equals(currentUser.getRole())) {
            throw new IllegalArgumentException("Lawyers cannot change their role");
        }
        if (updateUser.getUsername() != null && !updateUser.getUsername().equals(currentUser.getUsername())) {
            throw new IllegalArgumentException("Username change requires admin approval");
        }
        if (updateUser.getEmail() != null && !updateUser.getEmail().equals(currentUser.getEmail())) {
            throw new IllegalArgumentException("Email cannot be changed");
        }
        if (updateUser.getPassword() != null && updateUser.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
    }

    /**
     * NGO role: Can update organization info but not role, username, or email
     */
    private void validateNgoUpdate(User currentUser, User updateUser) {
        if (updateUser.getRole() != null && !updateUser.getRole().equals(currentUser.getRole())) {
            throw new IllegalArgumentException("NGOs cannot change their role");
        }
        if (updateUser.getUsername() != null && !updateUser.getUsername().equals(currentUser.getUsername())) {
            throw new IllegalArgumentException("Username change requires admin approval");
        }
        if (updateUser.getEmail() != null && !updateUser.getEmail().equals(currentUser.getEmail())) {
            throw new IllegalArgumentException("Email cannot be changed");
        }
        if (updateUser.getPassword() != null && updateUser.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
    }

    /**
     * ADMIN role: Can update any field including roles
     */
    private void validateAdminUpdate(User currentUser, User updateUser) {
        if (updateUser.getPassword() != null && updateUser.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (updateUser.getEmail() != null && updateUser.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        //ADMINs have no other restrictions
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
            String bio,
            String language,
            Double latitude,
            Double longitude
    ) {}

    record NgoProfileUpdate(
            String organization,
            String contactInfo,
            String location,
            String ngoName,
            String registrationNo,
            String city,
            String website,
            String description,
            String language,
            Double latitude,
            Double longitude
    ) {}
}
