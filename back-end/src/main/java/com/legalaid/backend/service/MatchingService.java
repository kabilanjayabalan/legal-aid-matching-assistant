package com.legalaid.backend.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.legalaid.backend.dto.CaseSummaryDTO;
import com.legalaid.backend.dto.MatchCardDTO;
import com.legalaid.backend.dto.MatchResultDTO;
import com.legalaid.backend.dto.MatchSummaryDTO;
import com.legalaid.backend.dto.ProviderDashboardStatsDTO;
import com.legalaid.backend.model.Case;
import com.legalaid.backend.model.DirectoryLawyer;
import com.legalaid.backend.model.DirectoryNgo;
import com.legalaid.backend.model.LawyerProfile;
import com.legalaid.backend.model.Match;
import com.legalaid.backend.model.MatchStatus;
import com.legalaid.backend.model.NGOProfile;
import com.legalaid.backend.model.ProviderType;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.repository.DirectoryLawyerRepository;
import com.legalaid.backend.repository.DirectoryNgoRepository;
import com.legalaid.backend.repository.LawyerProfileRepository;
import com.legalaid.backend.repository.MatchRepository;
import com.legalaid.backend.repository.NGOProfileRepository;
import com.legalaid.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private static final int MAX_MATCH_RESULTS = 25;

    private final LawyerProfileRepository lawyerProfileRepository;
    private final NGOProfileRepository ngoProfileRepository;
    private final DirectoryLawyerRepository directoryLawyerRepository;
    private final DirectoryNgoRepository directoryNgoRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    // 🔔 Notification service (REQUIRED)
    private final NotificationService notificationService;

    public MatchResultDTO generateMatches(Case caseObj, Integer sensitivity) {

        if (caseObj.getCategory() == null || caseObj.getCategory().isBlank()) {
            throw new RuntimeException("Case category must be set before generating matches");
        }

        List<MatchCardDTO> results = new ArrayList<>();

        /* ================= REGISTERED LAWYERS ================= */
        for (LawyerProfile lp : lawyerProfileRepository.findByIsAvailableTrueAndVerifiedTrue()) {

            int score = scoreRegisteredLawyer(caseObj, lp);
            if (score <= 0) continue;

            Match match = matchRepository
                    .findByCaseObjIdAndProviderTypeAndProviderId(
                            caseObj.getId(),
                            ProviderType.LAWYER,
                            lp.getId()
                    )
                    .orElse(null);

                if (match == null) {
                    match = new Match();
                    match.setCaseObj(caseObj);
                    match.setProviderType(ProviderType.LAWYER);
                    match.setProviderId(lp.getId());
                    match.setStatus(MatchStatus.PENDING); // ✅ ONLY here
                }
            if (isTerminal(match.getStatus())) continue;

            // ✅ Detect new match BEFORE save
            boolean isNewMatch = (match.getId() == null);
            if (isNewMatch) {
            match.setScore(score);
            matchRepository.save(match);
            }

            // 🔔 Notify only once
            if (isNewMatch) {
                notificationService.notifyUser(
                        lp.getUser().getId().longValue(),
                        "MATCH_REQUEST",
                        "New match request received",
                        String.valueOf(match.getId())
                );
            }

            results.add(new MatchCardDTO(
                    "REGISTERED",
                    "LAWYER",
                    match.getId(),
                    lp.getName(),
                    lp.getCity(),
                    lp.getSpecialization(),
                    score,
                    lp.getVerified(),
                    true,
                    match.getStatus(),
                    match.getCaseObj().getStatus()
            ));
        }

        /* ================= REGISTERED NGOs ================= */
        for (NGOProfile np : ngoProfileRepository.findByIsAvailableTrueAndVerifiedTrue()) {

            int score = scoreRegisteredNgo(caseObj, np);
            if (score <= 0) continue;

            Match match = matchRepository
                    .findByCaseObjIdAndProviderTypeAndProviderId(
                            caseObj.getId(),
                            ProviderType.NGO,
                            np.getId()
                    )
                    .orElse(null);

                if (match == null) {
                    match = new Match();
                    match.setCaseObj(caseObj);
                    match.setProviderType(ProviderType.NGO);
                    match.setProviderId(np.getId());
                    match.setStatus(MatchStatus.PENDING); // ✅ ONLY here
                }
            if (isTerminal(match.getStatus())) continue;

            // ✅ Detect new match BEFORE save
            boolean isNewMatch = (match.getId() == null);
            if (isNewMatch) {
                match.setScore(score);
                matchRepository.save(match);
            }

            // 🔔 Notify only once
            if (isNewMatch) {
                notificationService.notifyUser(
                        Long.valueOf(np.getUser().getId()),
                        "MATCH_REQUEST",
                        "New match request received",
                        String.valueOf(match.getId())
                );
            }

            results.add(new MatchCardDTO(
                    "REGISTERED",
                    "NGO",
                    match.getId(),
                    np.getNgoName(),
                    np.getCity(),
                    np.getDescription(),
                    score,
                    np.getVerified(),
                    true,
                    match.getStatus(),
                    match.getCaseObj().getStatus()
            ));
        }

        /* ================= DIRECTORY LAWYERS ================= */
        for (DirectoryLawyer dl : directoryLawyerRepository.findAll()) {

            int score = scoreDirectoryLawyer(caseObj, dl);
            if (score <= 0) continue;

            results.add(new MatchCardDTO(
                    "DIRECTORY",
                    "LAWYER",
                    null,
                    dl.getFullName(),
                    dl.getCity(),
                    dl.getSpecialization(),
                    score,
                    dl.getVerified(),
                    false,
                    null,
                    null
            ));
        }

        /* ================= DIRECTORY NGOs ================= */
        for (DirectoryNgo dn : directoryNgoRepository.findAll()) {

            int score = scoreDirectoryNgo(caseObj, dn);
            if (score <= 0) continue;

            results.add(new MatchCardDTO(
                    "DIRECTORY",
                    "NGO",
                    null,
                    dn.getOrgName(),
                    dn.getCity(),
                    dn.getFocusArea(),
                    score,
                    dn.getVerified(),
                    false,
                    null,
                    null
            ));
        }

        int effectiveSensitivity =
        sensitivity == null ? 40 : Math.max(0, Math.min(sensitivity, 100));


        List<MatchCardDTO> topResults =
                results.stream()
                .filter(r -> r.score() >= effectiveSensitivity)
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .limit(MAX_MATCH_RESULTS)
                .toList();

        return new MatchResultDTO(topResults);
    }

    public ProviderDashboardStatsDTO getProviderDashboardStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Integer providerId;
        ProviderType providerType;

        if (user.getRole() == Role.LAWYER) {
            providerId = lawyerProfileRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Lawyer profile not found. Please complete your profile setup by updating your profile information."))
                    .getId();
            providerType = ProviderType.LAWYER;
        } else if (user.getRole() == Role.NGO) {
            providerId = ngoProfileRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("NGO profile not found. Please complete your profile setup by updating your profile information."))
                    .getId();
            providerType = ProviderType.NGO;
        } else {
            throw new RuntimeException("User is not a provider");
        }

        // 1. Counts
        long matchRequestsCount = matchRepository.countByProviderTypeAndProviderIdAndStatus(
                providerType, providerId, MatchStatus.CITIZEN_ACCEPTED);
        
        long assignedCasesCount = matchRepository.countByProviderTypeAndProviderIdAndStatus(
                providerType, providerId, MatchStatus.PROVIDER_CONFIRMED);

        // 2. Recent Match Requests (Top 5)
        List<Match> recentRequests = matchRepository.findTop5ByProviderTypeAndProviderIdAndStatusOrderByCreatedAtDesc(
                providerType, providerId, MatchStatus.CITIZEN_ACCEPTED, PageRequest.of(0, 5));
        
        List<MatchSummaryDTO> recentRequestDTOs = recentRequests.stream()
                .map(m -> new MatchSummaryDTO(
                        m.getId(),
                        m.getStatus(),
                        m.getCaseObj().getStatus(),
                        m.getCaseObj().getId(),
                        m.getCaseObj().getTitle(),
                        m.getScore(),
                        m.getCreatedAt()
                ))
                .collect(Collectors.toList());

        // 3. Recent Assigned Cases (Top 5)
        List<Match> recentAssigned = matchRepository.findTop5ByProviderTypeAndProviderIdAndStatusOrderByCreatedAtDesc(
                providerType, providerId, MatchStatus.PROVIDER_CONFIRMED, PageRequest.of(0, 5));

        List<CaseSummaryDTO> recentAssignedDTOs = recentAssigned.stream()
                .map(m -> new CaseSummaryDTO(
                        m.getCaseObj().getId(),
                        m.getCaseObj().getCaseNumber(),
                        m.getCaseObj().getCaseType(),
                        m.getCaseObj().getTitle(),
                        m.getCaseObj().getCategory(),
                        m.getCaseObj().getStatus(),
                        m.getCaseObj().getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new ProviderDashboardStatsDTO(
                matchRequestsCount,
                assignedCasesCount,
                recentRequestDTOs,
                recentAssignedDTOs
        );
    }

    /* ================= SCORING ================= */

    private int scoreRegisteredLawyer(Case c, LawyerProfile p) {
        int score = 0;
        
        // 1. Specialization/Expertise Match (Max 40)
        // Combine specialization and expertise for broader matching
        String providerText = (p.getSpecialization() != null ? p.getSpecialization() : "") + " " + 
                              (p.getExpertise() != null ? p.getExpertise() : "");
        score += calculateTextMatchScore(c.getCategory(), providerText, 40);
        
        // 2. Bio/Description Match (Max 10)
        score += calculateTextMatchScore(c.getDescription(), p.getBio(), 10);

        // 3. Location Match (Max 30)
        score += calculateLocationScore(c, p.getLatitude(), p.getLongitude(), p.getCity(), 30);

        // 4. Language Match (Max 10)
        score += calculateLanguageScore(c.getPreferredLanguage(), p.getLanguage(), 10);

        // 5. Verification Bonus (Max 10)
        if (Boolean.TRUE.equals(p.getVerified())) score += 10;

        return capScore(score);
    }

    private int scoreRegisteredNgo(Case c, NGOProfile p) {
        int score = 0;
        
        // 1. Focus Area Match (Max 40)
        // Using description as focus area for NGOs
        score += calculateTextMatchScore(c.getCategory(), p.getDescription(), 40);
        
        // 2. Description Match (Max 10)
        score += calculateTextMatchScore(c.getDescription(), p.getDescription(), 10);

        // 3. Location Match (Max 30)
        score += calculateLocationScore(c, p.getLatitude(), p.getLongitude(), p.getCity(), 30);

        // 4. Language Match (Max 10)
        score += calculateLanguageScore(c.getPreferredLanguage(), p.getLanguage(), 10);

        // 5. Verification Bonus (Max 10)
        if (Boolean.TRUE.equals(p.getVerified())) score += 10;

        return capScore(score);
    }

    private int scoreDirectoryLawyer(Case c, DirectoryLawyer d) {
        int score = 0;

        // 1. Specialization Match (Max 40)
        score += calculateTextMatchScore(c.getCategory(), d.getSpecialization(), 35);
        
        // 2. Location Match (Max 30)
        score += calculateLocationScore(c, d.getLatitude(), d.getLongitude(), d.getCity(), 35);

        // 3. Language Match (Max 10)
        score += calculateLanguageScore(c.getPreferredLanguage(), d.getLanguage(), 12);

        // 4. Verification Bonus (Max 10)
        if (Boolean.TRUE.equals(d.getVerified())) score += 9;
        
        // Base score for directory entries to ensure they appear if relevant
        if (score > 0) score += 8;

        return capScore(score);
    }

    private int scoreDirectoryNgo(Case c, DirectoryNgo d) {
        int score = 0;

        // 1. Focus Area Match (Max 40)
        score += calculateTextMatchScore(c.getCategory(), d.getFocusArea(), 37);

        // 2. Location Match (Max 30)
        score += calculateLocationScore(c, d.getLatitude(), d.getLongitude(), d.getCity(), 34);

        // 3. Language Match (Max 10)
        score += calculateLanguageScore(c.getPreferredLanguage(), d.getLanguage(), 12);

        // 4. Verification Bonus (Max 10)
        if (Boolean.TRUE.equals(d.getVerified())) score += 9;
        
        // Base score for directory entries
        if (score > 0) score += 8;

        return capScore(score);
    }

    // --- Helper Methods ---
    private int calculateTextMatchScore(String caseText, String providerText, int maxScore) {
        if (caseText == null || providerText == null) return 0; // Normalize texts
        String cText = caseText.toLowerCase(); String pText = providerText.toLowerCase(); // Split into keywords (comma, space, slash separated)
        List<String> caseKeywords = Arrays.stream(
            cText.split("[,\\s/]+"))
            .filter(w -> w.length() > 2)  // Ignore short words
            .collect(Collectors.toList());
        if (caseKeywords.isEmpty()) return 0;
        int matches = 0;
        for (String keyword : caseKeywords) {
            if (pText.contains(keyword)) { matches++; }
        }
        if (matches == 0) return 0; // Calculate score: more matches = higher score, up to maxScore // If at least one keyword matches, give at least 50% of maxScore
        double matchRatio = (double) matches / caseKeywords.size();
        int score = (int) (maxScore * (0.5 + (0.5 * matchRatio)));
        return Math.min(score, maxScore);
    }


    private int calculateLocationScore(Case c, Double pLat, Double pLon, String pCity, int maxScore) {
        // 1. Try Geolocation Match first (Most accurate)
        if (c.getLatitude() != null && c.getLongitude() != null && pLat != null && pLon != null) {
            double distance = distanceKm(c.getLatitude(), c.getLongitude(), pLat, pLon);
            
            if (distance <= 50) return maxScore; // Very close (< 10km)
            if (distance <= 100) return (int) (maxScore * 0.8); // Close (< 30km)
            if (distance <= 500) return (int) (maxScore * 0.5); // Nearby (< 60km)
            if (distance <= 1000) return (int) (maxScore * 0.2); // Within region (< 100km)
            return 0; // Too far
        }
        
        // 2. Fallback to City String Match
        if (c.getLocation() != null && pCity != null) {
            String cCity = c.getLocation().trim().toLowerCase();
            String pCityStr = pCity.trim().toLowerCase();
            
            if (cCity.equals(pCityStr)) return maxScore; // Exact match
            if (cCity.contains(pCityStr) || pCityStr.contains(cCity)) return (int) (maxScore * 0.7); // Partial match
        }
        
        return 0;
    }

    private int calculateLanguageScore(String caseLang, String providerLang, int maxScore) {
        if (caseLang == null || providerLang == null) return 0;
        
        String cLang = caseLang.toLowerCase();
        String pLang = providerLang.toLowerCase();
        
        // Split by comma to handle multiple languages
        String[] cLangs = cLang.split("[,\\s]+");
        
        for (String lang : cLangs) {
            if (lang.length() > 2 && pLang.contains(lang)) {
                return maxScore; // Match found
            }
        }
        
        return 0;
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private int capScore(int score) {
        return Math.min(score, 100);
    }

    private boolean isTerminal(MatchStatus status) {
        return status == MatchStatus.REJECTED;
    }
}
