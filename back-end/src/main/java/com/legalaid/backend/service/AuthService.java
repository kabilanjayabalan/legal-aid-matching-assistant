package com.legalaid.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.legalaid.backend.model.DirectoryLawyer;
import com.legalaid.backend.model.DirectoryNgo;
import com.legalaid.backend.model.LawyerProfile;
import com.legalaid.backend.model.NGOProfile;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.model.UserStatus;
import com.legalaid.backend.repository.DirectoryLawyerRepository;
import com.legalaid.backend.repository.DirectoryNgoRepository;
import com.legalaid.backend.repository.LawyerProfileRepository;
import com.legalaid.backend.repository.NGOProfileRepository;
import com.legalaid.backend.repository.ProfileRepository;
import com.legalaid.backend.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final DirectoryLawyerRepository directoryLawyerRepository;
    private final DirectoryNgoRepository directoryNgoRepository;
    private final ProfileRepository profileRepository;
    private final LawyerProfileRepository lawyerProfileRepository;
    private final NGOProfileRepository ngoProfileRepository;

    public AuthService(UserRepository userRepository,
                      DirectoryLawyerRepository directoryLawyerRepository,
                      DirectoryNgoRepository directoryNgoRepository,
                      ProfileRepository profileRepository,
                      LawyerProfileRepository lawyerProfileRepository,
                      NGOProfileRepository ngoProfileRepository) {
        this.userRepository = userRepository;
        this.directoryLawyerRepository = directoryLawyerRepository;
        this.directoryNgoRepository = directoryNgoRepository;
        this.profileRepository = profileRepository;
        this.lawyerProfileRepository = lawyerProfileRepository;
        this.ngoProfileRepository = ngoProfileRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRole().name())
                .build();
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public User approveUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with id: " + userId));
        user.setApproved(true);
        return userRepository.save(user);
    }

    public User approveUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));
        user.setApproved(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setStatusChangedAt(LocalDateTime.now());
        user.setStatusReason("Approved by admin");
        return userRepository.save(user);
    }

    public User rejectUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));
        user.setApproved(false);
        user.setStatus(UserStatus.BLOCKED);
        user.setStatusChangedAt(LocalDateTime.now());
        user.setStatusReason("Rejected by admin");
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));
    }

    @Transactional
    public void registerUserWithClaim(User user, String referenceId, Object signupData) {
        // Save the user first
            // ✅ SET STATUS BEFORE SAVE (CRITICAL)
        if (user.getRole() == Role.CITIZEN) {
            user.setStatus(UserStatus.ACTIVE);
            user.setStatusChangedAt(LocalDateTime.now());
            user.setStatusReason("Citizen auto-activated on registration");
        } else {
            user.setStatus(UserStatus.PENDING);
            user.setStatusChangedAt(LocalDateTime.now());
            user.setStatusReason("Awaiting admin approval");
        }
        User savedUser = userRepository.save(user);
        log.debug("User saved with ID: {}", savedUser.getId());

        // If referenceId is provided and user is LAWYER or NGO, try to claim profile
        if (referenceId != null && !referenceId.trim().isEmpty()) {
            if (savedUser.getRole() == Role.LAWYER) {
                claimLawyerProfile(savedUser, referenceId.trim(), signupData);
            } else if (savedUser.getRole() == Role.NGO) {
                claimNgoProfile(savedUser, referenceId.trim(), signupData);
            } else {
                log.debug("Reference ID provided but role is not LAWYER or NGO, ignoring referenceId");
            }
        } else {
            // Create empty profile for LAWYER/NGO even without referenceId
            if (savedUser.getRole() == Role.LAWYER) {
                createEmptyLawyerProfile(savedUser, signupData);
            } else if (savedUser.getRole() == Role.NGO) {
                createEmptyNgoProfile(savedUser, signupData);
            }
        }
    }

    private void claimLawyerProfile(User user, String barRegistrationId, Object signupData) {
        log.info("Attempting to claim lawyer profile with barRegistrationId: {}", barRegistrationId);

        // Check if this barRegistrationId is already claimed by another user
        Optional<LawyerProfile> existingProfile = lawyerProfileRepository.findByBarRegistrationNo(barRegistrationId);
        if (existingProfile.isPresent()) {
            log.warn("Bar registration ID {} is already claimed by user {}", barRegistrationId, existingProfile.get().getUser().getId());
            throw new IllegalArgumentException("Error: This bar registration ID is already claimed by another user!");
        }

        // Look up the directory entry
        DirectoryLawyer directoryLawyer = directoryLawyerRepository.findByBarRegistrationId(barRegistrationId);
        if (directoryLawyer == null) {
            log.debug("Bar registration ID {} not found in directory, proceeding with standard registration", barRegistrationId);
            // Create profile with signup data for standard registration
            createEmptyLawyerProfile(user, signupData);
            return;
        }

        log.info("Found directory entry for barRegistrationId: {}, auto-approving and creating profile", barRegistrationId);

        // Create and populate LawyerProfile
        LawyerProfile lawyerProfile = new LawyerProfile();
        lawyerProfile.setUser(user);
        lawyerProfile.setName(directoryLawyer.getFullName());
        lawyerProfile.setExpertise(directoryLawyer.getSpecialization());
        lawyerProfile.setLocation(directoryLawyer.getCity());
        lawyerProfile.setCity(directoryLawyer.getCity());
        lawyerProfile.setSpecialization(directoryLawyer.getSpecialization());
        lawyerProfile.setBarRegistrationNo(directoryLawyer.getBarRegistrationId());
        lawyerProfile.setContactInfo(directoryLawyer.getContactNumber() != null ?
            "Phone: " + directoryLawyer.getContactNumber() +
            (directoryLawyer.getEmail() != null ? "\nEmail: " + directoryLawyer.getEmail() : "") :
            (directoryLawyer.getEmail() != null ? "Email: " + directoryLawyer.getEmail() : null));
        lawyerProfile.setVerified(true);
        lawyerProfile.setCreatedAt(LocalDateTime.now());
        lawyerProfile.setLanguage(null); // Can be set later by user
        LawyerProfile savedProfile = lawyerProfileRepository.save(lawyerProfile);

        // Auto-approve the user and link profile
        user.setApproved(true);
        user.setProfileId(savedProfile.getId());
        user.setStatus(UserStatus.ACTIVE);
        user.setStatusChangedAt(LocalDateTime.now());
        user.setStatusReason("Auto-approved with directory verification");
        userRepository.save(user);

        // Delete the directory entry after successful claim
        directoryLawyerRepository.delete(directoryLawyer);
        log.info("Deleted directory lawyer entry with barRegistrationId: {} after successful claim", barRegistrationId);

        log.info("Profile and LawyerProfile created and verified for lawyer user ID: {} with barRegistrationId: {}", user.getId(), barRegistrationId);
    }

    private void claimNgoProfile(User user, String registrationNumber, Object signupData) {
        log.info("Attempting to claim NGO profile with registrationNumber: {}", registrationNumber);

        // Check if this registrationNumber is already claimed by another user
        Optional<NGOProfile> existingProfile = ngoProfileRepository.findByRegistrationNo(registrationNumber);
        if (existingProfile.isPresent()) {
            log.warn("Registration number {} is already claimed by user {}", registrationNumber, existingProfile.get().getUser().getId());
            throw new IllegalArgumentException("Error: This registration number is already claimed by another user!");
        }

        // Look up the directory entry
        DirectoryNgo directoryNgo = directoryNgoRepository.findByRegistrationNumber(registrationNumber);
        if (directoryNgo == null) {
            log.debug("Registration number {} not found in directory, proceeding with standard registration", registrationNumber);
            // Create profile with signup data for standard registration
            createEmptyNgoProfile(user, signupData);
            return;
        }

        log.info("Found directory entry for registrationNumber: {}, auto-approving and creating profile", registrationNumber);

        // Create and populate NGOProfile
        NGOProfile ngoProfile = new NGOProfile();
        ngoProfile.setUser(user);
        ngoProfile.setNgoName(directoryNgo.getOrgName());
        ngoProfile.setOrganization(directoryNgo.getOrgName());
        ngoProfile.setRegistrationNo(directoryNgo.getRegistrationNumber());
        ngoProfile.setDescription(directoryNgo.getFocusArea());
        ngoProfile.setCity(directoryNgo.getCity());
        ngoProfile.setLocation(directoryNgo.getCity());
        ngoProfile.setWebsite(directoryNgo.getWebsite());
        ngoProfile.setContactInfo(directoryNgo.getContactNumber() != null ?
            "Phone: " + directoryNgo.getContactNumber() +
            (directoryNgo.getEmail() != null ? "\nEmail: " + directoryNgo.getEmail() : "") +
            (directoryNgo.getWebsite() != null ? "\nWebsite: " + directoryNgo.getWebsite() : "") :
            (directoryNgo.getEmail() != null ? "Email: " + directoryNgo.getEmail() : null));
        ngoProfile.setVerified(true);
        ngoProfile.setCreatedAt(LocalDateTime.now());
        ngoProfile.setLanguage(null); // Can be set later by user
        NGOProfile savedProfile = ngoProfileRepository.save(ngoProfile);

        // Auto-approve the user and link profile
        user.setApproved(true);
        user.setProfileId(savedProfile.getId());
        user.setStatus(UserStatus.ACTIVE);
        user.setStatusChangedAt(LocalDateTime.now());
        user.setStatusReason("Auto-approved with directory verification");
        userRepository.save(user);

        // Delete the directory entry after successful claim
        directoryNgoRepository.delete(directoryNgo);
        log.info("Deleted directory NGO entry with registrationNumber: {} after successful claim", registrationNumber);

        log.info("Profile and NGOProfile created and verified for NGO user ID: {} with registrationNumber: {}", user.getId(), registrationNumber);
    }

    private void createEmptyLawyerProfile(User user, Object signupData) {
        log.info("Creating lawyer profile for user ID: {}", user.getId());

        LawyerProfile lawyerProfile = new LawyerProfile();
        lawyerProfile.setUser(user);
        lawyerProfile.setName(user.getFullName());
        lawyerProfile.setCreatedAt(LocalDateTime.now());
        lawyerProfile.setVerified(false);
        lawyerProfile.setIsAvailable(true);

        // If signup data is provided, extract and set lawyer-specific fields
        if (signupData != null) {
            log.info("Extracting lawyer-specific fields from signup data. Data class: {}", signupData.getClass().getName());
            try {
                // Cast to SignupRequest if possible, otherwise use reflection
                if (signupData instanceof com.legalaid.backend.controller.AuthController.SignupRequest signupRequest) {
                    // Direct access to record fields
                    String barRegNo = signupRequest.barRegistrationNo();
                    if (barRegNo != null && !barRegNo.trim().isEmpty()) {
                        lawyerProfile.setBarRegistrationNo(barRegNo.trim());
                        log.info("Set barRegistrationNo: {}", barRegNo);
                    }

                    String spec = signupRequest.specialization();
                    if (spec != null && !spec.trim().isEmpty()) {
                        lawyerProfile.setSpecialization(spec.trim());
                        lawyerProfile.setExpertise(spec.trim());
                        log.info("Set specialization: {}", spec);
                    }

                    Integer exp = signupRequest.experienceYears();
                    if (exp != null) {
                        lawyerProfile.setExperienceYears(exp);
                        log.info("Set experienceYears: {}", exp);
                    }

                    String city = signupRequest.city();
                    if (city != null && !city.trim().isEmpty()) {
                        lawyerProfile.setCity(city.trim());
                        lawyerProfile.setLocation(city.trim());
                        log.info("Set city: {}", city);
                    }

                    String bio = signupRequest.bio();
                    if (bio != null && !bio.trim().isEmpty()) {
                        lawyerProfile.setBio(bio.trim());
                        log.info("Set bio: {}", bio);
                    }

                    String lang = signupRequest.language();
                    if (lang != null && !lang.trim().isEmpty()) {
                        lawyerProfile.setLanguage(lang.trim());
                        log.info("Set language: {}", lang);
                    }

                    String contact = signupRequest.contactInfo();
                    if (contact != null && !contact.trim().isEmpty()) {
                        lawyerProfile.setContactInfo(contact.trim());
                        log.info("Set contactInfo: {}", contact);
                    }
                } else {
                    // Fallback to reflection for other types
                    var dataClass = signupData.getClass();

                    // Bar Registration Number
                    try {
                        var barRegMethod = dataClass.getMethod("barRegistrationNo");
                        String barRegNo = (String) barRegMethod.invoke(signupData);
                        log.info("Extracted barRegistrationNo: {}", barRegNo);
                        if (barRegNo != null && !barRegNo.trim().isEmpty()) {
                            lawyerProfile.setBarRegistrationNo(barRegNo.trim());
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract barRegistrationNo: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }

                    // Specialization
                    try {
                        var specMethod = dataClass.getMethod("specialization");
                        String spec = (String) specMethod.invoke(signupData);
                        log.info("Extracted specialization: {}", spec);
                        if (spec != null && !spec.trim().isEmpty()) {
                            lawyerProfile.setSpecialization(spec.trim());
                            lawyerProfile.setExpertise(spec.trim());
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract specialization: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }

                    // Experience Years
                    try {
                        var expMethod = dataClass.getMethod("experienceYears");
                        Integer exp = (Integer) expMethod.invoke(signupData);
                        log.info("Extracted experienceYears: {}", exp);
                        if (exp != null) {
                            lawyerProfile.setExperienceYears(exp);
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract experienceYears: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }

                    // City
                    try {
                        var cityMethod = dataClass.getMethod("city");
                        String city = (String) cityMethod.invoke(signupData);
                        log.info("Extracted city: {}", city);
                        if (city != null && !city.trim().isEmpty()) {
                            lawyerProfile.setCity(city.trim());
                            lawyerProfile.setLocation(city.trim());
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract city: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }

                    // Bio
                    try {
                        var bioMethod = dataClass.getMethod("bio");
                        String bio = (String) bioMethod.invoke(signupData);
                        log.info("Extracted bio: {}", bio);
                        if (bio != null && !bio.trim().isEmpty()) {
                            lawyerProfile.setBio(bio.trim());
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract bio: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }

                    // Language
                    try {
                        var langMethod = dataClass.getMethod("language");
                        String lang = (String) langMethod.invoke(signupData);
                        log.info("Extracted language: {}", lang);
                        if (lang != null && !lang.trim().isEmpty()) {
                            lawyerProfile.setLanguage(lang.trim());
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract language: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }

                    // Contact Info
                    try {
                        var contactMethod = dataClass.getMethod("contactInfo");
                        String contact = (String) contactMethod.invoke(signupData);
                        log.info("Extracted contactInfo: {}", contact);
                        if (contact != null && !contact.trim().isEmpty()) {
                            lawyerProfile.setContactInfo(contact.trim());
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract contactInfo: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }
                }

            } catch (Exception e) {
                log.error("Error extracting signup data for lawyer profile: {}", e.getMessage(), e);
            }
        } else {
            log.warn("No signup data provided for lawyer profile creation");
        }

        LawyerProfile savedProfile = lawyerProfileRepository.save(lawyerProfile);

        // Link profile to user
        user.setProfileId(savedProfile.getId());
        userRepository.save(user);

        log.info("LawyerProfile created for user ID: {} with profile ID: {}", user.getId(), savedProfile.getId());
    }

    private void createEmptyNgoProfile(User user, Object signupData) {
        log.info("Creating NGO profile for user ID: {}", user.getId());

        NGOProfile ngoProfile = new NGOProfile();
        ngoProfile.setUser(user);
        ngoProfile.setNgoName(user.getFullName());
        ngoProfile.setOrganization(user.getFullName());
        ngoProfile.setCreatedAt(LocalDateTime.now());
        ngoProfile.setVerified(false);
        ngoProfile.setIsAvailable(true);

        // If signup data is provided, extract and set NGO-specific fields
        if (signupData != null) {
            log.info("Extracting NGO-specific fields from signup data. Data class: {}", signupData.getClass().getName());
            try {
                // Cast to SignupRequest if possible, otherwise use reflection
                if (signupData instanceof com.legalaid.backend.controller.AuthController.SignupRequest signupRequest) {
                    // Direct access to record fields
                    String ngoName = signupRequest.ngoName();
                    if (ngoName != null && !ngoName.trim().isEmpty()) {
                        ngoProfile.setNgoName(ngoName.trim());
                        ngoProfile.setOrganization(ngoName.trim());
                        log.info("Set ngoName: {}", ngoName);
                    }

                    String regNo = signupRequest.registrationNo();
                    if (regNo != null && !regNo.trim().isEmpty()) {
                        ngoProfile.setRegistrationNo(regNo.trim());
                        log.info("Set registrationNo: {}", regNo);
                    }

                    String city = signupRequest.city();
                    if (city != null && !city.trim().isEmpty()) {
                        ngoProfile.setCity(city.trim());
                        ngoProfile.setLocation(city.trim());
                        log.info("Set city: {}", city);
                    }

                    String website = signupRequest.website();
                    if (website != null && !website.trim().isEmpty()) {
                        ngoProfile.setWebsite(website.trim());
                        log.info("Set website: {}", website);
                    }

                    String desc = signupRequest.description();
                    if (desc != null && !desc.trim().isEmpty()) {
                        ngoProfile.setDescription(desc.trim());
                        log.info("Set description: {}", desc);
                    }

                    String lang = signupRequest.language();
                    if (lang != null && !lang.trim().isEmpty()) {
                        ngoProfile.setLanguage(lang.trim());
                        log.info("Set language: {}", lang);
                    }

                    String contact = signupRequest.contactInfo();
                    if (contact != null && !contact.trim().isEmpty()) {
                        ngoProfile.setContactInfo(contact.trim());
                        log.info("Set contactInfo: {}", contact);
                    }
                } else {
                    // Fallback to reflection for other types
                    var dataClass = signupData.getClass();

                    // NGO Name
                    try {
                        var ngoNameMethod = dataClass.getMethod("ngoName");
                        String ngoName = (String) ngoNameMethod.invoke(signupData);
                        log.info("Extracted ngoName: {}", ngoName);
                        if (ngoName != null && !ngoName.trim().isEmpty()) {
                            ngoProfile.setNgoName(ngoName.trim());
                            ngoProfile.setOrganization(ngoName.trim());
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract ngoName: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }

                    // Registration Number
                    try {
                        var regNoMethod = dataClass.getMethod("registrationNo");
                        String regNo = (String) regNoMethod.invoke(signupData);
                        log.info("Extracted registrationNo: {}", regNo);
                        if (regNo != null && !regNo.trim().isEmpty()) {
                            ngoProfile.setRegistrationNo(regNo.trim());
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract registrationNo: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }

                    // City
                    try {
                        var cityMethod = dataClass.getMethod("city");
                        String city = (String) cityMethod.invoke(signupData);
                        log.info("Extracted city: {}", city);
                        if (city != null && !city.trim().isEmpty()) {
                            ngoProfile.setCity(city.trim());
                            ngoProfile.setLocation(city.trim());
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract city: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }

                    // Website
                    try {
                        var websiteMethod = dataClass.getMethod("website");
                        String website = (String) websiteMethod.invoke(signupData);
                        log.info("Extracted website: {}", website);
                        if (website != null && !website.trim().isEmpty()) {
                            ngoProfile.setWebsite(website.trim());
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract website: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }

                    // Description
                    try {
                        var descMethod = dataClass.getMethod("description");
                        String desc = (String) descMethod.invoke(signupData);
                        log.info("Extracted description: {}", desc);
                        if (desc != null && !desc.trim().isEmpty()) {
                            ngoProfile.setDescription(desc.trim());
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract description: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }

                    // Language
                    try {
                        var langMethod = dataClass.getMethod("language");
                        String lang = (String) langMethod.invoke(signupData);
                        log.info("Extracted language: {}", lang);
                        if (lang != null && !lang.trim().isEmpty()) {
                            ngoProfile.setLanguage(lang.trim());
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract language: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }

                    // Contact Info
                    try {
                        var contactMethod = dataClass.getMethod("contactInfo");
                        String contact = (String) contactMethod.invoke(signupData);
                        log.info("Extracted contactInfo: {}", contact);
                        if (contact != null && !contact.trim().isEmpty()) {
                            ngoProfile.setContactInfo(contact.trim());
                        }
                    } catch (Exception e) {
                        log.warn("Could not extract contactInfo: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                    }
                }

            } catch (Exception e) {
                log.error("Error extracting signup data for NGO profile: {}", e.getMessage(), e);
            }
        } else {
            log.warn("No signup data provided for NGO profile creation");
        }

        NGOProfile savedProfile = ngoProfileRepository.save(ngoProfile);

        // Link profile to user
        user.setProfileId(savedProfile.getId());
        userRepository.save(user);

        log.info("NGOProfile created for user ID: {} with profile ID: {}", user.getId(), savedProfile.getId());
    }
}

