package com.legalaid.backend.controller;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/location")
public class LocationController {
    RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/search")
    public ResponseEntity<String> searchCity(@RequestParam String q) {
        

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "LegalAidApp/1.0 (contact@legalaid.com)");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = "https://nominatim.openstreetmap.org/search"
                   + "?q=" + q
                   + "&format=json"
                   + "&limit=5"
                   + "&addressdetails=1";

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return ResponseEntity.ok(response.getBody());
    }
    @GetMapping("/reverse")
    public ResponseEntity<Map<String, Object>> reverse(
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        String url =
            "https://nominatim.openstreetmap.org/reverse"
            + "?format=json"
            + "&lat=" + lat
            + "&lon=" + lon;

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "LegalAidApp/1.0");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response =
            restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        return ResponseEntity.ok(response.getBody());
    }
}

