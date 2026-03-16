package com.legalaid.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.legalaid.backend.dto.DirectorySearchRequest;
import com.legalaid.backend.dto.ImportSummary;
import com.legalaid.backend.dto.ProviderDetailsDTO;
import com.legalaid.backend.model.DirectoryLawyer;
import com.legalaid.backend.model.DirectoryNgo;
import com.legalaid.backend.model.ImportMode;
import com.legalaid.backend.model.LawyerProfile;
import com.legalaid.backend.model.NGOProfile;
import com.legalaid.backend.repository.DirectoryLawyerRepository;
import com.legalaid.backend.repository.DirectoryNgoRepository;
import com.legalaid.backend.repository.LawyerProfileRepository;
import com.legalaid.backend.repository.NGOProfileRepository;

@Service
public class DirectoryService {

    private final DirectoryLawyerRepository directoryLawyerRepository;
    private final DirectoryNgoRepository directoryNgoRepository;
    private final LawyerProfileRepository lawyerProfileRepository;
    private final NGOProfileRepository ngoProfileRepository;
    private final GeocodingService geocodingService;

    public DirectoryService(DirectoryLawyerRepository directoryLawyerRepository,
                            DirectoryNgoRepository directoryNgoRepository,
                            LawyerProfileRepository lawyerProfileRepository,
                            NGOProfileRepository ngoProfileRepository,
                            GeocodingService geocodingService) {
        this.directoryLawyerRepository = directoryLawyerRepository;
        this.directoryNgoRepository = directoryNgoRepository;
        this.lawyerProfileRepository = lawyerProfileRepository;
        this.ngoProfileRepository = ngoProfileRepository;
        this.geocodingService = geocodingService;
    }

