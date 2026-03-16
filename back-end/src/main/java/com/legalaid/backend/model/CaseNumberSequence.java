package com.legalaid.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity to track sequential case numbers per case type per year.
 * This ensures unique and sequential case numbers for each type and year combination.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "case_number_sequence",
    uniqueConstraints = @UniqueConstraint(columnNames = {"case_type", "year"})
)
public class CaseNumberSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "case_type", nullable = false, length = 10)
    private String caseType;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "last_number", nullable = false)
    private Integer lastNumber = 0;
}

