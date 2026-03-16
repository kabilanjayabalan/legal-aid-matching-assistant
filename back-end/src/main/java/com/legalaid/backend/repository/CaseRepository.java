package com.legalaid.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.legalaid.backend.model.Case;
import com.legalaid.backend.model.CasePriority;
import com.legalaid.backend.model.CaseStatus;
import com.legalaid.backend.model.User;

@Repository
public interface CaseRepository extends JpaRepository<Case, Integer> {
    List<Case> findByCreatedBy(User user);
    List<Case> findByAssignedTo(User user);
    List<Case> findByStatus(CaseStatus status);
    List<Case> findByPriority(CasePriority priority);

    long countByStatus(CaseStatus status);

    @Query("""
        SELECT c.category, COUNT(c)
        FROM Case c
        GROUP BY c.category
    """)
    List<Object[]> countCasesByCategory();

    @Query("""
        SELECT LOWER(c.category), c.status, COUNT(c)
        FROM Case c
        GROUP BY LOWER(c.category), c.status
    """)
    List<Object[]> countCasesByCategoryAndStatus();

    @Query("""
        SELECT c.status, COUNT(c)
        FROM Case c
        GROUP BY c.status
    """)
    List<Object[]> countCasesByStatus();

    long count();

    @Query("""
        SELECT c FROM Case c
        WHERE
            (:status IS NULL OR c.status = :status)
        AND (
            COALESCE(:search, '') = '' OR
            c.title ILIKE CONCAT('%', CAST(:search AS string), '%') OR
            c.category ILIKE CONCAT('%', CAST(:search AS string), '%')
        )
    """)
    Page<Case> findCasesForMonitoring(
        @Param("search") String search,
        @Param("status") CaseStatus status,
        Pageable pageable
    );

    @Query("""
        SELECT c.latitude,c.longitude, COUNT(c) FROM Case c WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL
        GROUP BY c.latitude, c.longitude
        """)
    List<Object[]> countCasesByGeo();

    @Query("""
SELECT date_trunc('day', c.createdAt), COUNT(c)
FROM Case c
GROUP BY date_trunc('day', c.createdAt)
ORDER BY date_trunc('day', c.createdAt)
""")
List<Object[]> countCasesDaily();

@Query("""
SELECT date_trunc('month', c.createdAt), COUNT(c)
FROM Case c
GROUP BY date_trunc('month', c.createdAt)
ORDER BY date_trunc('month', c.createdAt)
""")
List<Object[]> countCasesMonthly();

@Query("""
SELECT date_trunc('year', c.createdAt), COUNT(c)
FROM Case c
GROUP BY date_trunc('year', c.createdAt)
ORDER BY date_trunc('year', c.createdAt)
""")
List<Object[]> countCasesYearly();

}
