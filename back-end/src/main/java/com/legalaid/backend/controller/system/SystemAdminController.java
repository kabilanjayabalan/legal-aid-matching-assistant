package com.legalaid.backend.controller.system;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.legalaid.backend.dto.system.MaintenanceUpdateRequest;
import com.legalaid.backend.service.system.SystemSettingsService;

@RestController
@RequestMapping("/admin/system")
@PreAuthorize("hasAuthority('ADMIN')")
public class SystemAdminController {

    private final SystemSettingsService systemSettingsService;

    public SystemAdminController(SystemSettingsService systemSettingsService) {
        this.systemSettingsService = systemSettingsService;
    }

    @PostMapping("/maintenance")
    public ResponseEntity<Void> updateMaintenance(
            @RequestBody MaintenanceUpdateRequest request,
            Authentication authentication) {

        systemSettingsService.updateMaintenance(
                request,
                authentication.getName()
        );

        return ResponseEntity.ok().build();
    }
}


