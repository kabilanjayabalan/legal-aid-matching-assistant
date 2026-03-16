package com.legalaid.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email"),
                @UniqueConstraint(columnNames = "username")
        })
public class User {

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Setter
    @Getter
    @NotBlank
    private String username;

    @Setter
    @Getter
    @NotBlank
    @Email
    private String email;

    @Setter
    @Getter
    @NotBlank
    private String password;

    @Setter
    @Getter
    @NotBlank
    private String fullName;

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    private Role role;

    @Getter
    @Setter
    @Column(name = "profile_id")
    private Integer profileId;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Getter
    @Setter
    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @Getter
    @Setter
    @Column(name = "status_reason")
    private String statusReason; // optional for SUSPENDED / BLOCKED


    @Setter
    @Getter
    @Column(name = "is_approved")
    private Boolean approved;

    @Setter
    @Getter
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {}

    public User(String username, String email, String password, String fullName, Role role, Boolean approved, UserStatus status) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.approved = approved;
        this.status = status;
    }
}