package com.legalaid.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.legalaid.backend.model.LawyerProfile;
import com.legalaid.backend.model.User;

@Repository
public interface LawyerProfileRepository extends JpaRepository<LawyerProfile, Integer> {
    Optional<LawyerProfile> findByUser(User user);
    Page<LawyerProfile> findAll(Pageable pageable);
    Page<LawyerProfile> findByVerifiedIsNull(Pageable pageable);

    List<LawyerProfile> findByCityContainingIgnoreCase(String city);

    List<LawyerProfile> findBySpecializationContainingIgnoreCase(String specialization);

    @Query("SELECT l FROM LawyerProfile l WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(l.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(l.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(l.specialization) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(l.user.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:location IS NULL OR :location = '' OR LOWER(l.city) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "(:expertise IS NULL OR :expertise = '' OR LOWER(l.specialization) LIKE LOWER(CONCAT('%', :expertise, '%'))) AND "
            +
            "(:language IS NULL OR :language = '' OR (l.language IS NOT NULL AND LOWER(l.language) LIKE LOWER(CONCAT('%', :language, '%')))) AND "
            +
            "(:isVerified IS NULL OR l.verified = :isVerified)")
    Page<LawyerProfile> searchLawyers(
            @Param("query") String query,
            @Param("location") String location,
            @Param("expertise") String expertise,
            @Param("language") String language,
            @Param("isVerified") Boolean isVerified,
            Pageable pageable);

    @Query("SELECT DISTINCT l.specialization FROM LawyerProfile l WHERE l.specialization IS NOT NULL AND l.specialization != '' ORDER BY l.specialization")
    List<String> findDistinctSpecializations();

    List<LawyerProfile> findByIsAvailableTrueAndVerifiedTrue();

    Optional<LawyerProfile> findByBarRegistrationNo(String barRegistrationNo);

    Optional<LawyerProfile> findByUserId(Integer userId);
    @Query("""
        SELECT lp.latitude, lp.longitude, COUNT(lp)
        FROM LawyerProfile lp
        WHERE lp.latitude IS NOT NULL AND lp.longitude IS NOT NULL
        GROUP BY lp.latitude, lp.longitude
        """)
        List<Object[]> countLawyersByGeo();

}
