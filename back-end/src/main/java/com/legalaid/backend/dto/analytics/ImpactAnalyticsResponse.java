package com.legalaid.backend.dto.analytics;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImpactAnalyticsResponse {

    private List<GeoCountDTO> lawyersByLocation;
    private List<GeoCountDTO> ngosByLocation;
    private List<GeoCountDTO> citizensByLocation;
    private List<GeoCountDTO> casesByLocation;
    private List<TotalGeoCountDTO> totalByLocation;

}

