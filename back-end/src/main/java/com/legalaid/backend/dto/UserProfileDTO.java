package com.legalaid.backend.dto;

import lombok.Data;

@Data
public class UserProfileDTO {
    private Integer id;
    private String fullName;
    private String email;
    private String role;
    
    // Lawyer specific fields
    private String barRegistrationNo;
    private String specialization;
    private Integer experienceYears;
    private String bio;
    
    // NGO specific fields
    private String ngoName;
    private String registrationNo;
    private String website;
    private String description;
    
    // Common fields
    private String city;
    private String contactInfo;
    private Boolean verified;
    private String language;
    private Boolean isAvailable;
    private Double latitude;
    private Double longitude;
}
