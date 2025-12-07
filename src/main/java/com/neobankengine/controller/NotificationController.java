package com.neobankengine.controller;

import com.neobankengine.dto.NotificationDto;
import com.neobankengine.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // GET /api/notifications?unreadOnly=true&limit=10 (params optional)
    @GetMapping
    public ResponseEntity<List<NotificationDto>> list(
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly,
            @RequestParam(value = "limit", required = false) Integer limit) {

        String email = currentUserEmail();
        List<NotificationDto> list =
                notificationService.getNotificationsForUser(email, unreadOnly, limit);
        return ResponseEntity.ok(list);
    }

    // GET /api/notifications/unread-count
    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount() {
        String email = currentUserEmail();
        return ResponseEntity.ok(notificationService.getUnreadCount(email));
    }

    // POST /api/notifications/{id}/read
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        String email = currentUserEmail();
        notificationService.markAsRead(id, email);
        return ResponseEntity.ok().build();
    }

    // POST /api/notifications/mark-all-read
    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllRead() {
        String email = currentUserEmail();
        notificationService.markAllAsRead(email);
        return ResponseEntity.ok().build();
    }
}
