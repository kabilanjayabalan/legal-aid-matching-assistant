package com.legalaid.backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TotalGeoCountDTO {
    private Double latitude;
    private Double longitude;
    private Long total;
    private LocationBreakdownDTO breakdown;
}
