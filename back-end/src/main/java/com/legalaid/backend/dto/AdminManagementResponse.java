package com.legalaid.backend.dto;

import com.legalaid.backend.model.UserStatus;

public class AdminManagementResponse {

    private Integer id;
    private String email;
    private String fullName;
    private String role;
    private UserStatus status;
    private String createdAt;

    // ✅ Constructor
    public AdminManagementResponse(
            Integer id,
            String email,
            String fullName,
            String role,
            UserStatus status,
            String createdAt
    ) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    // ✅ Getters (add setters only if needed)
    public Integer getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
