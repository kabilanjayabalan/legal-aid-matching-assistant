package com.legalaid.backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LocationBreakdownDTO {
    private long lawyers;
    private long ngos;
    private long citizens;
    private long cases;
}
