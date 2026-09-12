package com.aml.system.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;



@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "batches", uniqueConstraints = {
    @UniqueConstraint(name = "uk_batches_tenant_date", columnNames = {"tenant_id", "batch_date"}),
    @UniqueConstraint(name = "uk_batches_tenant_checksum", columnNames = {"tenant_id", "file_checksum"})
})
public class Batch {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

            @Column(name = "tenant_id", nullable = false, length = 64)
            private String tenantId;

            @Column(name = "batch_date", nullable = false)
            private LocalDate batchDate;

            @Column(name = "file_checksum", nullable = false, length = 64)
            private String fileChecksum;



        @Column
        private String fileName;
        @Enumerated(EnumType.STRING)
        @Column
        private BatchStatus status;
        @Column
        private LocalDateTime uploadedAt;






    }
