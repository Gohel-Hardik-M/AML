package com.aml.system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "aml_batches", indexes = {
        @Index(name = "idx_batch_tenant_date", columnList = "tenant_id, batch_date", unique = true),
        @Index(name = "idx_batch_tenant_checksum", columnList = "tenant_id, file_checksum", unique = true)
})
public class BatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "batch_id", updatable = false, nullable = false)
    private UUID batchId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "batch_date", nullable = false)
    private LocalDate batchDate;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_checksum", nullable = false, length = 64)
    private String fileChecksum;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 32)
    private String status = "PENDING";

    @Builder.Default
    @Column(name = "total_records", nullable = false)
    private Integer totalRecords = 0;

    @Builder.Default
    @Column(name = "valid_records", nullable = false)
    private Integer validRecords = 0;

    @Builder.Default
    @Column(name = "invalid_records", nullable = false)
    private Integer invalidRecords = 0;

    @Builder.Default
    @Column(name = "alerts_generated", nullable = false)
    private Integer alertsGenerated = 0;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Builder.Default
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}