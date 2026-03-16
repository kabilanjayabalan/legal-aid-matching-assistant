package com.legalaid.backend.controller.analytics;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.legalaid.backend.dto.analytics.AnalyticsOverviewResponse;
import com.legalaid.backend.dto.analytics.AppointmentAnalyticsResponse;
import com.legalaid.backend.dto.analytics.CaseAnalyticsResponse;
import com.legalaid.backend.dto.analytics.ImpactAnalyticsResponse;
import com.legalaid.backend.dto.analytics.MatchAnalyticsResponse;
import com.legalaid.backend.dto.analytics.TrendAnalyticsResponse;
import com.legalaid.backend.dto.analytics.UserRoleAnalyticsResponse;
import com.legalaid.backend.service.analytics.AnalyticsOverviewService;
import com.legalaid.backend.service.analytics.AppointmentAnalyticsService;
import com.legalaid.backend.service.analytics.CaseAnalyticsService;
import com.legalaid.backend.service.analytics.ImpactAnalyticsService;
import com.legalaid.backend.service.analytics.MatchAnalyticsService;
import com.legalaid.backend.service.analytics.TrendAnalyticsService;
import com.legalaid.backend.service.analytics.UserAnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AnalyticsController {

    private final UserAnalyticsService userAnalyticsService;
    private final CaseAnalyticsService caseAnalyticsService;
    private final MatchAnalyticsService matchAnalyticsService;
    private final AppointmentAnalyticsService appointmentAnalyticsService;
    private final AnalyticsOverviewService analyticsOverviewService;
    private final ImpactAnalyticsService impactAnalyticsService;
    private final TrendAnalyticsService trendAnalyticsService;

    /* ================= USERS ================= */

    @GetMapping("/users")      // 4. Pie Charts – Role Distribution
    public ResponseEntity<UserRoleAnalyticsResponse> getUserRoles() {
        return ResponseEntity.ok(userAnalyticsService.getUserRoleDistribution());
    }

    /* ================= CASES ================= */

    @GetMapping("/cases")   // 3. Bar Charts – Case Categories
    public ResponseEntity<CaseAnalyticsResponse> getCaseAnalytics() {
        return ResponseEntity.ok(caseAnalyticsService.getCaseAnalytics());
    }

    /* ================= OVERVIEW ================= */

    @GetMapping("/overview")  // 1.KPI Cards
    public ResponseEntity<AnalyticsOverviewResponse> getOverview() {
        return ResponseEntity.ok(analyticsOverviewService.getOverview());
    }
    
    @GetMapping("/impact")   // 5. Map Visualization – Location Analytics 
    public ResponseEntity<ImpactAnalyticsResponse> getImpact() {
        return ResponseEntity.ok(impactAnalyticsService.getImpactAnalytics());
    }

    @GetMapping("/trends")   //2. Line Charts – Growth Trends
    public ResponseEntity<TrendAnalyticsResponse> getTrends(
            @RequestParam(defaultValue = "monthly") String range) {
        return ResponseEntity.ok(trendAnalyticsService.getTrends(range));
    }

    /* ================= MATCHES ================= */

    @GetMapping("/matches")
    public ResponseEntity<MatchAnalyticsResponse> getMatchAnalytics() {
        return ResponseEntity.ok(matchAnalyticsService.getMatchAnalytics());
    }
    
    /* ================= APPOINTMENTS ================= */

    @GetMapping("/appointments")
    public ResponseEntity<AppointmentAnalyticsResponse> getAppointmentAnalytics() {
        return ResponseEntity.ok(appointmentAnalyticsService.getAppointmentAnalytics());
    }
}