    public ImportSummary importLawyers(List<DirectoryLawyer> lawyers, ImportMode importMode) {
        ImportSummary summary = new ImportSummary();

        if (lawyers == null || lawyers.isEmpty()) {
            return summary;
        }

        ImportMode mode = (importMode != null) ? importMode : ImportMode.SKIP;

        for (DirectoryLawyer lawyer : lawyers) {
            summary.incrementTotalProcessed();

            if (lawyer.getBarRegistrationId() == null) {
                summary.addError("Skipped lawyer with null barRegistrationId");
                summary.incrementSkipped();
                continue;
            }

            // Fetch coordinates if missing
            if (lawyer.getLatitude() == null || lawyer.getLongitude() == null) {
                if (StringUtils.hasText(lawyer.getCity())) {
                    try {
                        // Respect Nominatim rate limit (1 request per second)
                        Thread.sleep(1100); 
                        Optional<double[]> coords = geocodingService.getCoordinates(lawyer.getCity());
                        if (coords.isPresent()) {
                            lawyer.setLatitude(coords.get()[0]);
                            lawyer.setLongitude(coords.get()[1]);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        summary.addError("Geocoding interrupted for lawyer: " + lawyer.getFullName());
                    }
                }
            }

            DirectoryLawyer existingLawyer = directoryLawyerRepository.findByBarRegistrationId(lawyer.getBarRegistrationId());

            if (existingLawyer != null) {
                // Record exists - apply conflict resolution strategy
                if (ImportMode.UPDATE.equals(mode)) {
                    try {
                        // Update existing entity with new data
                        existingLawyer.setFullName(lawyer.getFullName());
                        existingLawyer.setSpecialization(lawyer.getSpecialization());
                        existingLawyer.setCity(lawyer.getCity());
                        existingLawyer.setContactNumber(lawyer.getContactNumber());
                        existingLawyer.setEmail(lawyer.getEmail());
                        existingLawyer.setLatitude(lawyer.getLatitude());
                        existingLawyer.setLongitude(lawyer.getLongitude());
                        // Preserve source, verified status, and createdAt from existing record
                        // Only update source if it's not already set to EXTERNAL_IMPORT
                        if (existingLawyer.getSource() == null || !existingLawyer.getSource().equals("EXTERNAL_IMPORT")) {
                            existingLawyer.setSource("EXTERNAL_IMPORT");
                        }

                        directoryLawyerRepository.save(existingLawyer);
                        summary.incrementUpdated();
                    } catch (Exception e) {
                        summary.addError("Failed to update lawyer with barRegistrationId: " + lawyer.getBarRegistrationId() + " - " + e.getMessage());
                        summary.incrementSkipped();
                    }
                } else if (ImportMode.CREATE.equals(mode)) {
                    summary.addError("Duplicate lawyer skipped (barRegistrationId exists): " + lawyer.getBarRegistrationId());
                    summary.incrementSkipped();
                } else {
                    // SKIP on existing -> do nothing except count
                    summary.incrementSkipped();
                }
            } else {
                // New record - create it
                try {
                    lawyer.setId(null);
                    lawyer.setSource("EXTERNAL_IMPORT");
                    lawyer.setVerified(true);
                    if (lawyer.getCreatedAt() == null) {
                        lawyer.setCreatedAt(LocalDateTime.now());
                    }
                    directoryLawyerRepository.save(lawyer);
                    summary.incrementImported();
                } catch (Exception e) {
                    summary.addError("Failed to import lawyer with barRegistrationId: " + lawyer.getBarRegistrationId() + " - " + e.getMessage());
                    summary.incrementSkipped();
                }
            }
        }

        return summary;
    }

    public ImportSummary importNgos(List<DirectoryNgo> ngos, ImportMode importMode) {
        ImportSummary summary = new ImportSummary();

        if (ngos == null || ngos.isEmpty()) {
            return summary;
        }

        ImportMode mode = (importMode != null) ? importMode : ImportMode.SKIP;

        for (DirectoryNgo ngo : ngos) {
            summary.incrementTotalProcessed();

            if (ngo.getRegistrationNumber() == null) {
                summary.addError("Skipped NGO with null registrationNumber");
                summary.incrementSkipped();
                continue;
            }

            // Fetch coordinates if missing
            if (ngo.getLatitude() == null || ngo.getLongitude() == null) {
                if (StringUtils.hasText(ngo.getCity())) {
                    try {
                        // Respect Nominatim rate limit (1 request per second)
                        Thread.sleep(1100);
                        Optional<double[]> coords = geocodingService.getCoordinates(ngo.getCity());
                        if (coords.isPresent()) {
                            ngo.setLatitude(coords.get()[0]);
                            ngo.setLongitude(coords.get()[1]);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        summary.addError("Geocoding interrupted for NGO: " + ngo.getOrgName());
                    }
                }
            }

            DirectoryNgo existingNgo = directoryNgoRepository.findByRegistrationNumber(ngo.getRegistrationNumber());

            if (existingNgo != null) {
                // Record exists - apply conflict resolution strategy
                if (ImportMode.UPDATE.equals(mode)) {
                    try {
                        // Update existing entity with new data
                        existingNgo.setOrgName(ngo.getOrgName());
                        existingNgo.setFocusArea(ngo.getFocusArea());
                        existingNgo.setCity(ngo.getCity());
                        existingNgo.setContactNumber(ngo.getContactNumber());
                        existingNgo.setEmail(ngo.getEmail());
                        existingNgo.setWebsite(ngo.getWebsite());
                        existingNgo.setLatitude(ngo.getLatitude());
                        existingNgo.setLongitude(ngo.getLongitude());
                        // Preserve source, verified status, and createdAt from existing record
                        // Only update source if it's not already set to EXTERNAL_IMPORT
                        if (existingNgo.getSource() == null || !existingNgo.getSource().equals("EXTERNAL_IMPORT")) {
                            existingNgo.setSource("EXTERNAL_IMPORT");
                        }

                        directoryNgoRepository.save(existingNgo);
                        summary.incrementUpdated();
                    } catch (Exception e) {
                        summary.addError("Failed to update NGO with registrationNumber: " + ngo.getRegistrationNumber() + " - " + e.getMessage());
                        summary.incrementSkipped();
                    }
                } else if (ImportMode.CREATE.equals(mode)) {
                    summary.addError("Duplicate NGO skipped (registrationNumber exists): " + ngo.getRegistrationNumber());
                    summary.incrementSkipped();
                } else {
                    // SKIP strategy - do nothing
                    summary.incrementSkipped();
                }
            } else {
                // New record - create it
                try {
                    ngo.setId(null);
                    ngo.setSource("EXTERNAL_IMPORT");
                    ngo.setVerified(true);
                    if (ngo.getCreatedAt() == null) {
                        ngo.setCreatedAt(LocalDateTime.now());
                    }
                    directoryNgoRepository.save(ngo);
                    summary.incrementImported();
                } catch (Exception e) {
                    summary.addError("Failed to import NGO with registrationNumber: " + ngo.getRegistrationNumber() + " - " + e.getMessage());
                    summary.incrementSkipped();
                }
            }
        }

        return summary;
    }

    public List<DirectoryLawyer> searchLawyers(String city, String specialization) {
        // Search external directory
        List<DirectoryLawyer> externalResults = searchExternalLawyers(city, specialization);

        // Search internal profiles
        List<DirectoryLawyer> internalResults = searchInternalLawyers(city, specialization);

        // Combine results
        List<DirectoryLawyer> combined = new ArrayList<>();
        combined.addAll(externalResults);
        combined.addAll(internalResults);

        return combined;
    }

    private List<DirectoryLawyer> searchExternalLawyers(String city, String specialization) {
        boolean hasCity = StringUtils.hasText(city);
        boolean hasSpec = StringUtils.hasText(specialization);

        if (!hasCity && !hasSpec) {
            return directoryLawyerRepository.findAll();
        }

        List<DirectoryLawyer> results = new ArrayList<>();

        if (hasCity) {
            results = directoryLawyerRepository.findByCityContainingIgnoreCase(city);
        }

        if (hasSpec) {
            List<DirectoryLawyer> bySpec = directoryLawyerRepository.findBySpecializationContainingIgnoreCase(specialization);
            if (!results.isEmpty()) {
                // intersect
                results.retainAll(bySpec);
            } else {
                results = bySpec;
            }
        }

        return results;
    }

    private List<DirectoryLawyer> searchInternalLawyers(String city, String specialization) {
        List<LawyerProfile> profiles = new ArrayList<>();
        boolean hasCity = StringUtils.hasText(city);
        boolean hasSpec = StringUtils.hasText(specialization);

        if (!hasCity && !hasSpec) {
            profiles = lawyerProfileRepository.findAll();
        } else if (hasCity && hasSpec) {
            List<LawyerProfile> byCity = lawyerProfileRepository.findByCityContainingIgnoreCase(city);
            List<LawyerProfile> bySpec = lawyerProfileRepository.findBySpecializationContainingIgnoreCase(specialization);
            profiles = byCity.stream()
                    .filter(bySpec::contains)
                    .collect(Collectors.toList());
        } else if (hasCity) {
            profiles = lawyerProfileRepository.findByCityContainingIgnoreCase(city);
        } else {
            profiles = lawyerProfileRepository.findBySpecializationContainingIgnoreCase(specialization);
        }

        // Convert LawyerProfile to DirectoryLawyer format for unified response
        return profiles.stream()
                .filter(profile -> profile.getUser() != null &&
                        (profile.getVerified() == null || profile.getVerified()))
                .map(this::convertLawyerProfileToDirectoryLawyer)
                .collect(Collectors.toList());
    }

    private DirectoryLawyer convertLawyerProfileToDirectoryLawyer(LawyerProfile profile) {
        DirectoryLawyer lawyer = new DirectoryLawyer();
        lawyer.setId(profile.getId());
        lawyer.setFullName(profile.getName() != null ? profile.getName() : profile.getUser().getFullName());
        lawyer.setBarRegistrationId(profile.getBarRegistrationNo());
        lawyer.setSpecialization(profile.getSpecialization());
        lawyer.setCity(profile.getCity() != null ? profile.getCity() : profile.getLocation());
        lawyer.setContactNumber(extractContactFromInfo(profile.getContactInfo()));
        lawyer.setEmail(profile.getUser().getEmail());
        lawyer.setSource("INTERNAL");
        lawyer.setVerified(profile.getVerified() != null ? profile.getVerified() : false);
        lawyer.setLanguage(profile.getLanguage());
        lawyer.setCreatedAt(profile.getCreatedAt());
        lawyer.setLatitude(profile.getLatitude());
        lawyer.setLongitude(profile.getLongitude());
        return lawyer;
    }

    public List<DirectoryNgo> searchNgos(String city, String focusArea) {
        // Search external directory
        List<DirectoryNgo> externalResults = searchExternalNgos(city, focusArea);

        // Search internal profiles
        List<DirectoryNgo> internalResults = searchInternalNgos(city, focusArea);

        // Combine results
        List<DirectoryNgo> combined = new ArrayList<>();
        combined.addAll(externalResults);
        combined.addAll(internalResults);

        return combined;
    }

    private List<DirectoryNgo> searchExternalNgos(String city, String focusArea) {
        boolean hasCity = StringUtils.hasText(city);
        boolean hasFocus = StringUtils.hasText(focusArea);

        if (!hasCity && !hasFocus) {
            return directoryNgoRepository.findAll();
        }

        List<DirectoryNgo> results = new ArrayList<>();

        if (hasCity) {
            results = directoryNgoRepository.findByCityContainingIgnoreCase(city);
        }

        if (hasFocus) {
            List<DirectoryNgo> byFocus = directoryNgoRepository.findByFocusAreaContainingIgnoreCase(focusArea);
            if (!results.isEmpty()) {
                // intersect
                results.retainAll(byFocus);
            } else {
                results = byFocus;
            }
        }

        return results;
    }

    private List<DirectoryNgo> searchInternalNgos(String city, String focusArea) {
        List<NGOProfile> profiles = new ArrayList<>();
        boolean hasCity = StringUtils.hasText(city);

        if (!hasCity) {
            profiles = ngoProfileRepository.findAll();
        } else {
            profiles = ngoProfileRepository.findByCityContainingIgnoreCase(city);
        }

        // Filter by focusArea if provided (using description field)
        if (StringUtils.hasText(focusArea)) {
            String focusLower = focusArea.toLowerCase();
            profiles = profiles.stream()
                    .filter(profile -> profile.getDescription() != null &&
                            profile.getDescription().toLowerCase().contains(focusLower))
                    .collect(Collectors.toList());
        }

        // Convert NGOProfile to DirectoryNgo format for unified response
        return profiles.stream()
                .filter(profile -> profile.getUser() != null &&
                        (profile.getVerified() == null || profile.getVerified()))
                .map(this::convertNgoProfileToDirectoryNgo)
                .collect(Collectors.toList());
    }

    private DirectoryNgo convertNgoProfileToDirectoryNgo(NGOProfile profile) {
        DirectoryNgo ngo = new DirectoryNgo();
        ngo.setId(profile.getId());
        ngo.setOrgName(profile.getNgoName() != null ? profile.getNgoName() :
                (profile.getOrganization() != null ? profile.getOrganization() : profile.getUser().getFullName()));
        ngo.setRegistrationNumber(profile.getRegistrationNo());
        ngo.setFocusArea(profile.getDescription());
        ngo.setCity(profile.getCity() != null ? profile.getCity() : profile.getLocation());
        ngo.setContactNumber(extractContactFromInfo(profile.getContactInfo()));
        ngo.setEmail(profile.getUser().getEmail());
        ngo.setWebsite(profile.getWebsite());
        ngo.setSource("INTERNAL");
        ngo.setVerified(profile.getVerified() != null ? profile.getVerified() : false);
        ngo.setLanguage(profile.getLanguage());
        ngo.setCreatedAt(profile.getCreatedAt());
        ngo.setLatitude(profile.getLatitude());
        ngo.setLongitude(profile.getLongitude());

        return ngo;
    }

    private String extractContactFromInfo(String contactInfo) {
        if (contactInfo == null || contactInfo.trim().isEmpty()) {
            return null;
        }
        // Try to extract phone number from contact info
        // This is a simple implementation - you might want to enhance it
        String[] lines = contactInfo.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.matches(".*\\d{10,}.*")) {
                return line;
            }
        }
        return contactInfo.length() > 50 ? contactInfo.substring(0, 50) : contactInfo;
    }

