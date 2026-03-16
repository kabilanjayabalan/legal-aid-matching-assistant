package com.legalaid.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.legalaid.backend.model.NGOProfile;
import com.legalaid.backend.model.User;

@Repository
public interface NGOProfileRepository extends JpaRepository<NGOProfile, Integer> {
    Optional<NGOProfile> findByUser(User user);
    Page<NGOProfile> findByVerifiedIsNull(Pageable pageable);
    Page<NGOProfile> findAll(Pageable pageable);

    List<NGOProfile> findByCityContainingIgnoreCase(String city);

    @Query("SELECT n FROM NGOProfile n WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(COALESCE(n.ngoName, n.organization, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(n.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(n.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(n.user.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:location IS NULL OR :location = '' OR LOWER(n.city) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "(:expertise IS NULL OR :expertise = '' OR LOWER(n.description) LIKE LOWER(CONCAT('%', :expertise, '%'))) AND "
            +
            "(:language IS NULL OR :language = '' OR (n.language IS NOT NULL AND LOWER(n.language) LIKE LOWER(CONCAT('%', :language, '%')))) AND "
            +
            "(:isVerified IS NULL OR n.verified = :isVerified)")
    Page<NGOProfile> searchNgos(
            @Param("query") String query,
            @Param("location") String location,
            @Param("expertise") String expertise,
            @Param("language") String language,
            @Param("isVerified") Boolean isVerified,
            Pageable pageable);

    @Query("SELECT DISTINCT n.description FROM NGOProfile n WHERE n.description IS NOT NULL AND n.description != '' ORDER BY n.description")
    List<String> findDistinctFocusAreas();

    List<NGOProfile> findByIsAvailableTrueAndVerifiedTrue();

    Optional<NGOProfile> findByRegistrationNo(String registrationNo);

    Optional<NGOProfile> findByUserId(Integer userId);

    @Query("""
        SELECT np.latitude, np.longitude, COUNT(np)
        FROM NGOProfile np
        WHERE np.latitude IS NOT NULL AND np.longitude IS NOT NULL
        GROUP BY np.latitude, np.longitude
        """)
        List<Object[]> countNgosByGeo();

}
