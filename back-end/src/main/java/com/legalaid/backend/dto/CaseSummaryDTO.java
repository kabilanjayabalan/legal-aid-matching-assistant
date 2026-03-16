package com.legalaid.backend.dto;

import java.time.LocalDateTime;

import com.legalaid.backend.model.CaseStatus;
import com.legalaid.backend.model.CaseType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseSummaryDTO {
    private Integer caseId;
    private String caseNumber;
    private CaseType caseType;
    private String title;
    private String category;
    private CaseStatus status;
    private LocalDateTime createdAt;
}
