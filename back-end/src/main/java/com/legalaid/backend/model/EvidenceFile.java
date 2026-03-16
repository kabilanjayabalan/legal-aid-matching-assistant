package com.legalaid.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evidence_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "data", nullable = false, columnDefinition = "BYTEA")
    private byte[] data;

    @ManyToOne
    @JoinColumn(name = "case_id", referencedColumnName = "id", nullable = false)
    private Case caseObj;
}

