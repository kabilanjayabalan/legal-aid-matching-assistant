package com.legalaid.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.legalaid.backend.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);
    
    List<Notification> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
}
