package com.legalaid.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lawyer_profiles")
public class LawyerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String expertise;

    private String location;

    @Column(name = "contact_info", columnDefinition = "TEXT")
    private String contactInfo;

    // Columns defined in DB migration
    @Column(name = "bar_registration_no")
    private String barRegistrationNo;

    private String specialization;

    @Column(name = "experience_years")
    private Integer experienceYears;

    private String city;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    private Boolean verified;

    private String language;

    
    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;
}
