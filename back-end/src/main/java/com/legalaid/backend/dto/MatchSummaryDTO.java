package com.legalaid.backend.dto;

import java.time.LocalDateTime;

import com.legalaid.backend.model.CaseStatus;
import com.legalaid.backend.model.MatchStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchSummaryDTO {
    private Integer id;
    private MatchStatus matchStatus;
    private CaseStatus caseStatus;
    private Integer caseId;
    private String caseTitle;
    private Integer score;
    private LocalDateTime createdAt;
}
