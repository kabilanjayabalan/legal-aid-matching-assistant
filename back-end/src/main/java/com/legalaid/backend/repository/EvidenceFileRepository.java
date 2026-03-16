package com.legalaid.backend.repository;

import com.legalaid.backend.model.EvidenceFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EvidenceFileRepository extends JpaRepository<EvidenceFile, Integer> {
    Optional<EvidenceFile> findById(Integer id);
}

