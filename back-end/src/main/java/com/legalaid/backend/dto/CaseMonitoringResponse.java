package com.legalaid.backend.dto;

import com.legalaid.backend.model.Case;
import com.legalaid.backend.model.CaseStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaseMonitoringResponse {

    private String id;
    private String title;
    private String category;

    private String citizen;
    private String assignedTo;

    private String status;
    private String matchStatus;

    private int progress;
    private String priority;

    private String lastUpdate;

    public static CaseMonitoringResponse fromEntity(Case c) {
        return fromEntity(c, null);
    }

    public static CaseMonitoringResponse fromEntity(Case c, String matchedProviderName) {
        boolean assigned = c.getAssignedTo() != null;
        boolean hasMatch = matchedProviderName != null && !matchedProviderName.isEmpty();

        String assignedToValue;
        String matchStatusValue;

        if (assigned) {
            assignedToValue = c.getAssignedTo().getEmail();
            matchStatusValue = "Matched";
        } else if (hasMatch) {
            assignedToValue = matchedProviderName;
            matchStatusValue = "Match Pending";
        } else {
            assignedToValue = "Pending";
            matchStatusValue = "Searching";
        }

        return CaseMonitoringResponse.builder()
                .id("CASE-" + c.getId())
                .title(c.getTitle())
                .category(c.getCategory())

                .citizen(c.getCreatedBy().getEmail())
                .assignedTo(assignedToValue)

                .status(formatEnum(c.getStatus().name()))
                .matchStatus(matchStatusValue)

                .progress(calculateProgress(c.getStatus(), matchStatusValue))
                .priority(formatEnum(c.getPriority().name()))

                .lastUpdate(c.getUpdatedAt().toString())
                .build();
    }

    private static int calculateProgress(CaseStatus status, String matchStatus) {
        // Base progress from case status
        int baseProgress = switch (status) {
            case OPEN -> 20;
            case IN_PROGRESS -> 50;
            case CLOSED -> 100;
        };

        // If case is closed, return 100%
        if (status == CaseStatus.CLOSED) {
            return 100;
        }

        // Add bonus based on match status
        int matchBonus = switch (matchStatus) {
            case "Matched" -> 30;
            case "Match Pending" -> 15;
            default -> 0; // Searching
        };

        return Math.min(baseProgress + matchBonus, 99); // Cap at 99% until closed
    }

    private static String formatEnum(String value) {
        return value.replace("_", " ");
    }
}

