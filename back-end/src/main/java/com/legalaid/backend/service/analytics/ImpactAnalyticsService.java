package com.legalaid.backend.service.analytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.legalaid.backend.dto.analytics.GeoCountDTO;
import com.legalaid.backend.dto.analytics.ImpactAnalyticsResponse;
import com.legalaid.backend.dto.analytics.LocationBreakdownDTO;
import com.legalaid.backend.dto.analytics.TotalGeoCountDTO;
import com.legalaid.backend.repository.CaseRepository;
import com.legalaid.backend.repository.LawyerProfileRepository;
import com.legalaid.backend.repository.NGOProfileRepository;
import com.legalaid.backend.repository.ProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImpactAnalyticsService {

    private final ProfileRepository profileRepository;
    private final CaseRepository caseRepository;
    private final NGOProfileRepository ngoProfileRepository;
    private final LawyerProfileRepository lawyerProfileRepository;

   public ImpactAnalyticsResponse getImpactAnalytics() {

    List<GeoCountDTO> lawyers = mapGeo(lawyerProfileRepository.countLawyersByGeo());
    List<GeoCountDTO> ngos = mapGeo(ngoProfileRepository.countNgosByGeo());
    List<GeoCountDTO> citizens = mapGeo(profileRepository.countCitizensByGeo());
    List<GeoCountDTO> cases = mapGeo(caseRepository.countCasesByGeo());
    List<TotalGeoCountDTO> totalByLocation =
            aggregateTotals(lawyers, ngos, citizens, cases);
    return new ImpactAnalyticsResponse(lawyers, ngos, citizens, cases,totalByLocation);
}
private List<TotalGeoCountDTO> aggregateTotals(
        List<GeoCountDTO> lawyers,
        List<GeoCountDTO> ngos,
        List<GeoCountDTO> citizens,
        List<GeoCountDTO> cases
) {
    Map<String, LocationBreakdownDTO> breakdownMap = new HashMap<>();
    Map<String, Long> totalMap = new HashMap<>();
    Map<String, Double[]> coordMap = new HashMap<>();

    addToMap(breakdownMap, totalMap, coordMap, lawyers, "lawyers");
    addToMap(breakdownMap, totalMap, coordMap, ngos, "ngos");
    addToMap(breakdownMap, totalMap, coordMap, citizens, "citizens");
    addToMap(breakdownMap, totalMap, coordMap, cases, "cases");

    List<TotalGeoCountDTO> result = new ArrayList<>();

    for (String key : breakdownMap.keySet()) {
        Double[] coords = coordMap.get(key);

        result.add(new TotalGeoCountDTO(
                coords[0],
                coords[1],
                totalMap.get(key),
                breakdownMap.get(key)
        ));
    }

    return result;
}
private void addToMap(
        Map<String, LocationBreakdownDTO> breakdownMap,
        Map<String, Long> totalMap,
        Map<String, Double[]> coordMap,
        List<GeoCountDTO> data,
        String type
) {
    for (GeoCountDTO d : data) {
        String key = d.getLatitude() + "," + d.getLongitude();

        coordMap.putIfAbsent(key, new Double[]{d.getLatitude(), d.getLongitude()});

        LocationBreakdownDTO existing =
                breakdownMap.getOrDefault(key, new LocationBreakdownDTO(0, 0, 0, 0));

        long lawyers = existing.getLawyers();
        long ngos = existing.getNgos();
        long citizens = existing.getCitizens();
        long cases = existing.getCases();

        switch (type) {
            case "lawyers" -> lawyers += d.getCount();
            case "ngos" -> ngos += d.getCount();
            case "citizens" -> citizens += d.getCount();
            case "cases" -> cases += d.getCount();
        }

        breakdownMap.put(key,
                new LocationBreakdownDTO(lawyers, ngos, citizens, cases)
        );

        totalMap.put(key, totalMap.getOrDefault(key, 0L) + d.getCount());
    }
}



private List<GeoCountDTO> mapGeo(List<Object[]> rows) {
    List<GeoCountDTO> result = new ArrayList<>();
    for (Object[] r : rows) {
        if (r[0] != null && r[1] != null) {   // null safety
            result.add(new GeoCountDTO(
                    (Double) r[0],   // latitude
                    (Double) r[1],   // longitude
                    (Long) r[2]      // count
            ));
        }
    }
    return result;
}

}
