package com.legalaid.backend.service;

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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final LawyerProfileRepository lawyerProfileRepository;
    private final NGOProfileRepository ngoProfileRepository;
    private final GeocodingService geocodingService;

    public UserService(UserRepository userRepository,
                       LawyerProfileRepository lawyerProfileRepository,
                       NGOProfileRepository ngoProfileRepository,
                       GeocodingService geocodingService) {
        this.userRepository = userRepository;
        this.lawyerProfileRepository = lawyerProfileRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.geocodingService = geocodingService;
    }

    public UserProfileDTO getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserProfileDTO profileDTO = new UserProfileDTO();
        profileDTO.setId(user.getId());
        profileDTO.setFullName(user.getFullName());
        profileDTO.setEmail(user.getEmail());
        profileDTO.setRole(user.getRole().name());

        if (user.getRole() == Role.LAWYER) {
            lawyerProfileRepository.findByUser(user).ifPresent(profile -> {
                profileDTO.setBarRegistrationNo(profile.getBarRegistrationNo());
                profileDTO.setSpecialization(profile.getSpecialization());
                profileDTO.setExperienceYears(profile.getExperienceYears());
                profileDTO.setCity(profile.getCity());
                profileDTO.setBio(profile.getBio());
                profileDTO.setContactInfo(profile.getContactInfo());
                profileDTO.setVerified(profile.getVerified());
                profileDTO.setLanguage(profile.getLanguage());
                profileDTO.setIsAvailable(profile.getIsAvailable());
                profileDTO.setLatitude(profile.getLatitude());
                profileDTO.setLongitude(profile.getLongitude());
            });
        } else if (user.getRole() == Role.NGO) {
            ngoProfileRepository.findByUser(user).ifPresent(profile -> {
                profileDTO.setNgoName(profile.getNgoName());
                profileDTO.setRegistrationNo(profile.getRegistrationNo());
                profileDTO.setCity(profile.getCity());
                profileDTO.setWebsite(profile.getWebsite());
                profileDTO.setDescription(profile.getDescription());
                profileDTO.setContactInfo(profile.getContactInfo());
                profileDTO.setVerified(profile.getVerified());
                profileDTO.setLanguage(profile.getLanguage());
                profileDTO.setIsAvailable(profile.getIsAvailable());
                profileDTO.setLatitude(profile.getLatitude());
                profileDTO.setLongitude(profile.getLongitude());
            });
        }

        return profileDTO;
    }

    @Transactional
    public UserProfileDTO updateLawyerProfile(String email, LawyerProfileDTO lawyerDTO) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getRole() != Role.LAWYER) {
            throw new IllegalArgumentException("User is not a lawyer");
        }

        LawyerProfile profile = lawyerProfileRepository.findByUser(user)
                .orElse(new LawyerProfile());

        profile.setUser(user);
        profile.setBarRegistrationNo(lawyerDTO.getBarRegistrationNo());
        profile.setSpecialization(lawyerDTO.getSpecialization());
        profile.setExperienceYears(lawyerDTO.getExperienceYears());
        profile.setCity(lawyerDTO.getCity());
        profile.setBio(lawyerDTO.getBio());
        profile.setContactInfo(lawyerDTO.getContactInfo());
        profile.setLanguage(lawyerDTO.getLanguage());
        profile.setIsAvailable(lawyerDTO.getIsAvailable());
        
        // Update coordinates if provided manually, otherwise fetch from city
        if (lawyerDTO.getLatitude() != null && lawyerDTO.getLongitude() != null) {
            profile.setLatitude(lawyerDTO.getLatitude());
            profile.setLongitude(lawyerDTO.getLongitude());
        } else if (StringUtils.hasText(lawyerDTO.getCity())) {
            // Only geocode if coordinates are missing or if city has changed (and no manual coords provided)
            // Here we assume if manual coords are null, we should try to fill them
            Optional<double[]> coords = geocodingService.getCoordinates(lawyerDTO.getCity());
            if (coords.isPresent()) {
                profile.setLatitude(coords.get()[0]);
                profile.setLongitude(coords.get()[1]);
            }
        }

        lawyerProfileRepository.save(profile);

        // Update user full name if provided
        if (lawyerDTO.getFullName() != null && !lawyerDTO.getFullName().isEmpty()) {
            user.setFullName(lawyerDTO.getFullName());
            userRepository.save(user);
        }

        return getUserProfile(email);
    }

    @Transactional
    public UserProfileDTO updateNGOProfile(String email, NGOProfileDTO ngoDTO) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getRole() != Role.NGO) {
            throw new IllegalArgumentException("User is not an NGO");
        }

        NGOProfile profile = ngoProfileRepository.findByUser(user)
                .orElse(new NGOProfile());

        profile.setUser(user);
        profile.setNgoName(ngoDTO.getNgoName());
        profile.setRegistrationNo(ngoDTO.getRegistrationNo());
        profile.setCity(ngoDTO.getCity());
        profile.setWebsite(ngoDTO.getWebsite());
        profile.setDescription(ngoDTO.getDescription());
        profile.setContactInfo(ngoDTO.getContactInfo());
        profile.setLanguage(ngoDTO.getLanguage());
        profile.setIsAvailable(ngoDTO.getIsAvailable());

        // Update coordinates if provided manually, otherwise fetch from city
        if (ngoDTO.getLatitude() != null && ngoDTO.getLongitude() != null) {
            profile.setLatitude(ngoDTO.getLatitude());
            profile.setLongitude(ngoDTO.getLongitude());
        } else if (StringUtils.hasText(ngoDTO.getCity())) {
            Optional<double[]> coords = geocodingService.getCoordinates(ngoDTO.getCity());
            if (coords.isPresent()) {
                profile.setLatitude(coords.get()[0]);
                profile.setLongitude(coords.get()[1]);
            }
        }

        ngoProfileRepository.save(profile);

        // Update user full name (as NGO name) if provided
        if (ngoDTO.getNgoName() != null && !ngoDTO.getNgoName().isEmpty()) {
            user.setFullName(ngoDTO.getNgoName());
            userRepository.save(user);
        }

        return getUserProfile(email);
    }
}
