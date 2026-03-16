package com.legalaid.backend.external.service;

import com.legalaid.backend.repository.ProfileRepository;
import com.legalaid.backend.external.dto.NgoExternalDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NgoIngestionService {

    private final ProfileRepository ProfileRepository;

    public void ingest(List<NgoExternalDTO> ngos) {
        ngos.forEach(ngo ->
                ProfileRepository.upsertNgoProfile(
                        ngo.getName(),
                        ngo.getCity(),
                        ngo.getState()
                )
        );
    }
}