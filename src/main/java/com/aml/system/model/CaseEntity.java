package com.aml.system.model;

import com.aml.system.model.enums.AlertSeverity;
import com.aml.system.model.enums.CaseStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Compliance Case Entity for Operational Investigation & SAR Filings.
 *
 * ARCHITECTURAL NOTE (JPA USAGE):
 * JPA / Hibernate is optimal here because Case Management is an OLTP domain with:
 * - Low write frequency (hundreds or thousands of cases/day vs. 10M transactions/batch)
 * - Complex state machines, audit trails, and relational UI lookups
 * - Fine-grained optimistic locking (@Version) for concurrent compliance analyst access
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "aml_cases", indexes = {
        @Index(name = "idx_case_tenant_status", columnList = "tenantId, status"),
        @Index(name = "idx_case_assignee", columnList = "tenantId, assignedAnalystId"),
        @Index(name = "idx_case_customer", columnList = "tenantId, customerId")
})
public class CaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID caseId;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 128)
    private String customerId;

    @Column(nullable = false, length = 255)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private CaseStatus status = CaseStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AlertSeverity riskLevel;

    @Column(length = 128)
    private String assignedAnalystId;

    @Column(length = 255)
    private String primaryRuleTriggered;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalSuspiciousAmount;

    @Column(columnDefinition = "TEXT")
    private String investigationNotes;

    @Column(length = 128)
    private String sarFilingReference;

    @Version
    private Long version;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
