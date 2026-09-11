package com.aml.system.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@ToString
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "aml_system_audit_logs")
public class SystemAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id")
    private UUID logId;

    @Column(name = "user_id", length = 128)
    private String userId;

    @Column(name = "action_type", nullable = false, length = 64)
    private String actionType;

    @Column(name = "affected_record_id", length = 128)
    private String affectedRecordId;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /**
     * DB-level timestamp. Uses Instant for timezone-safe compliance auditing.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemAuditLog that = (SystemAuditLog) o;
        return logId != null && Objects.equals(logId, that.logId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}