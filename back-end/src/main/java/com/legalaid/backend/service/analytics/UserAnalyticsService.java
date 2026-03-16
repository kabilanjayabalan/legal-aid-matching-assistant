package com.legalaid.backend.service.analytics;

import org.springframework.stereotype.Service;

import com.legalaid.backend.dto.analytics.UserRoleAnalyticsResponse;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAnalyticsService {

    private final UserRepository userRepository;

    public UserRoleAnalyticsResponse getUserRoleDistribution() {

        long citizenCount = userRepository.countByRole(Role.CITIZEN);
        long lawyerCount  = userRepository.countByRole(Role.LAWYER);
        long ngoCount     = userRepository.countByRole(Role.NGO);
        long adminCount   = userRepository.countByRole(Role.ADMIN);

        return new UserRoleAnalyticsResponse(
                citizenCount,
                lawyerCount,
                ngoCount,
                adminCount
        );
    }
}
