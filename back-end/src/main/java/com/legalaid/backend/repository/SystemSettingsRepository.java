package com.legalaid.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.legalaid.backend.model.SystemSettings;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {

    Optional<SystemSettings> findFirstByOrderByIdAsc();
    
    default Optional<SystemSettings> getSettings() {
        return findFirstByOrderByIdAsc();
    }
}

