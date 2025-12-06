package com.neobankengine.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Owner (user) email or user id — choose email (you already use email in services)
    private String userEmail;

    private String title;

    @Column(length = 2000)
    private String message;

    private String type; // e.g. DEPOSIT, WITHDRAW, TRANSFER, INFO

    private boolean readFlag = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    // optional metadata (json string) if needed
    @Column(length = 2000)
    private String meta;
}
