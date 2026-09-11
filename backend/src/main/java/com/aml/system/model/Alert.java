package com.aml.system.model;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "aml_alerts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {



        @Id
        @Column(name = "alert_id")
        private UUID alertId;

        @Column(name = "transaction_id", nullable = false)
        private UUID transactionId;

        @Column(name = "customer_id", nullable = false)
        private String customerId;

        @Column(name = "rule_code", nullable = false)
        private String ruleCode;

        @Column(name = "rule_name", nullable = false)
        private String ruleName;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private AlertSeverity severity;

        @Column(name = "triggered_amount", nullable = false)
        private BigDecimal triggeredAmount;

        @Column(columnDefinition = "TEXT")
        private String narrative;

        @Column(name = "detection_metadata_json", columnDefinition = "TEXT")
        private String detectionMetadataJson;

        @Builder.Default
        @Column(name = "is_reviewed", nullable = false)
        private boolean reviewed = false;

        @Column(name = "assigned_case_id")
        private UUID assignedCaseId;

        @Column(name = "batch_id")
        private UUID batchId;

        @Column(name = "tenant_id")
        private String tenantId;

        // The compliance officer assigned to review this alert (set by Bank Admin)
        @Column(name = "assigned_officer_id")
        private UUID assignedOfficerId;

        @Column(name = "reviewed_at")
        private LocalDateTime reviewedAt;

        @Column(name = "reviewed_by", length = 128)
        private String reviewedBy;

        @Column(name = "review_decision", length = 64)
        private String reviewDecision;

        @Column(name = "review_notes", columnDefinition = "TEXT")
        private String reviewNotes;

        @Column(name = "created_at")
        private LocalDateTime createdAt;

}
