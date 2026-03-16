package com.legalaid.backend.repository;

import com.legalaid.backend.model.DirectoryLawyer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DirectoryLawyerRepository extends JpaRepository<DirectoryLawyer, Integer> {

    boolean existsByBarRegistrationId(String barRegistrationId);

    DirectoryLawyer findByBarRegistrationId(String barRegistrationId);

    List<DirectoryLawyer> findByCityContainingIgnoreCase(String city);

    List<DirectoryLawyer> findBySpecializationContainingIgnoreCase(String specialization);

    @Query("SELECT l FROM DirectoryLawyer l WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(l.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(l.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(l.specialization) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:location IS NULL OR :location = '' OR LOWER(l.city) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "(:expertise IS NULL OR :expertise = '' OR LOWER(l.specialization) LIKE LOWER(CONCAT('%', :expertise, '%'))) AND " +
            "(:language IS NULL OR :language = '' OR (l.language IS NOT NULL AND LOWER(l.language) LIKE LOWER(CONCAT('%', :language, '%')))) AND " +
            "(:isVerified IS NULL OR l.verified = :isVerified)")
    Page<DirectoryLawyer> searchLawyers(
            @Param("query") String query,
            @Param("location") String location,
            @Param("expertise") String expertise,
            @Param("language") String language,
            @Param("isVerified") Boolean isVerified,
            Pageable pageable
    );

    @Query("SELECT DISTINCT l.specialization FROM DirectoryLawyer l WHERE l.specialization IS NOT NULL AND l.specialization != '' ORDER BY l.specialization")
    List<String> findDistinctSpecializations();

    // ========== GEOCODING SUPPORT METHODS ==========

    /**
     * Find all lawyers with missing latitude or longitude coordinates.
     * Used by GeocodingService for batch geocoding.
     */
    @Query("SELECT l FROM DirectoryLawyer l WHERE l.latitude IS NULL OR l.longitude IS NULL")
    List<DirectoryLawyer> findByLatitudeIsNullOrLongitudeIsNull();

    /**
     * Count lawyers with missing coordinates.
     */
    @Query("SELECT COUNT(l) FROM DirectoryLawyer l WHERE l.latitude IS NULL OR l.longitude IS NULL")
    long countByLatitudeIsNullOrLongitudeIsNull();

    // ========== HAVERSINE DISTANCE-BASED FILTERING ==========

    /**
     * Find all lawyers within a specified distance (in kilometers) from the given coordinates.
     * Uses the Haversine formula to calculate the great-circle distance.
     * Results are sorted by distance (nearest first).
     *
     * @param userLat   User's latitude
     * @param userLon   User's longitude
     * @param radiusKm  Maximum distance in kilometers
     * @return List of lawyers within the specified radius, sorted by distance
     */
    @Query(value = """
            SELECT l.*, 
                   (6371 * ACOS(
                       LEAST(1.0, GREATEST(-1.0,
                           COS(RADIANS(:userLat)) * COS(RADIANS(l.latitude)) * COS(RADIANS(l.longitude) - RADIANS(:userLon)) +
                           SIN(RADIANS(:userLat)) * SIN(RADIANS(l.latitude))
                       ))
                   )) AS distance
            FROM directory_lawyers l
            WHERE l.latitude IS NOT NULL 
              AND l.longitude IS NOT NULL
              AND (6371 * ACOS(
                       LEAST(1.0, GREATEST(-1.0,
                           COS(RADIANS(:userLat)) * COS(RADIANS(l.latitude)) * COS(RADIANS(l.longitude) - RADIANS(:userLon)) +
                           SIN(RADIANS(:userLat)) * SIN(RADIANS(l.latitude))
                       ))
                   )) <= :radiusKm
            ORDER BY distance ASC
            """, nativeQuery = true)
    List<DirectoryLawyer> findAllWithinDistance(
            @Param("userLat") double userLat,
            @Param("userLon") double userLon,
            @Param("radiusKm") double radiusKm
    );

    /**
     * Paginated version of findAllWithinDistance.
     * Find all lawyers within a specified distance from the given coordinates with pagination.
     *
     * @param userLat   User's latitude
     * @param userLon   User's longitude
     * @param radiusKm  Maximum distance in kilometers
     * @param pageable  Pagination information
     * @return Page of lawyers within the specified radius
     */
    @Query(value = """
            SELECT l.*, 
                   (6371 * ACOS(
                       LEAST(1.0, GREATEST(-1.0,
                           COS(RADIANS(:userLat)) * COS(RADIANS(l.latitude)) * COS(RADIANS(l.longitude) - RADIANS(:userLon)) +
                           SIN(RADIANS(:userLat)) * SIN(RADIANS(l.latitude))
                       ))
                   )) AS distance
            FROM directory_lawyers l
            WHERE l.latitude IS NOT NULL 
              AND l.longitude IS NOT NULL
              AND (6371 * ACOS(
                       LEAST(1.0, GREATEST(-1.0,
                           COS(RADIANS(:userLat)) * COS(RADIANS(l.latitude)) * COS(RADIANS(l.longitude) - RADIANS(:userLon)) +
                           SIN(RADIANS(:userLat)) * SIN(RADIANS(l.latitude))
                       ))
                   )) <= :radiusKm
            ORDER BY distance ASC
            """,
            countQuery = """
            SELECT COUNT(l.id)
            FROM directory_lawyers l
            WHERE l.latitude IS NOT NULL 
              AND l.longitude IS NOT NULL
              AND (6371 * ACOS(
                       LEAST(1.0, GREATEST(-1.0,
                           COS(RADIANS(:userLat)) * COS(RADIANS(l.latitude)) * COS(RADIANS(l.longitude) - RADIANS(:userLon)) +
                           SIN(RADIANS(:userLat)) * SIN(RADIANS(l.latitude))
                       ))
                   )) <= :radiusKm
            """,
            nativeQuery = true)
    Page<DirectoryLawyer> findAllWithinDistancePageable(
            @Param("userLat") double userLat,
            @Param("userLon") double userLon,
            @Param("radiusKm") double radiusKm,
            Pageable pageable
    );

    /**
     * Find all lawyers within distance with additional search filters.
     * Combines Haversine distance filtering with text-based search criteria.
     *
     * @param userLat     User's latitude
     * @param userLon     User's longitude
     * @param radiusKm    Maximum distance in kilometers
     * @param query       Optional search query for name, city, or specialization
     * @param expertise   Optional specialization filter
     * @param isVerified  Optional verified status filter
     * @return List of lawyers matching all criteria, sorted by distance
     */
    @Query(value = """
            SELECT l.*, 
                   (6371 * ACOS(
                       LEAST(1.0, GREATEST(-1.0,
                           COS(RADIANS(:userLat)) * COS(RADIANS(l.latitude)) * COS(RADIANS(l.longitude) - RADIANS(:userLon)) +
                           SIN(RADIANS(:userLat)) * SIN(RADIANS(l.latitude))
                       ))
                   )) AS distance
            FROM directory_lawyers l
            WHERE l.latitude IS NOT NULL 
              AND l.longitude IS NOT NULL
              AND (6371 * ACOS(
                       LEAST(1.0, GREATEST(-1.0,
                           COS(RADIANS(:userLat)) * COS(RADIANS(l.latitude)) * COS(RADIANS(l.longitude) - RADIANS(:userLon)) +
                           SIN(RADIANS(:userLat)) * SIN(RADIANS(l.latitude))
                       ))
                   )) <= :radiusKm
              AND (:query IS NULL OR :query = '' OR 
                   LOWER(l.full_name) LIKE LOWER(CONCAT('%', :query, '%')) OR
                   LOWER(l.city) LIKE LOWER(CONCAT('%', :query, '%')) OR
                   LOWER(l.specialization) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:expertise IS NULL OR :expertise = '' OR LOWER(l.specialization) LIKE LOWER(CONCAT('%', :expertise, '%')))
              AND (:isVerified IS NULL OR l.verified = :isVerified)
            ORDER BY distance ASC
            """, nativeQuery = true)
    List<DirectoryLawyer> findAllWithinDistanceFiltered(
            @Param("userLat") double userLat,
            @Param("userLon") double userLon,
            @Param("radiusKm") double radiusKm,
            @Param("query") String query,
            @Param("expertise") String expertise,
            @Param("isVerified") Boolean isVerified
    );
}


