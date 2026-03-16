package com.legalaid.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.legalaid.backend.model.SystemSettings;

@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {

    @Query("SELECT s FROM SystemSettings s ORDER BY s.id ASC")
    Optional<SystemSettings> getSettings();
}

