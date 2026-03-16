package com.legalaid.backend.external.scheduler;

import com.legalaid.backend.external.dto.NgoExternalDTO;
import com.legalaid.backend.external.service.NgoIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NgoIngestionScheduler {

    private final NgoIngestionService service;

    @Scheduled(cron = "0 0 2 * * ?")
    public void run() {
        service.ingest(List.of(
                new NgoExternalDTO("Helping Hands", "NGO001", "Delhi", "Delhi"),
                new NgoExternalDTO("Justice Care", "NGO002", "Mumbai", "Maharashtra")
        ));
    }
}