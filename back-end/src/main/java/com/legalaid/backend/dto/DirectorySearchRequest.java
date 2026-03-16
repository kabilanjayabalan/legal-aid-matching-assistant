package com.legalaid.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectorySearchRequest {
    private String query;
    private String type; // "LAWYER", "NGO", or "BOTH"
    private String expertise; // Matches 'specialization' for Lawyers and 'focusArea' for NGOs
    private String location; // Matches 'city'
    private String language; // Language filter
    private Boolean isVerified;
    private int page = 0;
    private int size = 10;
    private String sortBy = "id";
    private String sortDir = "asc"; // "asc" or "desc"
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private Integer sensitivity; // 0–100
    private String locationType; // "LIVE" or "PROFILE"
}

