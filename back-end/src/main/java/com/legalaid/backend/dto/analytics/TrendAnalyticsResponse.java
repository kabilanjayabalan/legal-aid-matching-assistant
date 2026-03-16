package com.legalaid.backend.dto.analytics;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrendAnalyticsResponse {
    private String range;
    private List<TimePointDTO> users;
    private List<TimePointDTO> cases;
    private List<TimePointDTO> matches;
}