package com.legalaid.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalaid.backend.model.DirectoryLawyer;
import com.legalaid.backend.model.DirectoryNgo;
import com.legalaid.backend.repository.DirectoryLawyerRepository;
import com.legalaid.backend.repository.DirectoryNgoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class GeocodingService {

    private static final String NOMINATIM_API_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "LegalAidMatchingPlatform/1.0 (contact@legalaid.com)";
    private static final long RATE_LIMIT_DELAY_MS = 1000; // 1 second between requests

    // Delhi fallback coordinates (used when geocoding fails)
    private static final double DELHI_FALLBACK_LAT = 28.6139;
    private static final double DELHI_FALLBACK_LON = 77.2090;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DirectoryLawyerRepository directoryLawyerRepository;
    private final DirectoryNgoRepository directoryNgoRepository;

    public GeocodingService(DirectoryLawyerRepository directoryLawyerRepository,
                            DirectoryNgoRepository directoryNgoRepository) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.directoryLawyerRepository = directoryLawyerRepository;
        this.directoryNgoRepository = directoryNgoRepository;
    }

    /**
     * Fetches coordinates for a given location string from Nominatim API.
     *
     * @param location The city or location string to geocode
     * @return Optional containing [latitude, longitude] if found, empty otherwise
     */
    public Optional<double[]> getCoordinates(String location) {
        if (!StringUtils.hasText(location)) {
            return Optional.empty();
        }

        try {
            String sanitizedLocation = sanitizeLocationString(location);

            String url = UriComponentsBuilder.fromHttpUrl(NOMINATIM_API_URL)
                    .queryParam("q", sanitizedLocation)
                    .queryParam("format", "json")
                    .queryParam("limit", 1)
                    .queryParam("countrycodes", "in") // Limit to India for better accuracy
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.isArray() && !root.isEmpty()) {
                    JsonNode firstResult = root.get(0);
                    double lat = firstResult.get("lat").asDouble();
                    double lon = firstResult.get("lon").asDouble();
                    log.debug("Geocoded '{}' -> [{}, {}]", location, lat, lon);
                    return Optional.of(new double[]{lat, lon});
                } else {
                    log.warn("No geocoding results for location: '{}'", location);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching coordinates for location: '{}' - {}", location, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Sanitizes the location string before sending to Nominatim API.
     * Removes common problematic characters and normalizes the string.
     *
     * @param location The raw location string
     * @return Sanitized location string
     */
    private String sanitizeLocationString(String location) {
        if (location == null) {
            return "";
        }

        // Trim and normalize whitespace
        String sanitized = location.trim().replaceAll("\\s+", " ");

        // Remove common problematic patterns
        sanitized = sanitized
                .replaceAll("(?i)\\bdistrict\\b", "")
                .replaceAll("(?i)\\btaluk\\b", "")
                .replaceAll("(?i)\\btown\\b", "")
                .replaceAll("(?i)\\bvillage\\b", "")
                .replaceAll("[()\\[\\]{}]", "")
                .replaceAll(",\\s*,", ",")
                .replaceAll("^,|,$", "")
                .trim();

        return sanitized;
    }

    /**
     * Batch geocoding process to update all records with missing coordinates.
     * This method fetches all DirectoryLawyer and DirectoryNgo records with NULL
     * latitude or longitude and attempts to geocode them.
     *
     * @return GeocodingResult containing statistics and failed records
     */
    public GeocodingResult retryFailedGeocodings() {
        log.info("Starting batch geocoding for records with missing coordinates...");

        GeocodingResult result = new GeocodingResult();

        // Process DirectoryLawyers with missing coordinates
        List<DirectoryLawyer> lawyersWithMissingCoords = directoryLawyerRepository.findByLatitudeIsNullOrLongitudeIsNull();
        log.info("Found {} lawyers with missing coordinates", lawyersWithMissingCoords.size());
        result.setTotalLawyersProcessed(lawyersWithMissingCoords.size());

        for (DirectoryLawyer lawyer : lawyersWithMissingCoords) {
            try {
                // Rate limiting - sleep 1 second between requests
                Thread.sleep(RATE_LIMIT_DELAY_MS);

                String location = lawyer.getCity();
                if (!StringUtils.hasText(location)) {
                    // No city available - use Delhi fallback
                    lawyer.setLatitude(DELHI_FALLBACK_LAT);
                    lawyer.setLongitude(DELHI_FALLBACK_LON);
                    directoryLawyerRepository.save(lawyer);
                    result.incrementLawyersUpdated();
                    result.addFailedLawyer(lawyer.getId(), location, "Used Delhi fallback - No city/location available");
                    log.warn("Lawyer ID {} has no city, using Delhi fallback coordinates", lawyer.getId());
                    continue;
                }

                Optional<double[]> coords = getCoordinates(location);
                if (coords.isPresent()) {
                    lawyer.setLatitude(coords.get()[0]);
                    lawyer.setLongitude(coords.get()[1]);
                    directoryLawyerRepository.save(lawyer);
                    result.incrementLawyersUpdated();
                    log.info("Updated lawyer ID {} with coordinates [{}, {}]",
                            lawyer.getId(), coords.get()[0], coords.get()[1]);
                } else {
                    // Fallback to Delhi coordinates when geocoding fails
                    lawyer.setLatitude(DELHI_FALLBACK_LAT);
                    lawyer.setLongitude(DELHI_FALLBACK_LON);
                    directoryLawyerRepository.save(lawyer);
                    result.incrementLawyersUpdated();
                    result.addFailedLawyer(lawyer.getId(), location, "Used Delhi fallback - No results from Nominatim API");
                    log.warn("Lawyer ID {} geocoding failed for '{}', using Delhi fallback coordinates", lawyer.getId(), location);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Batch geocoding interrupted");
                break;
            } catch (Exception e) {
                result.addFailedLawyer(lawyer.getId(), lawyer.getCity(), e.getMessage());
                log.error("Error geocoding lawyer ID {}: {}", lawyer.getId(), e.getMessage());
            }
        }

        // Process DirectoryNgos with missing coordinates
        List<DirectoryNgo> ngosWithMissingCoords = directoryNgoRepository.findByLatitudeIsNullOrLongitudeIsNull();
        log.info("Found {} NGOs with missing coordinates", ngosWithMissingCoords.size());
        result.setTotalNgosProcessed(ngosWithMissingCoords.size());

        for (DirectoryNgo ngo : ngosWithMissingCoords) {
            try {
                // Rate limiting - sleep 1 second between requests
                Thread.sleep(RATE_LIMIT_DELAY_MS);

                String location = ngo.getCity();
                if (!StringUtils.hasText(location)) {
                    // No city available - use Delhi fallback
                    ngo.setLatitude(DELHI_FALLBACK_LAT);
                    ngo.setLongitude(DELHI_FALLBACK_LON);
                    directoryNgoRepository.save(ngo);
                    result.incrementNgosUpdated();
                    result.addFailedNgo(ngo.getId(), location, "Used Delhi fallback - No city/location available");
                    log.warn("NGO ID {} has no city, using Delhi fallback coordinates", ngo.getId());
                    continue;
                }

                Optional<double[]> coords = getCoordinates(location);
                if (coords.isPresent()) {
                    ngo.setLatitude(coords.get()[0]);
                    ngo.setLongitude(coords.get()[1]);
                    directoryNgoRepository.save(ngo);
                    result.incrementNgosUpdated();
                    log.info("Updated NGO ID {} with coordinates [{}, {}]",
                            ngo.getId(), coords.get()[0], coords.get()[1]);
                } else {
                    // Fallback to Delhi coordinates when geocoding fails
                    ngo.setLatitude(DELHI_FALLBACK_LAT);
                    ngo.setLongitude(DELHI_FALLBACK_LON);
                    directoryNgoRepository.save(ngo);
                    result.incrementNgosUpdated();
                    result.addFailedNgo(ngo.getId(), location, "Used Delhi fallback - No results from Nominatim API");
                    log.warn("NGO ID {} geocoding failed for '{}', using Delhi fallback coordinates", ngo.getId(), location);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Batch geocoding interrupted");
                break;
            } catch (Exception e) {
                result.addFailedNgo(ngo.getId(), ngo.getCity(), e.getMessage());
                log.error("Error geocoding NGO ID {}: {}", ngo.getId(), e.getMessage());
            }
        }

        log.info("Batch geocoding completed. Lawyers updated: {}/{}, NGOs updated: {}/{}",
                result.getLawyersUpdated(), result.getTotalLawyersProcessed(),
                result.getNgosUpdated(), result.getTotalNgosProcessed());

        return result;
    }

    /**
     * Gets the count of records with missing coordinates for preview purposes.
     *
     * @return Map containing counts of lawyers and NGOs with missing coordinates
     */
    public Map<String, Object> getMissingCoordinatesCount() {
        long lawyerCount = directoryLawyerRepository.countByLatitudeIsNullOrLongitudeIsNull();
        long ngoCount = directoryNgoRepository.countByLatitudeIsNullOrLongitudeIsNull();

        Map<String, Object> counts = new HashMap<>();
        counts.put("lawyersWithMissingCoordinates", lawyerCount);
        counts.put("ngosWithMissingCoordinates", ngoCount);
        counts.put("totalRecordsToProcess", lawyerCount + ngoCount);
        counts.put("estimatedTimeMinutes", (lawyerCount + ngoCount) / 60); // 1 second per request

        return counts;
    }

    /**
     * Result class to hold batch geocoding statistics.
     */
    public static class GeocodingResult {
        private int totalLawyersProcessed;
        private int lawyersUpdated;
        private int totalNgosProcessed;
        private int ngosUpdated;
        private List<FailedRecord> failedLawyers = new ArrayList<>();
        private List<FailedRecord> failedNgos = new ArrayList<>();

        public int getTotalLawyersProcessed() {
            return totalLawyersProcessed;
        }

        public void setTotalLawyersProcessed(int totalLawyersProcessed) {
            this.totalLawyersProcessed = totalLawyersProcessed;
        }

        public int getLawyersUpdated() {
            return lawyersUpdated;
        }

        public void incrementLawyersUpdated() {
            this.lawyersUpdated++;
        }

        public int getTotalNgosProcessed() {
            return totalNgosProcessed;
        }

        public void setTotalNgosProcessed(int totalNgosProcessed) {
            this.totalNgosProcessed = totalNgosProcessed;
        }

        public int getNgosUpdated() {
            return ngosUpdated;
        }

        public void incrementNgosUpdated() {
            this.ngosUpdated++;
        }

        public List<FailedRecord> getFailedLawyers() {
            return failedLawyers;
        }

        public void addFailedLawyer(Integer id, String location, String reason) {
            this.failedLawyers.add(new FailedRecord(id, location, reason));
        }

        public List<FailedRecord> getFailedNgos() {
            return failedNgos;
        }

        public void addFailedNgo(Integer id, String location, String reason) {
            this.failedNgos.add(new FailedRecord(id, location, reason));
        }
    }

    /**
     * Record class to hold information about failed geocoding attempts.
     */
    public static class FailedRecord {
        private final Integer id;
        private final String location;
        private final String reason;

        public FailedRecord(Integer id, String location, String reason) {
            this.id = id;
            this.location = location;
            this.reason = reason;
        }

        public Integer getId() {
            return id;
        }

        public String getLocation() {
            return location;
        }

        public String getReason() {
            return reason;
        }
    }
}
