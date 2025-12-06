package com.neobankengine.controller;

import com.neobankengine.dto.NotificationDto;
import com.neobankengine.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // helper to get logged-in user's email from JWT subject
    private String currentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * Get notifications for current user.
     *
     * Examples:
     *  GET /api/notifications
     *  GET /api/notifications?unreadOnly=true
     *  GET /api/notifications?unreadOnly=true&limit=10
     */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> list(
            @RequestParam(value = "unreadOnly", required = false, defaultValue = "false") boolean unreadOnly,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        String email = currentUserEmail();
        List<NotificationDto> list =
                notificationService.getNotificationsForUser(email, unreadOnly, limit);
        return ResponseEntity.ok(list);
    }

    /**
     * Get unread notification count.
     * Response: { "count": 3 }
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        String email = currentUserEmail();
        long count = notificationService.getUnreadCount(email);

        Map<String, Long> body = new HashMap<>();
        body.put("count", count);

        return ResponseEntity.ok(body);
    }

    /**
     * Mark a single notification as read.
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        String email = currentUserEmail();
        notificationService.markAsRead(id, email);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mark all notifications for current user as read.
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        String email = currentUserEmail();
        notificationService.markAllAsRead(email);
        return ResponseEntity.noContent().build();
    }
}
