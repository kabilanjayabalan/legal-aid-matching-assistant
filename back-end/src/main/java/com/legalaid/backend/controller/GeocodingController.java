package com.legalaid.backend.controller;

import com.legalaid.backend.service.GeocodingService;
import com.legalaid.backend.service.GeocodingService.GeocodingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing geocoding operations.
 * Provides admin endpoints to retry failed geocoding and check status.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/geocoding")
public class GeocodingController {

    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    /**
     * Preview endpoint to check how many records need geocoding.
     * Returns the count of DirectoryLawyer and DirectoryNgo records with missing coordinates.
     *
     * @return Map containing counts and estimated processing time
     */
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> getGeocodingStatus() {
        log.info("Admin requested geocoding status");
        Map<String, Object> counts = geocodingService.getMissingCoordinatesCount();
        return ResponseEntity.ok(counts);
    }

    /**
     * Triggers batch geocoding for all records with missing coordinates.
     * This is a long-running operation that respects Nominatim's 1 request/second rate limit.
     *
     * WARNING: This operation can take a long time depending on the number of records.
     * For large datasets, consider running this during off-peak hours.
     *
     * @return GeocodingResult containing statistics and failed records
     */
    @PostMapping("/retry-failed")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<GeocodingResult> retryFailedGeocodings() {
        log.info("Admin triggered batch geocoding retry");

        try {
            GeocodingResult result = geocodingService.retryFailedGeocodings();

            log.info("Batch geocoding completed. Lawyers: {}/{} updated, NGOs: {}/{} updated",
                    result.getLawyersUpdated(), result.getTotalLawyersProcessed(),
                    result.getNgosUpdated(), result.getTotalNgosProcessed());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during batch geocoding: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Geocode a single location string (for testing purposes).
     *
     * @param location The location string to geocode
     * @return Coordinates if found, or error message
     */
    @GetMapping("/test")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> testGeocoding(@RequestParam String location) {
        log.info("Admin testing geocoding for location: {}", location);

        var coordsOpt = geocodingService.getCoordinates(location);

        Map<String, Object> response = new java.util.HashMap<>();
        if (coordsOpt.isPresent()) {
            double[] coords = coordsOpt.get();
            response.put("success", true);
            response.put("location", location);
            response.put("latitude", coords[0]);
            response.put("longitude", coords[1]);
        } else {
            response.put("success", false);
            response.put("location", location);
            response.put("message", "No coordinates found for this location");
        }

        return ResponseEntity.ok(response);
    }
}

