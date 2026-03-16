package com.legalaid.backend.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "cases",
    indexes = {
        @Index(
            name = "idx_case_created_by",
            columnList = "created_by"
        )
    }
)
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "case_number", unique = true, length = 50)
    private String caseNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", length = 10)
    private CaseType caseType;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status = CaseStatus.OPEN;

    @Enumerated(EnumType.STRING)
    private CasePriority priority = CasePriority.MEDIUM;

    @ManyToOne
    @JoinColumn(name = "created_by", referencedColumnName = "id", nullable = false)
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "assigned_to", referencedColumnName = "id")
    private User assignedTo;

    private String location;

    private Boolean isUrgent = false;

    @Column(name = "contact_info", columnDefinition = "TEXT")
    private String contactInfo;

    private String category;

    @ElementCollection
    @CollectionTable(name = "case_expertise_tags", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "tag")
    private List<String> expertiseTags;

    private String preferredLanguage;

    private String parties;

    @OneToMany(mappedBy = "caseObj", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvidenceFile> evidenceFiles;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "city")
    private String city;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne
    @JoinColumn(name = "closed_by")
    private User closedBy;

    @Column(name = "closure_reason", length = 500)
    private String closureReason;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}