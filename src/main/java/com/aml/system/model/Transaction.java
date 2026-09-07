package com.aml.system.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {



    private UUID transactionId;


    private String tenantId;

    private String sourceAccountId;


    private String destinationAccountId;

    private String customerID;


    private BigDecimal amount;


    private String currency;

    private TransactionType transactionType;

    private String countryCode;

    private String counterpartyCountryCode;

    private String counterpartyName;

    private String channel;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

}
