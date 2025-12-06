package com.neobankengine.service;

import com.neobankengine.dto.NotificationDto;
import com.neobankengine.entity.Notification;
import com.neobankengine.exception.ForbiddenException;
import com.neobankengine.exception.ResourceNotFoundException;
import com.neobankengine.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    // JavaMailSender is optional — if not configured, email sending will be skipped with a log.
    private final JavaMailSender mailSender;

    // ------------------------------------------------
    // CREATE
    // ------------------------------------------------
    @Transactional
    public NotificationDto createNotification(String userEmail,
                                              String title,
                                              String message,
                                              String type,
                                              String meta) {

        Notification n = Notification.builder()
                .userEmail(userEmail)
                .title(title)
                .message(message)
                .type(type)
                .meta(meta)
                .build();

        Notification saved = notificationRepository.save(n);

        // Fire-and-forget email (best-effort). Wrap in try/catch.
        try {
            sendEmailIfConfigured(userEmail, title, message);
        } catch (Exception ex) {
            log.warn("Email send failed for {}: {}", userEmail, ex.getMessage());
        }

        return toDto(saved);
    }

    // ------------------------------------------------
    // READ – original simple list
    // ------------------------------------------------
    /**
     * OLD method kept for compatibility.
     * Returns all notifications for user (newest first).
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(String userEmail) {
        return notificationRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------
    // READ – new filtered list (Day 15)
    // ------------------------------------------------
    /**
     * NEW: get notifications for a user with optional filters.
     *
     * @param email      user email
     * @param unreadOnly if true, only unread notifications
     * @param limit      maximum number of results (null or <=0 means no limit)
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsForUser(String email,
                                                         Boolean unreadOnly,
                                                         Integer limit) {

        List<Notification> list;
        if (Boolean.TRUE.equals(unreadOnly)) {
            // Make sure this method exists in NotificationRepository:
            // List<Notification> findByUserEmailAndReadFlagFalseOrderByCreatedAtDesc(String userEmail);
            list = notificationRepository.findByUserEmailAndReadFlagFalseOrderByCreatedAtDesc(email);
        } else {
            list = notificationRepository.findByUserEmailOrderByCreatedAtDesc(email);
        }

        if (limit != null && limit > 0 && list.size() > limit) {
            list = list.subList(0, limit);
        }

        return list.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------
    // UNREAD COUNT
    // ------------------------------------------------
    /**
     * OLD name used elsewhere.
     */
    @Transactional(readOnly = true)
    public long countUnread(String userEmail) {
        return notificationRepository.countByUserEmailAndReadFlagFalse(userEmail);
    }

    /**
     * NEW nicer name – just delegates to countUnread.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        return countUnread(email);
    }

    // ------------------------------------------------
    // MARK AS READ (single)
    // ------------------------------------------------
    @Transactional
    public void markAsRead(Long notificationId, String userEmail) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!n.getUserEmail().equals(userEmail)) {
            throw new ForbiddenException("Cannot modify another user's notification");
        }

        if (!n.isReadFlag()) {
            n.setReadFlag(true);
            notificationRepository.save(n);
        }
    }

    // ------------------------------------------------
    // MARK ALL AS READ
    // ------------------------------------------------
    @Transactional
    public void markAllAsRead(String email) {

        // Uses same repo method as above for unread
        List<Notification> list =
                notificationRepository.findByUserEmailAndReadFlagFalseOrderByCreatedAtDesc(email);

        if (list.isEmpty()) {
            return;
        }

        for (Notification n : list) {
            n.setReadFlag(true);
        }
        notificationRepository.saveAll(list);
    }

    // ------------------------------------------------
    // HELPERS
    // ------------------------------------------------
    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.isReadFlag(),
                n.getCreatedAt()
        );
    }

    private void sendEmailIfConfigured(String to, String subject, String text) {
        if (mailSender == null) {
            log.info("MailSender not configured - skipping email to {}", to);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
            log.info("Notification email sent to {}", to);
        } catch (Exception e) {
            log.warn("Failed sending email to {}: {}", to, e.getMessage());
        }
    }
}
