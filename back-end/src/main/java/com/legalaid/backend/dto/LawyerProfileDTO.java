package com.legalaid.backend.dto;

import lombok.Data;

@Data
public class LawyerProfileDTO {
    private String fullName;
    private String barRegistrationNo;
    private String specialization;
    private Integer experienceYears;
    private String city;
    private String bio;
    private String contactInfo;
    private String language;
    private Boolean isAvailable;
    private Double latitude;
    private Double longitude;
}
