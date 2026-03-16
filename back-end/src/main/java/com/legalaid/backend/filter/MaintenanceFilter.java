package com.legalaid.backend.filter;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.legalaid.backend.dto.system.MaintenanceStatusResponse;
import com.legalaid.backend.service.system.SystemSettingsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MaintenanceFilter extends OncePerRequestFilter {

    private final SystemSettingsService service;

    public MaintenanceFilter(SystemSettingsService service) {
        this.service = service;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws IOException, ServletException {

        String path = request.getRequestURI();

        // ✅ ALWAYS allow these
        if (path.startsWith("/auth") ||
            path.startsWith("/system/maintenance") ||
            path.startsWith("/admin") ||
            path.startsWith("/actuator")) {

            filterChain.doFilter(request, response);
            return;
        }

        MaintenanceStatusResponse status = service.getMaintenanceStatus();

        if (status.enabled()) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                { "message": "System is under maintenance" }
            """);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
