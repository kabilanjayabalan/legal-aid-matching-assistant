package com.legalaid.backend.controller.system;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.legalaid.backend.dto.system.MaintenanceStatusResponse;
import com.legalaid.backend.service.system.SystemSettingsService;

@RestController
@RequestMapping("/system")
public class SystemPublicController {

    private final SystemSettingsService systemSettingsService;

    public SystemPublicController(SystemSettingsService systemSettingsService) {
        this.systemSettingsService = systemSettingsService;
    }

    @GetMapping("/maintenance")
    public MaintenanceStatusResponse getMaintenanceStatus() {
        return systemSettingsService.getMaintenanceStatus();
    }
}

