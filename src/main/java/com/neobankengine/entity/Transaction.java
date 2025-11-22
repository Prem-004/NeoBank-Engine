package com.neobankengine.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data

public class Transaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    private Long accountId;

    private String type; // CREDIT or DEBIT

    private Double amount;

    private LocalDateTime timestamp = LocalDateTime.now();

    private String referenceText;
}
