package com.legalaid.backend.dto;

import lombok.Data;

@Data
public class NGOProfileDTO {
    private String ngoName;
    private String registrationNo;
    private String city;
    private String website;
    private String description;
    private String contactInfo;
    private String language;
    private Boolean isAvailable;
    private Double latitude;
    private Double longitude;
}
