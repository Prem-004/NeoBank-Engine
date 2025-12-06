package com.neobankengine.repository;

import com.neobankengine.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    long countByUserEmailAndReadFlagFalse(String userEmail);

    List<Notification> findByUserEmailAndReadFlagFalseOrderByCreatedAtDesc(String userEmail);

}
