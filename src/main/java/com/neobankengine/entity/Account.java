package com.neobankengine.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    private Long userId;

    private Double balance = 0.0;

    /**
     * ACTIVE or FROZEN (simple string approach)
     */
    private String status = "ACTIVE";

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
