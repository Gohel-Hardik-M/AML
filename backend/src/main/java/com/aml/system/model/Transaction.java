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



    @Id
    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;


    @Column(nullable = false)
    private String sourceAccountId;

    @Column(nullable = false)
    private String destinationAccountId;


    @Column(name = "customer_id", nullable = false)
    private String customerID;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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
