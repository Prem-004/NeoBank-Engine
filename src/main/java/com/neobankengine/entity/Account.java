package com.neobankengine.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data

public class Account
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    private Long userId;

    private Double balance = 0.0;

    private String status = "ACTIVE";

    private LocalDateTime createdAt = LocalDateTime.now();
}
