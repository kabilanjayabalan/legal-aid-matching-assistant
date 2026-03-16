package com.legalaid.backend.util;

import java.time.LocalDateTime;

import com.legalaid.backend.model.Case;
import com.legalaid.backend.model.CasePriority;
import com.legalaid.backend.model.CaseStatus;
import com.legalaid.backend.model.ChatMessage;
import com.legalaid.backend.model.DirectoryLawyer;
import com.legalaid.backend.model.DirectoryNgo;
import com.legalaid.backend.model.LawyerProfile;
import com.legalaid.backend.model.Match;
import com.legalaid.backend.model.MatchStatus;
import com.legalaid.backend.model.NGOProfile;
import com.legalaid.backend.model.Notification;
import com.legalaid.backend.model.ProviderType;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.model.UserStatus;

/**
 * Utility class for building test data objects
 * Provides consistent mock data across all tests
 */
public class TestDataBuilder {

    public static User createUser(Integer id, String email, String fullName, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setUsername(email.split("@")[0]);
        user.setPassword("encodedPassword123");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    public static Case createCase(Integer id, String title, String category, User createdBy) {
        Case caseObj = new Case();
        caseObj.setId(id);
        caseObj.setTitle(title);
        caseObj.setDescription("Test case description");
        caseObj.setCategory(category);
        caseObj.setLocation("Test Location");
        caseObj.setCity("Test City");
        caseObj.setStatus(CaseStatus.OPEN);
        caseObj.setPriority(CasePriority.MEDIUM);
        caseObj.setCreatedBy(createdBy);
        caseObj.setCreatedAt(LocalDateTime.now());
        caseObj.setContactInfo("test@example.com");
        caseObj.setIsUrgent(false);
        return caseObj;
    }

    public static LawyerProfile createLawyerProfile(Integer id, User user, String specialization) {
        LawyerProfile profile = new LawyerProfile();
        profile.setId(id);
        profile.setUser(user);
        profile.setBarRegistrationNo("BAR" + id);  // Correct field name
        profile.setSpecialization(specialization);
        profile.setExperienceYears(5);  // Correct field name
        profile.setCity("Test City");
        profile.setContactInfo("1234567890");  // Correct field name
        profile.setIsAvailable(true);
        profile.setVerified(true);
        profile.setCreatedAt(LocalDateTime.now());
        return profile;
    }

    public static NGOProfile createNGOProfile(Integer id, User user, String organizationName) {
        NGOProfile profile = new NGOProfile();
        profile.setId(id);
        profile.setUser(user);
        profile.setNgoName(organizationName);  // Correct field name
        profile.setRegistrationNo("REG" + id);  // Correct field name
        profile.setDescription("Human Rights and Legal Aid");  // Correct field name
        profile.setCity("Test City");
        profile.setContactInfo("0987654321");  // Correct field name
        profile.setIsAvailable(true);
        profile.setVerified(true);
        profile.setCreatedAt(LocalDateTime.now());
        return profile;
    }

    public static Match createMatch(Integer id, Case caseObj, ProviderType providerType, Integer providerId) {
        Match match = new Match();
        match.setId(id);
        match.setCaseObj(caseObj);
        match.setProviderType(providerType);
        match.setProviderId(providerId);
        match.setScore(85);
        match.setStatus(MatchStatus.PENDING);
        match.setCreatedAt(LocalDateTime.now());
        return match;
    }

    public static ChatMessage createChatMessage(Long id, Integer matchId, User sender, String content) {
        ChatMessage message = new ChatMessage();
        message.setId(id);  // Long type
        message.setMatchId(matchId);
        message.setSenderId(sender.getId());
        message.setMessage(content);  // Correct field name
        message.setCreatedAt(LocalDateTime.now());  // Correct field name
        return message;
    }

    public static Notification createNotification(Long id, Long userId, String type, String message) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setReferenceId("REF" + id);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }

    public static DirectoryLawyer createDirectoryLawyer(Integer id, String fullName, String barRegistrationId) {
        DirectoryLawyer lawyer = new DirectoryLawyer();
        lawyer.setId(id);
        lawyer.setFullName(fullName);
        lawyer.setBarRegistrationId(barRegistrationId);
        lawyer.setSpecialization("Criminal Law");
        lawyer.setCity("Test City");
        lawyer.setContactNumber("1234567890");
        lawyer.setEmail("lawyer" + id + "@example.com");
        lawyer.setSource("EXTERNAL_IMPORT");
        lawyer.setVerified(false);  // Correct method name
        lawyer.setCreatedAt(LocalDateTime.now());
        return lawyer;
    }

    public static DirectoryNgo createDirectoryNgo(Integer id, String name, String registrationNumber) {
        DirectoryNgo ngo = new DirectoryNgo();
        ngo.setId(id);
        ngo.setOrgName(name);
        ngo.setRegistrationNumber(registrationNumber);
        ngo.setFocusArea("Human Rights and Legal Aid");  // Single field, not array
        ngo.setCity("Test City");
        ngo.setContactNumber("0987654321");
        ngo.setEmail("ngo" + id + "@example.com");
        ngo.setWebsite("http://ngo" + id + ".org");
        ngo.setSource("EXTERNAL_IMPORT");
        ngo.setVerified(false);  // Correct method name
        ngo.setCreatedAt(LocalDateTime.now());
        return ngo;
    }

    // Note: Appointment model has a different structure
    // Create appointments directly in tests if needed with matching fields
}

