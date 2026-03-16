package com.legalaid.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.legalaid.backend.model.CaseNumberSequence;

import jakarta.persistence.LockModeType;

@Repository
public interface CaseNumberSequenceRepository extends JpaRepository<CaseNumberSequence, Integer> {

    /**
     * Find sequence by case type and year with pessimistic write lock
     * to prevent race conditions when generating case numbers
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cns FROM CaseNumberSequence cns WHERE cns.caseType = :caseType AND cns.year = :year")
    Optional<CaseNumberSequence> findByCaseTypeAndYearForUpdate(
        @Param("caseType") String caseType,
        @Param("year") Integer year
    );
}

