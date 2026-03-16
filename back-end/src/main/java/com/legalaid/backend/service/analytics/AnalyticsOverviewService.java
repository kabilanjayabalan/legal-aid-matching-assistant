package com.legalaid.backend.service.analytics;

import org.springframework.stereotype.Service;

import com.legalaid.backend.dto.analytics.AnalyticsOverviewResponse;
import com.legalaid.backend.dto.analytics.AppointmentAnalyticsResponse;
import com.legalaid.backend.dto.analytics.CaseAnalyticsResponse;
import com.legalaid.backend.dto.analytics.MatchAnalyticsResponse;
import com.legalaid.backend.dto.analytics.UserRoleAnalyticsResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsOverviewService {

    private final UserAnalyticsService userAnalyticsService;
    private final CaseAnalyticsService caseAnalyticsService;
    private final MatchAnalyticsService matchAnalyticsService;
    private final AppointmentAnalyticsService appointmentAnalyticsService;

    public AnalyticsOverviewResponse getOverview() {

    UserRoleAnalyticsResponse userRoles =
            userAnalyticsService.getUserRoleDistribution();

    CaseAnalyticsResponse caseAnalytics =
            caseAnalyticsService.getCaseAnalytics();

    MatchAnalyticsResponse matchAnalytics =
            matchAnalyticsService.getMatchAnalytics();

    AppointmentAnalyticsResponse appointmentAnalytics =
            appointmentAnalyticsService.getAppointmentAnalytics();

    return new AnalyticsOverviewResponse(
            userRoles.getTotalUsers(),
            userRoles.getLawyers(),
            userRoles.getNgos(),
            caseAnalytics.getTotalCases(),
            matchAnalytics.getTotalMatches(),
            caseAnalytics.getResolvedCases(),
            appointmentAnalytics.getActiveAppointments()
    );
}

}
