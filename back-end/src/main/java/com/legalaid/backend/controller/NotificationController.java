package com.legalaid.backend.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.legalaid.backend.model.Notification;
import com.legalaid.backend.model.User;
import com.legalaid.backend.repository.NotificationRepository;
import com.legalaid.backend.repository.UserRepository;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository repo;
    private final UserRepository userRepository;

    public NotificationController(
            NotificationRepository repo,
            UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    // 🔔 Bell dropdown
    @GetMapping
    public List<Notification> getNotifications(Authentication auth) {
        User user = getLoggedInUser(auth);
        Long userId = Long.valueOf(user.getId());
        return repo.findByUserIdOrderByCreatedAtDesc(
                userId
        );
    }

    // 🔴 Red dot count
    @GetMapping("/unread-count")
    public long getUnreadCount(Authentication auth) {
        User user = getLoggedInUser(auth);
        Long userId = Long.valueOf(user.getId());
        return repo.countByUserIdAndIsReadFalse(
                userId
        );
    }

    // ✔ Mark as read (ownership enforced)
    @PutMapping("/{id}/read")
    public void markAsRead(
            @PathVariable Long id,
            Authentication auth) {

        User user = getLoggedInUser(auth);

        Notification notification = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // 🔐 OWNERSHIP CHECK
        if (!notification.getUserId().equals(user.getId().toString())) {
            throw new RuntimeException("Unauthorized access");
        }

        notification.setIsRead(true);
        repo.save(notification);
    }
    @GetMapping("/recent")
    public List<Notification> getRecentNotifications(Authentication auth) {
        User user = getLoggedInUser(auth);
        Long userId = Long.valueOf(user.getId());
        return repo.findTop5ByUserIdOrderByCreatedAtDesc(userId);
    }
    /* ================= HELPER ================= */

    private User getLoggedInUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found: " + auth.getName()));
    }
}
