package com.legalaid.backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GeoCountDTO {
    private Double latitude;
    private Double longitude;
    private Long count;
}

