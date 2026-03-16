package com.legalaid.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.legalaid.backend.dto.CaseSummaryDTO;
import com.legalaid.backend.dto.CitizenMatchDTO;
import com.legalaid.backend.dto.MatchSummaryDTO;
import com.legalaid.backend.model.CaseStatus;
import com.legalaid.backend.model.Match;
import com.legalaid.backend.model.MatchStatus;
import com.legalaid.backend.model.ProviderType;

public interface MatchRepository extends JpaRepository<Match, Integer> {

    /* Find active match for a case (CITIZEN_ACCEPTED or PROVIDER_CONFIRMED) */
    @Query("""
        SELECT m FROM Match m
        WHERE m.caseObj.id = :caseId
        AND m.status IN (
            com.legalaid.backend.model.MatchStatus.CITIZEN_ACCEPTED,
            com.legalaid.backend.model.MatchStatus.PROVIDER_CONFIRMED
        )
        ORDER BY m.createdAt DESC
    """)
    Optional<Match> findActiveMatchByCaseId(@Param("caseId") Integer caseId);

    /* Citizen side */
    List<Match> findByCaseObjCreatedById(Integer citizenId);

    /* Provider side */
    
    List<Match> findByProviderTypeAndProviderIdAndStatus(
        ProviderType providerType,
        Integer providerId,
        MatchStatus status
    );

    Optional<Match> findByIdAndStatus(Integer id, MatchStatus status);
    Optional<Match> findByCaseObjIdAndProviderTypeAndProviderId(
        Integer caseId,
        ProviderType providerType,
        Integer providerId
    );
    /* DTO Queries */
    @Query("""
        SELECT new com.legalaid.backend.dto.MatchSummaryDTO(
            m.id,
            m.status,
            m.caseObj.status,
            c.id,
            c.title,
            m.score,
            m.createdAt
        )
        FROM Match m
        JOIN m.caseObj c
        WHERE m.providerType = :providerType
        AND m.providerId = :providerId
        AND m.status = :matchStatus
        AND m.caseObj.status = :caseStatus
    """)
    Page<MatchSummaryDTO> findProviderRequests(
        ProviderType providerType,
        Integer providerId,
        MatchStatus matchStatus,
        CaseStatus caseStatus,
        Pageable pageable
    );
    @Query("""
        SELECT new com.legalaid.backend.dto.CaseSummaryDTO(
            c.id,
            c.caseNumber,
            c.caseType,
            c.title,
            c.category,
            c.status,
            c.createdAt
        )
        FROM Match m
        JOIN m.caseObj c
        WHERE m.providerType = :providerType
        AND m.providerId = :providerId
        AND m.status = :status
    """)
    Page<CaseSummaryDTO> findAssignedCasesDTO(
        ProviderType providerType,
        Integer providerId,
        MatchStatus status,
        Pageable pageable
    );
    @Query("""
        SELECT
            m.id as matchId,
            m.status as status,
            c.id as caseId,
            c.title as caseTitle,
            m.providerType as providerType,
            m.providerId as providerId,
            m.saved as saved,
            m.createdAt as createdAt
        FROM Match m
        JOIN m.caseObj c
        WHERE c.createdBy.id = :citizenId
        AND m.saved = true
    """)
    Page<CitizenMatchDTO> findSavedCitizenMatches(
        Integer citizenId,
        Pageable pageable
    );
    @Query("""
        SELECT
            m.id as matchId,
            m.status as status,
            c.id as caseId,
            c.title as caseTitle,
            m.providerType as providerType,
            m.providerId as providerId,
            m.score as score,
            m.saved as saved,
            m.createdAt as createdAt
        FROM Match m
        JOIN m.caseObj c
        WHERE c.createdBy.id = :citizenId
        AND m.status IN (
            com.legalaid.backend.model.MatchStatus.CITIZEN_ACCEPTED,
            com.legalaid.backend.model.MatchStatus.PROVIDER_CONFIRMED
        )
    """)
    Page<CitizenMatchDTO> findCitizenMatches(
        Integer citizenId,
        Pageable pageable
    );

    long countByStatus(MatchStatus status);

    @Query(value = """
        SELECT
            m.id as matchId,
            c.id as caseId,
            c.title as caseTitle,
            c.category as caseCategory,
            u.id as citizenId,
            u.full_name as citizenName,
            m.provider_type as providerType,
            m.provider_id as providerId,
            CASE 
                WHEN m.provider_type = 'LAWYER' THEN l.name
                WHEN m.provider_type = 'NGO' THEN n.organization
            END as providerName,
            m.score as score,
            m.status as status,
            m.saved as saved,
            m.created_at as createdAt
        FROM matches m
        JOIN cases c ON c.id = m.case_id
        JOIN users u ON u.id = c.created_by
        LEFT JOIN lawyer_profiles l ON (m.provider_type = 'LAWYER' AND l.user_id = m.provider_id)
        LEFT JOIN ngo_profiles n ON (m.provider_type = 'NGO' AND n.user_id = m.provider_id)
        WHERE (:search IS NULL OR LOWER(CAST(c.title AS TEXT)) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(CAST(u.full_name AS TEXT)) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR m.status = CAST(:status AS TEXT))
        ORDER BY m.created_at DESC
    """, nativeQuery = true)
    Page<com.legalaid.backend.dto.MatchMonitoringDTO> findMatchesForMonitoring(
        String search,
        String status,
        Pageable pageable
    );

    @Query("""
SELECT date_trunc('day', m.createdAt), COUNT(m)
FROM Match m
GROUP BY date_trunc('day', m.createdAt)
ORDER BY date_trunc('day', m.createdAt)
""")
List<Object[]> countMatchesDaily();

@Query("""
SELECT date_trunc('month', m.createdAt), COUNT(m)
FROM Match m
GROUP BY date_trunc('month', m.createdAt)
ORDER BY date_trunc('month', m.createdAt)
""")
List<Object[]> countMatchesMonthly();

@Query("""
SELECT date_trunc('year', m.createdAt), COUNT(m)
FROM Match m
GROUP BY date_trunc('year', m.createdAt)
ORDER BY date_trunc('year', m.createdAt)
""")
List<Object[]> countMatchesYearly();

    List<Match> findByStatusInAndCreatedAtBefore(List<MatchStatus> statuses, LocalDateTime dateTime);

    long countByProviderTypeAndProviderIdAndStatus(ProviderType providerType, Integer providerId, MatchStatus status);

    @Query("""
        SELECT m FROM Match m
        WHERE m.providerType = :providerType
        AND m.providerId = :providerId
        AND m.status = :status
        ORDER BY m.createdAt DESC
    """)
    List<Match> findTop5ByProviderTypeAndProviderIdAndStatusOrderByCreatedAtDesc(
        @Param("providerType") ProviderType providerType,
        @Param("providerId") Integer providerId,
        @Param("status") MatchStatus status,
        Pageable pageable
    );
}
