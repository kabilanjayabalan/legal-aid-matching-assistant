package com.legalaid.backend.dto.analytics;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CaseAnalyticsResponse {

    private long totalCases;
    private long resolvedCases;
    private Map<String,Map<String,Long>> casesByCategory;
}