    public Object searchDirectory(DirectorySearchRequest request) {
        // Normalize the type parameter
        String type = (request.getType() != null) ? request.getType().toUpperCase() : "BOTH";

        // Create Sort object based on sortBy and sortDir
        Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortDir())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, request.getSortBy());

        // Create Pageable with pagination and sorting
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // Normalize query parameters - set to null if empty
        String query = StringUtils.hasText(request.getQuery()) ? request.getQuery() : null;
        String location = StringUtils.hasText(request.getLocation()) ? request.getLocation() : null;
        String expertise = StringUtils.hasText(request.getExpertise()) ? request.getExpertise() : null;
        String language = StringUtils.hasText(request.getLanguage()) ? request.getLanguage() : null;

        // Handle different search types
        if ("LAWYER".equals(type)) {
            return searchLawyersCombined(query, location, expertise, language, request.getIsVerified(), pageable,request.getLatitude(),
            request.getLongitude(), request.getRadiusKm());
        } else if ("NGO".equals(type)) {
            return searchNgosCombined(query, location, expertise, language, request.getIsVerified(), pageable,request.getLatitude(),
            request.getLongitude(), request.getRadiusKm());
        } else {
            // BOTH type - return a Map with both lawyers and ngos
            Page<DirectoryLawyer> lawyersPage = searchLawyersCombined(
                    query, location, expertise, language, request.getIsVerified(), pageable,request.getLatitude(),
            request.getLongitude(), request.getRadiusKm()
            );
            Page<DirectoryNgo> ngosPage = searchNgosCombined(
                    query, location, expertise, language, request.getIsVerified(), pageable,request.getLatitude(),
            request.getLongitude(), request.getRadiusKm()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("lawyers", lawyersPage);
            result.put("ngos", ngosPage);
            return result;
        }
    }

    private Page<DirectoryLawyer> searchLawyersCombined(String query, String location, String expertise,
                                                        String language, Boolean isVerified, Pageable pageable,Double userLatitude,
                                                        Double userLongitude, Double radiusKm) {
        // Prepare language filters for Java-side filtering (handles multiple languages)
        List<String> languageFilters = language != null && !language.trim().isEmpty() ?
                Arrays.asList(language.split(",")) : new ArrayList<>();
        
        boolean useDistanceFilter = userLatitude != null && userLongitude != null && radiusKm != null;

        List<DirectoryLawyer> externalLawyers;

        if (useDistanceFilter) {
            // Use the new Haversine-based repository query for external directory
            // This returns results sorted by distance (nearest first)
            externalLawyers = directoryLawyerRepository.findAllWithinDistanceFiltered(
                    userLatitude, userLongitude, radiusKm, query, expertise, isVerified
            );
        } else {
            // Get ALL results from external directory (no pagination yet)
            // Don't filter by language in query - we'll filter in Java to handle multiple languages
            Pageable allPageable = PageRequest.of(0, Integer.MAX_VALUE, pageable.getSort());
            Page<DirectoryLawyer> externalPage = directoryLawyerRepository.searchLawyers(
                    query, location, expertise, null, isVerified, allPageable
            );
            externalLawyers = new ArrayList<>(externalPage.getContent());
        }

        // Filter external directory entries by language if specified
        externalLawyers = externalLawyers.stream()
                .filter(lawyer -> {
                    // If no language filter, include all entries
                    if (languageFilters.isEmpty()) {
                        return true;
                    }
                    // If entry has no language, exclude it when language filter is active
                    if (lawyer.getLanguage() == null || lawyer.getLanguage().trim().isEmpty()) {
                        return false;
                    }
                    // Check if entry's language contains any of the selected languages
                    String entryLanguage = lawyer.getLanguage().toLowerCase();
                    return languageFilters.stream()
                            .anyMatch(lang -> entryLanguage.contains(lang.trim().toLowerCase()));
                })
                .collect(Collectors.toList());

        // Get ALL results from internal profiles (no pagination yet)
        // Don't filter by language in query - we'll filter in Java to handle multiple languages
        Pageable allPageable = PageRequest.of(0, Integer.MAX_VALUE, pageable.getSort());
        Page<LawyerProfile> internalPage = lawyerProfileRepository.searchLawyers(
                query, location, expertise, null, isVerified, allPageable
        );

        // Convert internal profiles to DirectoryLawyer format and filter by language if specified
        List<DirectoryLawyer> internalLawyers = internalPage.getContent().stream()
                .filter(profile -> profile.getUser() != null)
                .filter(profile -> {
                    // If no language filter, include all profiles
                    if (languageFilters.isEmpty()) {
                        return true;
                    }
                    // If profile has no language, exclude it when language filter is active
                    if (profile.getLanguage() == null || profile.getLanguage().trim().isEmpty()) {
                        return false;
                    }
                    // Check if profile's language contains any of the selected languages
                    String profileLanguage = profile.getLanguage().toLowerCase();
                    return languageFilters.stream()
                            .anyMatch(lang -> profileLanguage.contains(lang.trim().toLowerCase()));
                })
                .map(this::convertLawyerProfileToDirectoryLawyer)
                .collect(Collectors.toList());

        // Filter internal profiles by distance if location filtering is active
        if (useDistanceFilter) {
            double userLat = userLatitude;
            double userLng = userLongitude;
            double radius = radiusKm;

            internalLawyers = internalLawyers.stream()
                    .filter(lawyer -> {
                        // Only include if coordinates are available and within radius
                        if (lawyer.getLatitude() != null && lawyer.getLongitude() != null) {
                            return distanceKm(userLat, userLng, lawyer.getLatitude(), lawyer.getLongitude()) <= radius;
                        }
                        // Fallback: include if city matches location filter
                        return location != null && lawyer.getCity() != null &&
                               lawyer.getCity().equalsIgnoreCase(location);
                    })
                    .collect(Collectors.toList());
        }

        // Combine results
        List<DirectoryLawyer> combined = new ArrayList<>();
        combined.addAll(externalLawyers);
        combined.addAll(internalLawyers);

        // Sort combined results
        if (useDistanceFilter) {
            // When using distance filter, sort by distance (nearest first)
            double userLat = userLatitude;
            double userLng = userLongitude;

            combined.sort((a, b) -> {
                Double distA = (a.getLatitude() != null && a.getLongitude() != null)
                        ? distanceKm(userLat, userLng, a.getLatitude(), a.getLongitude())
                        : Double.MAX_VALUE;
                Double distB = (b.getLatitude() != null && b.getLongitude() != null)
                        ? distanceKm(userLat, userLng, b.getLatitude(), b.getLongitude())
                        : Double.MAX_VALUE;
                return distA.compareTo(distB);
            });
        } else if (pageable.getSort().isSorted()) {
            // Sort by specified field if no distance filter
            Sort sort = pageable.getSort();
            combined.sort((a, b) -> {
                for (Sort.Order order : sort) {
                    int comparison = compareByField(a, b, order.getProperty(), order.getDirection());
                    if (comparison != 0) {
                        return comparison;
                    }
                }
                return 0;
            });
        }

        // Apply pagination to combined results
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), combined.size());
        List<DirectoryLawyer> pagedContent = start < combined.size() ?
                combined.subList(start, end) : new ArrayList<>();

        return new PageImpl<>(pagedContent, pageable, combined.size());
    }

    private int compareByField(DirectoryLawyer a, DirectoryLawyer b, String field, Sort.Direction direction) {
        Comparable<?> valueA = getFieldValue(a, field);
        Comparable<?> valueB = getFieldValue(b, field);

        if (valueA == null && valueB == null) return 0;
        if (valueA == null) return direction == Sort.Direction.ASC ? -1 : 1;
        if (valueB == null) return direction == Sort.Direction.ASC ? 1 : -1;

        @SuppressWarnings("unchecked")
        int result = ((Comparable<Object>) valueA).compareTo(valueB);
        return direction == Sort.Direction.ASC ? result : -result;
    }

    private Comparable<?> getFieldValue(DirectoryLawyer lawyer, String field) {
        return switch (field.toLowerCase()) {
            case "fullname", "name" -> lawyer.getFullName();
            case "city" -> lawyer.getCity();
            case "specialization" -> lawyer.getSpecialization();
            case "id" -> lawyer.getId();
            case "createdat", "created_at" -> lawyer.getCreatedAt();
            default -> lawyer.getId(); // default to ID
        };
    }

    private Page<DirectoryNgo> searchNgosCombined(String query, String location, String expertise,
                                                  String language, Boolean isVerified, Pageable pageable,
                                                Double userLatitude, Double userLongitude, Double radiusKm) {
        // Prepare language filters for Java-side filtering (handles multiple languages)
        List<String> languageFilters = language != null && !language.trim().isEmpty() ?
                Arrays.asList(language.split(",")) : new ArrayList<>();
        
        boolean useDistanceFilter = userLatitude != null && userLongitude != null && radiusKm != null;

        List<DirectoryNgo> externalNgos;

        if (useDistanceFilter) {
            // Use the new Haversine-based repository query for external directory
            // This returns results sorted by distance (nearest first)
            externalNgos = directoryNgoRepository.findAllWithinDistanceFiltered(
                    userLatitude, userLongitude, radiusKm, query, expertise, isVerified
            );
        } else {
            // Get ALL results from external directory (no pagination yet)
            // Don't filter by language in query - we'll filter in Java to handle multiple languages
            Pageable allPageable = PageRequest.of(0, Integer.MAX_VALUE, pageable.getSort());
            Page<DirectoryNgo> externalPage = directoryNgoRepository.searchNgos(
                    query, location, expertise, null, isVerified, allPageable
            );
            externalNgos = new ArrayList<>(externalPage.getContent());
        }

        // Filter external directory entries by language if specified
        externalNgos = externalNgos.stream()
                .filter(ngo -> {
                    // If no language filter, include all entries
                    if (languageFilters.isEmpty()) {
                        return true;
                    }
                    // If entry has no language, exclude it when language filter is active
                    if (ngo.getLanguage() == null || ngo.getLanguage().trim().isEmpty()) {
                        return false;
                    }
                    // Check if entry's language contains any of the selected languages
                    String entryLanguage = ngo.getLanguage().toLowerCase();
                    return languageFilters.stream()
                            .anyMatch(lang -> entryLanguage.contains(lang.trim().toLowerCase()));
                })
                .collect(Collectors.toList());

        // Get ALL results from internal profiles (no pagination yet)
        // Don't filter by language in query - we'll filter in Java to handle multiple languages
        Pageable allPageable = PageRequest.of(0, Integer.MAX_VALUE, pageable.getSort());
        Page<NGOProfile> internalPage = ngoProfileRepository.searchNgos(
                query, location, expertise, null, isVerified, allPageable
        );

        // Convert internal profiles to DirectoryNgo format and filter by language if specified
        List<DirectoryNgo> internalNgos = internalPage.getContent().stream()
                .filter(profile -> profile.getUser() != null)
                .filter(profile -> {
                    // If no language filter, include all profiles
                    if (languageFilters.isEmpty()) {
                        return true;
                    }
                    // If profile has no language, exclude it when language filter is active
                    if (profile.getLanguage() == null || profile.getLanguage().trim().isEmpty()) {
                        return false;
                    }
                    // Check if profile's language contains any of the selected languages
                    String profileLanguage = profile.getLanguage().toLowerCase();
                    return languageFilters.stream()
                            .anyMatch(lang -> profileLanguage.contains(lang.trim().toLowerCase()));
                })
                .map(this::convertNgoProfileToDirectoryNgo)
                .collect(Collectors.toList());

        // Filter internal profiles by distance if location filtering is active
        if (useDistanceFilter) {
            double userLat = userLatitude;
            double userLng = userLongitude;
            double radius = radiusKm;

            internalNgos = internalNgos.stream()
                    .filter(ngo -> {
                        // Only include if coordinates are available and within radius
                        if (ngo.getLatitude() != null && ngo.getLongitude() != null) {
                            return distanceKm(userLat, userLng, ngo.getLatitude(), ngo.getLongitude()) <= radius;
                        }
                        // Fallback: include if city matches location filter
                        return location != null && ngo.getCity() != null &&
                               ngo.getCity().equalsIgnoreCase(location);
                    })
                    .collect(Collectors.toList());
        }

        // Combine results
        List<DirectoryNgo> combined = new ArrayList<>();
        combined.addAll(externalNgos);
        combined.addAll(internalNgos);

        // Sort combined results
        if (useDistanceFilter) {
            // When using distance filter, sort by distance (nearest first)
            double userLat = userLatitude;
            double userLng = userLongitude;

            combined.sort((a, b) -> {
                Double distA = (a.getLatitude() != null && a.getLongitude() != null)
                        ? distanceKm(userLat, userLng, a.getLatitude(), a.getLongitude())
                        : Double.MAX_VALUE;
                Double distB = (b.getLatitude() != null && b.getLongitude() != null)
                        ? distanceKm(userLat, userLng, b.getLatitude(), b.getLongitude())
                        : Double.MAX_VALUE;
                return distA.compareTo(distB);
            });
        } else if (pageable.getSort().isSorted()) {
            // Sort by specified field if no distance filter
            Sort sort = pageable.getSort();
            combined.sort((a, b) -> {
                for (Sort.Order order : sort) {
                    int comparison = compareNgoByField(a, b, order.getProperty(), order.getDirection());
                    if (comparison != 0) {
                        return comparison;
                    }
                }
                return 0;
            });
        }

        // Apply pagination to combined results
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), combined.size());
        List<DirectoryNgo> pagedContent = start < combined.size() ?
                combined.subList(start, end) : new ArrayList<>();

        return new PageImpl<>(pagedContent, pageable, combined.size());
    }

    private int compareNgoByField(DirectoryNgo a, DirectoryNgo b, String field, Sort.Direction direction) {
        Comparable<?> valueA = getNgoFieldValue(a, field);
        Comparable<?> valueB = getNgoFieldValue(b, field);

        if (valueA == null && valueB == null) return 0;
        if (valueA == null) return direction == Sort.Direction.ASC ? -1 : 1;
        if (valueB == null) return direction == Sort.Direction.ASC ? 1 : -1;

        @SuppressWarnings("unchecked")
        int result = ((Comparable<Object>) valueA).compareTo(valueB);
        return direction == Sort.Direction.ASC ? result : -result;
    }

    private Comparable<?> getNgoFieldValue(DirectoryNgo ngo, String field) {
        return switch (field.toLowerCase()) {
            case "orgname", "name" -> ngo.getOrgName();
            case "city" -> ngo.getCity();
            case "focusarea", "focus_area" -> ngo.getFocusArea();
            case "id" -> ngo.getId();
            case "createdat", "created_at" -> ngo.getCreatedAt();
            default -> ngo.getId(); // default to ID
        };
    }

    /**
     * Gets all unique specializations from both internal lawyer profiles and external directory lawyers.
     * @return List of unique specializations, sorted alphabetically
     */
    public List<String> getAllSpecializations() {
        Set<String> specializations = new LinkedHashSet<>();

        // Get specializations from external directory
        List<String> externalSpecs = directoryLawyerRepository.findDistinctSpecializations();
        specializations.addAll(externalSpecs);

        // Get specializations from internal profiles
        List<String> internalSpecs = lawyerProfileRepository.findDistinctSpecializations();
        specializations.addAll(internalSpecs);

        // Return sorted list
        return specializations.stream()
                .filter(spec -> spec != null && !spec.trim().isEmpty())
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Gets all unique focus areas from both internal NGO profiles and external directory NGOs.
     * @return List of unique focus areas, sorted alphabetically
     */
    public List<String> getAllFocusAreas() {
        Set<String> focusAreas = new LinkedHashSet<>();

        // Get focus areas from external directory
        List<String> externalFocusAreas = directoryNgoRepository.findDistinctFocusAreas();
        focusAreas.addAll(externalFocusAreas);

        // Get focus areas from internal profiles (using description field)
        List<String> internalFocusAreas = ngoProfileRepository.findDistinctFocusAreas();
        focusAreas.addAll(internalFocusAreas);

        // Return sorted list
        return focusAreas.stream()
                .filter(area -> area != null && !area.trim().isEmpty())
                .sorted()
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> checkLawyerImports(List<String> barRegistrationIds) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (barRegistrationIds == null) {
            return result;
        }

        for (String barId : barRegistrationIds) {
            boolean exists = directoryLawyerRepository.existsByBarRegistrationId(barId);
            Map<String, Object> entry = new HashMap<>();
            entry.put("barRegistrationId", barId);
            entry.put("status", exists ? "MATCH" : "NEW_IMPORT");
            result.add(entry);
        }

        return result;
    }

    public List<Map<String, Object>> checkNgoImports(List<String> registrationNumbers) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (registrationNumbers == null) {
            return result;
        }

        for (String regNo : registrationNumbers) {
            boolean exists = directoryNgoRepository.existsByRegistrationNumber(regNo);
            Map<String, Object> entry = new HashMap<>();
            entry.put("registrationNumber", regNo);
            entry.put("status", exists ? "MATCH" : "NEW_IMPORT");
            result.add(entry);
        }

        return result;
    }

    /**
     * Gets lawyer details by ID from either internal profile or external directory.
     * @param lawyerId The ID of the lawyer profile
     * @return ProviderDetailsDTO with lawyer details, or null if not found
     */
    public ProviderDetailsDTO getLawyerDetailsById(Integer lawyerId) {
        // First check internal profiles
        java.util.Optional<LawyerProfile> internalProfile = lawyerProfileRepository.findById(lawyerId);
        if (internalProfile.isPresent()) {
            return convertLawyerProfileToDTO(internalProfile.get());
        }

        // Then check external directory
        java.util.Optional<DirectoryLawyer> directoryLawyer = directoryLawyerRepository.findById(lawyerId);
        if (directoryLawyer.isPresent()) {
            return convertDirectoryLawyerToDTO(directoryLawyer.get());
        }

        return null;
    }

    /**
     * Gets NGO details by ID from either internal profile or external directory.
     * @param ngoId The ID of the NGO profile
     * @return ProviderDetailsDTO with NGO details, or null if not found
     */
    public ProviderDetailsDTO getNgoDetailsById(Integer ngoId) {
        // First check internal profiles
        java.util.Optional<NGOProfile> internalProfile = ngoProfileRepository.findById(ngoId);
        if (internalProfile.isPresent()) {
            return convertNgoProfileToDTO(internalProfile.get());
        }

        // Then check external directory
        java.util.Optional<DirectoryNgo> directoryNgo = directoryNgoRepository.findById(ngoId);
        if (directoryNgo.isPresent()) {
            return convertDirectoryNgoToDTO(directoryNgo.get());
        }

        return null;
    }

    private ProviderDetailsDTO convertLawyerProfileToDTO(LawyerProfile profile) {
        ProviderDetailsDTO dto = new ProviderDetailsDTO();
        dto.setId(profile.getId());
        dto.setName(profile.getName() != null ? profile.getName() : 
                   (profile.getUser() != null ? profile.getUser().getFullName() : null));
        dto.setProviderType("LAWYER");
        dto.setVerified(profile.getVerified() != null ? profile.getVerified() : false);
        dto.setCity(profile.getCity());
        dto.setLocation(profile.getLocation());
        dto.setEmail(profile.getUser() != null ? profile.getUser().getEmail() : null);
        dto.setContactInfo(profile.getContactInfo());
        dto.setSpecialization(profile.getSpecialization());
        dto.setExpertise(profile.getExpertise());
        dto.setBio(profile.getBio());
        dto.setExperienceYears(profile.getExperienceYears());
        dto.setBarRegistrationNo(profile.getBarRegistrationNo());
        dto.setLanguage(profile.getLanguage());
        dto.setIsAvailable(profile.getIsAvailable());
        dto.setCreatedAt(profile.getCreatedAt());
        dto.setSource("INTERNAL");
        dto.setLatitude(profile.getLatitude());
        dto.setLongitude(profile.getLongitude());
        return dto;
    }

    private ProviderDetailsDTO convertDirectoryLawyerToDTO(DirectoryLawyer lawyer) {
        ProviderDetailsDTO dto = new ProviderDetailsDTO();
        dto.setId(lawyer.getId());
        dto.setName(lawyer.getFullName());
        dto.setProviderType("LAWYER");
        dto.setVerified(lawyer.getVerified() != null ? lawyer.getVerified() : false);
        dto.setCity(lawyer.getCity());
        dto.setEmail(lawyer.getEmail());
        dto.setContactNumber(lawyer.getContactNumber());
        dto.setContactInfo(lawyer.getContactNumber()); // Use contactNumber as contactInfo
        dto.setSpecialization(lawyer.getSpecialization());
        dto.setBarRegistrationNo(lawyer.getBarRegistrationId());
        dto.setLanguage(lawyer.getLanguage());
        dto.setCreatedAt(lawyer.getCreatedAt());
        dto.setSource(lawyer.getSource() != null ? lawyer.getSource() : "EXTERNAL");
        dto.setLatitude(lawyer.getLatitude());
        dto.setLongitude(lawyer.getLongitude());
        return dto;
    }

    private ProviderDetailsDTO convertNgoProfileToDTO(NGOProfile profile) {
        ProviderDetailsDTO dto = new ProviderDetailsDTO();
        dto.setId(profile.getId());
        dto.setName(profile.getNgoName() != null ? profile.getNgoName() : profile.getOrganization());
        dto.setProviderType("NGO");
        dto.setVerified(profile.getVerified() != null ? profile.getVerified() : false);
        dto.setCity(profile.getCity());
        dto.setLocation(profile.getLocation());
        dto.setEmail(profile.getUser() != null ? profile.getUser().getEmail() : null);
        dto.setContactInfo(profile.getContactInfo());
        dto.setWebsite(profile.getWebsite());
        dto.setDescription(profile.getDescription());
        dto.setFocusArea(profile.getDescription()); // Using description as focus area
        dto.setRegistrationNo(profile.getRegistrationNo());
        dto.setLanguage(profile.getLanguage());
        dto.setIsAvailable(profile.getIsAvailable());
        dto.setCreatedAt(profile.getCreatedAt());
        dto.setSource("INTERNAL");
        dto.setLatitude(profile.getLatitude());
        dto.setLongitude(profile.getLongitude());
        return dto;
    }

    private ProviderDetailsDTO convertDirectoryNgoToDTO(DirectoryNgo ngo) {
        ProviderDetailsDTO dto = new ProviderDetailsDTO();
        dto.setId(ngo.getId());
        dto.setName(ngo.getOrgName());
        dto.setProviderType("NGO");
        dto.setVerified(ngo.getVerified() != null ? ngo.getVerified() : false);
        dto.setCity(ngo.getCity());
        dto.setEmail(ngo.getEmail());
        dto.setContactNumber(ngo.getContactNumber());
        dto.setContactInfo(ngo.getContactNumber()); // Use contactNumber as contactInfo
        dto.setWebsite(ngo.getWebsite());
        dto.setFocusArea(ngo.getFocusArea());
        dto.setRegistrationNo(ngo.getRegistrationNumber());
        dto.setLanguage(ngo.getLanguage());
        dto.setCreatedAt(ngo.getCreatedAt());
        dto.setSource(ngo.getSource() != null ? ngo.getSource() : "EXTERNAL");
        dto.setLatitude(ngo.getLatitude());
        dto.setLongitude(ngo.getLongitude());
        return dto;
    }
    private double distanceKm(
        double lat1, double lon1,
        double lat2, double lon2) {

    final int R = 6371; // Earth radius (km)

    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
            * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

}
