package com.legalaid.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "matches",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"case_id", "provider_type", "provider_id"})
    },
    indexes = {
        @Index(
            name = "idx_match_provider_status",
            columnList = "provider_type, provider_id, status"
        ),
        @Index(
            name = "idx_match_case",
            columnList = "case_id"
        )
    }
)
@Getter
@Setter
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseObj;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private ProviderType providerType;

    @Column(name = "provider_id", nullable = false)
    private Integer providerId;

    private Integer score;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt ;
    
    @Column(nullable = false)
    private Boolean saved = false;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
