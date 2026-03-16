package com.legalaid.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.legalaid.backend.dto.AdminManagementResponse;
import com.legalaid.backend.dto.AppointmentStatsDTO;
import com.legalaid.backend.model.AppointmentStatus;
import com.legalaid.backend.model.Role;
import com.legalaid.backend.model.User;
import com.legalaid.backend.model.UserStatus;
import com.legalaid.backend.repository.AppointmentRepository;
import com.legalaid.backend.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public void changeStatus(Integer userId, UserStatus newStatus) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // 🚫 Never allow admin to be modified
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Admin status cannot be changed");
        }

        // 🚫 No-op
        if (user.getStatus() == newStatus) {
            return;
        }

        user.setStatus(newStatus);
        user.setStatusChangedAt(LocalDateTime.now());

        userRepository.save(user);
    }
    public Page<AdminManagementResponse> getUsers(
            int page,
            int size,
            String search,
            String role,
            UserStatus status
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // Convert String role to Role enum
        Role roleEnum = null;
        if (role != null && !role.isBlank()) {
            try {
                roleEnum = Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid role, will be treated as null (no filter)
            }
        }

        Page<User> users = userRepository.findUsers(
                search == null ? "" : search.toLowerCase(),
                roleEnum,
                status,
                pageable
        );

        return users.map(user -> new AdminManagementResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getStatus(),
                user.getCreatedAt().toString()
        ));
    }
     public AppointmentStatsDTO getAppointmentStats() {

        LocalDate today = LocalDate.now();

        long total = appointmentRepository.count();

        long pending = appointmentRepository
                .countByStatus(AppointmentStatus.PENDING);

        long confirmed = appointmentRepository
                .countByStatus(AppointmentStatus.CONFIRMED);

        long completed = appointmentRepository
                .countByStatus(AppointmentStatus.COMPLETED);

        long cancelled = appointmentRepository
                .countByStatus(AppointmentStatus.CANCELLED);

        long upcoming = appointmentRepository
                .countByStatusAndAppointmentDateGreaterThanEqual(
                        AppointmentStatus.CONFIRMED, today);

        return new AppointmentStatsDTO(
                total,
                upcoming,
                pending,
                confirmed,
                completed,
                cancelled
        );
    }
}

