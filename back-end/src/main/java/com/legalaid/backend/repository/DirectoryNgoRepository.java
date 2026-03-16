package com.legalaid.backend.repository;

import com.legalaid.backend.model.DirectoryNgo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DirectoryNgoRepository extends JpaRepository<DirectoryNgo, Integer> {

    boolean existsByRegistrationNumber(String registrationNumber);

    DirectoryNgo findByRegistrationNumber(String registrationNumber);

    List<DirectoryNgo> findByCityContainingIgnoreCase(String city);

    List<DirectoryNgo> findByFocusAreaContainingIgnoreCase(String focusArea);

    @Query("SELECT n FROM DirectoryNgo n WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(n.orgName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(n.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(n.focusArea) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:location IS NULL OR :location = '' OR LOWER(n.city) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "(:expertise IS NULL OR :expertise = '' OR LOWER(n.focusArea) LIKE LOWER(CONCAT('%', :expertise, '%'))) AND " +
            "(:language IS NULL OR :language = '' OR (n.language IS NOT NULL AND LOWER(n.language) LIKE LOWER(CONCAT('%', :language, '%')))) AND " +
            "(:isVerified IS NULL OR n.verified = :isVerified)")
    Page<DirectoryNgo> searchNgos(
            @Param("query") String query,
            @Param("location") String location,
            @Param("expertise") String expertise,
            @Param("language") String language,
            @Param("isVerified") Boolean isVerified,
            Pageable pageable
    );

    @Query("SELECT DISTINCT n.focusArea FROM DirectoryNgo n WHERE n.focusArea IS NOT NULL AND n.focusArea != '' ORDER BY n.focusArea")
    List<String> findDistinctFocusAreas();

    // ========== GEOCODING SUPPORT METHODS ==========

    /**
     * Find all NGOs with missing latitude or longitude coordinates.
     * Used by GeocodingService for batch geocoding.
     */
    @Query("SELECT n FROM DirectoryNgo n WHERE n.latitude IS NULL OR n.longitude IS NULL")
    List<DirectoryNgo> findByLatitudeIsNullOrLongitudeIsNull();

    /**
     * Count NGOs with missing coordinates.
     */
    @Query("SELECT COUNT(n) FROM DirectoryNgo n WHERE n.latitude IS NULL OR n.longitude IS NULL")
    long countByLatitudeIsNullOrLongitudeIsNull();

    // ========== HAVERSINE DISTANCE-BASED FILTERING ==========

    /**
     * Find all NGOs within a specified distance (in kilometers) from the given coordinates.
     * Uses the Haversine formula to calculate the great-circle distance.
     * Results are sorted by distance (nearest first).
     *
     * @param userLat   User's latitude
     * @param userLon   User's longitude
     * @param radiusKm  Maximum distance in kilometers
     * @return List of NGOs within the specified radius, sorted by distance
     */
    @Query(value = """
            SELECT n.*, 
                   (6371 * ACOS(
                       LEAST(1.0, GREATEST(-1.0,
                           COS(RADIANS(:userLat)) * COS(RADIANS(n.latitude)) * COS(RADIANS(n.longitude) - RADIANS(:userLon)) +
                           SIN(RADIANS(:userLat)) * SIN(RADIANS(n.latitude))
                       ))
                   )) AS distance
            FROM directory_ngos n
            WHERE n.latitude IS NOT NULL 
              AND n.longitude IS NOT NULL
              AND (6371 * ACOS(
                       LEAST(1.0, GREATEST(-1.0,
                           COS(RADIANS(:userLat)) * COS(RADIANS(n.latitude)) * COS(RADIANS(n.longitude) - RADIANS(:userLon)) +
                           SIN(RADIANS(:userLat)) * SIN(RADIANS(n.latitude))
                       ))
                   )) <= :radiusKm
            ORDER BY distance ASC
            """, nativeQuery = true)
    List<DirectoryNgo> findAllWithinDistance(
            @Param("userLat") double userLat,
            @Param("userLon") double userLon,
            @Param("radiusKm") double radiusKm
    );

    /**
     * Paginated version of findAllWithinDistance.
     * Find all NGOs within a specified distance from the given coordinates with pagination.
     *
     * @param userLat   User's latitude
     * @param userLon   User's longitude
     * @param radiusKm  Maximum distance in kilometers
     * @param pageable  Pagination information
     * @return Page of NGOs within the specified radius
     */
    @Query(value = """
            SELECT n.*, 
                   (6371 * ACOS(
                       LEAST(1.0, GREATEST(-1.0,
                           COS(RADIANS(:userLat)) * COS(RADIANS(n.latitude)) * COS(RADIANS(n.longitude) - RADIANS(:userLon)) +
                           SIN(RADIANS(:userLat)) * SIN(RADIANS(n.latitude))
                       ))
                   )) AS distance
            FROM directory_ngos n
            WHERE n.latitude IS NOT NULL 
              AND n.longitude IS NOT NULL
              AND (6371 * ACOS(
                       LEAST(1.0, GREATEST(-1.0,
                           COS(RADIANS(:userLat)) * COS(RADIANS(n.latitude)) * COS(RADIANS(n.longitude) - RADIANS(:userLon)) +
                           SIN(RADIANS(:userLat)) * SIN(RADIANS(n.latitude))
                       ))
                   )) <= :radiusKm
            ORDER BY distance ASC
            """,
            countQuery = """
            SELECT COUNT(n.id)
            FROM directory_ngos n
            WHERE n.latitude IS NOT NULL 
              AND n.longitude IS NOT NULL
              AND (6371 * ACOS(
                       LEAST(1.0, GREATEST(-1.0,
                           COS(RADIANS(:userLat)) * COS(RADIANS(n.latitude)) * COS(RADIANS(n.longitude) - RADIANS(:userLon)) +
                           SIN(RADIANS(:userLat)) * SIN(RADIANS(n.latitude))
                       ))
                   )) <= :radiusKm
            """,
            nativeQuery = true)
    Page<DirectoryNgo> findAllWithinDistancePageable(
            @Param("userLat") double userLat,
            @Param("userLon") double userLon,
            @Param("radiusKm") double radiusKm,
            Pageable pageable
    );

    /**
     * Find all NGOs within distance with additional search filters.
     * Combines Haversine distance filtering with text-based search criteria.
     *
     * @param userLat     User's latitude
     * @param userLon     User's longitude
     * @param radiusKm    Maximum distance in kilometers
     * @param query       Optional search query for name, city, or focus area
     * @param expertise   Optional focus area filter
     * @param isVerified  Optional verified status filter
     * @return List of NGOs matching all criteria, sorted by distance
     */
    @Query(value = """
            SELECT n.*, 
                   (6371 * ACOS(
                       LEAST(1.0, GREATEST(-1.0,
                           COS(RADIANS(:userLat)) * COS(RADIANS(n.latitude)) * COS(RADIANS(n.longitude) - RADIANS(:userLon)) +
                           SIN(RADIANS(:userLat)) * SIN(RADIANS(n.latitude))
                       ))
                   )) AS distance
            FROM directory_ngos n
            WHERE n.latitude IS NOT NULL 
              AND n.longitude IS NOT NULL
              AND (6371 * ACOS(
                       LEAST(1.0, GREATEST(-1.0,
                           COS(RADIANS(:userLat)) * COS(RADIANS(n.latitude)) * COS(RADIANS(n.longitude) - RADIANS(:userLon)) +
                           SIN(RADIANS(:userLat)) * SIN(RADIANS(n.latitude))
                       ))
                   )) <= :radiusKm
              AND (:query IS NULL OR :query = '' OR 
                   LOWER(n.org_name) LIKE LOWER(CONCAT('%', :query, '%')) OR
                   LOWER(n.city) LIKE LOWER(CONCAT('%', :query, '%')) OR
                   LOWER(n.focus_area) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:expertise IS NULL OR :expertise = '' OR LOWER(n.focus_area) LIKE LOWER(CONCAT('%', :expertise, '%')))
              AND (:isVerified IS NULL OR n.verified = :isVerified)
            ORDER BY distance ASC
            """, nativeQuery = true)
    List<DirectoryNgo> findAllWithinDistanceFiltered(
            @Param("userLat") double userLat,
            @Param("userLon") double userLon,
            @Param("radiusKm") double radiusKm,
            @Param("query") String query,
            @Param("expertise") String expertise,
            @Param("isVerified") Boolean isVerified
    );
}


