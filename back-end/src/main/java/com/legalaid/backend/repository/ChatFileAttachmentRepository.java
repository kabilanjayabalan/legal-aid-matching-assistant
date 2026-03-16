package com.legalaid.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.legalaid.backend.model.ChatFileAttachment;

@Repository
public interface ChatFileAttachmentRepository extends JpaRepository<ChatFileAttachment, Long> {
}


