package com.legalaid.backend.service.analytics;

import java.util.List;

import org.springframework.stereotype.Service;

import com.legalaid.backend.dto.analytics.TimePointDTO;
import com.legalaid.backend.dto.analytics.TrendAnalyticsResponse;
import com.legalaid.backend.repository.CaseRepository;
import com.legalaid.backend.repository.MatchRepository;
import com.legalaid.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrendAnalyticsService {

    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final MatchRepository matchRepository;

    public TrendAnalyticsResponse getTrends(String range) {

    List<Object[]> users;
    List<Object[]> cases;
    List<Object[]> matches;

    switch (range) {
        case "daily" -> {
            users = userRepository.countUsersDaily();
            cases = caseRepository.countCasesDaily();
            matches = matchRepository.countMatchesDaily();
        }
        case "yearly" -> {
            users = userRepository.countUsersYearly();
            cases = caseRepository.countCasesYearly();
            matches = matchRepository.countMatchesYearly();
        }
        default -> { // monthly
            users = userRepository.countUsersMonthly();
            cases = caseRepository.countCasesMonthly();
            matches = matchRepository.countMatchesMonthly();
        }
    }

    return new TrendAnalyticsResponse(
        range,
        map(users),
        map(cases),
        map(matches)
    );
}
private List<TimePointDTO> map(List<Object[]> rows) {
    return rows.stream().map(r -> {
        String time = r[0].toString(); // date_trunc returns timestamp
        Long count = ((Number) r[1]).longValue();
        return new TimePointDTO(time, count);
    }).toList();
}



}
