package com.legalaid.backend.dto;

import java.time.LocalDateTime;

import com.legalaid.backend.model.MatchStatus;
import com.legalaid.backend.model.ProviderType;

public interface MatchMonitoringDTO {
    Integer getMatchId();
    Integer getCaseId();
    String getCaseTitle();
    String getCaseCategory();
    Integer getCitizenId();
    String getCitizenName();
    ProviderType getProviderType();
    Integer getProviderId();
    String getProviderName();
    Integer getScore();
    MatchStatus getStatus();
    Boolean getSaved();
    LocalDateTime getCreatedAt();
}

