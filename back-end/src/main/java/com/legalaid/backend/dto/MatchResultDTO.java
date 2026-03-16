package com.legalaid.backend.dto;

import java.util.List;

public record MatchResultDTO(
        List<MatchCardDTO> results
) {}

