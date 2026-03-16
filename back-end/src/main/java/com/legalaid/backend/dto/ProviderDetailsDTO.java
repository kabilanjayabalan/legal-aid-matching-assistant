package com.legalaid.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for provider (Lawyer/NGO) details to be displayed in the frontend.
 * Combines data from both internal profiles and external directory.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderDetailsDTO {
    
    // Basic Information
    private Integer id;
    private String name;              // For lawyers: name, for NGOs: ngoName
    private String providerType;      // "LAWYER" or "NGO"
    private Boolean verified;
    
    // Location
    private String city;
    private String location;
    
    // Contact Information
    private String email;
    private String contactInfo;
    private String contactNumber;
    private String website;
    
    // Professional Details
    private String specialization;    // For lawyers
    private String focusArea;        // For NGOs
    private String expertise;        // For lawyers (bio/expertise)
    private String description;       // For NGOs
    private String bio;              // For lawyers
    private Integer experienceYears;  // For lawyers
    
    // Registration Details
    private String barRegistrationNo; // For lawyers
    private String registrationNo;    // For NGOs
    
    // Additional
    private String language;
    private Boolean isAvailable;
    private LocalDateTime createdAt;
    private String source;            // "INTERNAL" or "EXTERNAL"
    private Double latitude;
    private Double longitude;
}
