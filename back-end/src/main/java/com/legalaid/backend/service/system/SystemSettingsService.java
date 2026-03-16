package com.legalaid.backend.service.system;

import org.springframework.stereotype.Service;

import com.legalaid.backend.dto.system.MaintenanceStatusResponse;
import com.legalaid.backend.dto.system.MaintenanceUpdateRequest;
import com.legalaid.backend.model.SystemSettings;
import com.legalaid.backend.repository.SystemSettingsRepository;

@Service
public class SystemSettingsService {

    private final SystemSettingsRepository repository;

    public SystemSettingsService(SystemSettingsRepository repository) {
        this.repository = repository;
    }

    public MaintenanceStatusResponse getMaintenanceStatus() {
        SystemSettings settings = repository.getSettings()
            .orElseGet(this::createDefault);
            // ✅ AUTO-DISABLE AFTER END TIME
    if (settings.isMaintenanceEnabled()
        && settings.getMaintenanceEnd() != null
        && settings.getMaintenanceEnd().isBefore(java.time.LocalDateTime.now())) {

        settings.setMaintenanceEnabled(false);
        settings.setMaintenanceStart(null);
        settings.setMaintenanceEnd(null);
        settings.setMaintenanceMessage(null);

        repository.save(settings); // 🔥 persist change
    }

        return new MaintenanceStatusResponse(
            settings.isMaintenanceEnabled(),
            settings.getMaintenanceStart(),
            settings.getMaintenanceEnd(),
            settings.getMaintenanceMessage()
        );
    }

    public void updateMaintenance(MaintenanceUpdateRequest req, String adminEmail) {
        SystemSettings settings = repository.getSettings()
            .orElseGet(this::createDefault);

        settings.setMaintenanceEnabled(req.enabled());
        settings.setMaintenanceStart(req.start());
        settings.setMaintenanceEnd(req.end());
        settings.setMaintenanceMessage(req.message());
        settings.setUpdatedBy(adminEmail);

        repository.save(settings);
    }

    public void disableMaintenance() {
        SystemSettings settings = repository.getSettings()
            .orElseGet(this::createDefault);

        settings.setMaintenanceEnabled(false);
        settings.setMaintenanceStart(null);
        settings.setMaintenanceEnd(null);

        repository.save(settings);
    }

    private SystemSettings createDefault() {
        SystemSettings s = new SystemSettings();
        s.setMaintenanceEnabled(false);
        return repository.save(s);
    }
}
