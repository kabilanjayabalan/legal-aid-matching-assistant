package com.legalaid.backend.dto;

import java.time.LocalDateTime;

import com.legalaid.backend.model.MatchStatus;
import com.legalaid.backend.model.ProviderType;

public interface CitizenMatchDTO {

    Integer getMatchId();
    MatchStatus getStatus();

    Integer getCaseId();
    String getCaseTitle();

    ProviderType getProviderType();
    Integer getProviderId();

    Integer getScore();
    Boolean getSaved();
    LocalDateTime getCreatedAt();
}

