package com.legalaid.backend.dto.analytics;

import lombok.Getter;

@Getter
public class UserRoleAnalyticsResponse {

    private final long citizens;
    private final long lawyers;
    private final long ngos;
    private final long admins;
    private final long totalUsers;

    private final double citizenPercentage;
    private final double lawyerPercentage;
    private final double ngoPercentage;
    private final double adminPercentage;

    public UserRoleAnalyticsResponse(
            long citizens,
            long lawyers,
            long ngos,
            long admins
    ) {
        this.citizens = citizens;
        this.lawyers = lawyers;
        this.ngos = ngos;
        this.admins = admins;

        this.totalUsers = citizens + lawyers + ngos + admins;

        this.citizenPercentage = calculate(citizens, totalUsers);
        this.lawyerPercentage  = calculate(lawyers, totalUsers);
        this.ngoPercentage     = calculate(ngos, totalUsers);
        this.adminPercentage   = calculate(admins, totalUsers);
    }

    private double calculate(long count, long total) {
        return total == 0 ? 0.0 : (count * 100.0) / total;
    }
}
