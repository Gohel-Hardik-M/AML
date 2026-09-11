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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "aml_users", indexes = {
        @Index(name = "idx_user_tenant_username", columnList = "tenant_id, username", unique = true)
})
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID userId;

    /**
     * Optimistic locking version to prevent lost updates (e.g., concurrent login attempts).
     */
    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "email", unique = true, length = 128)
    private String email;

    @Column(name = "username", nullable = false, length = 128)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 128)
    private String fullName;

    /**
     * Stored as String in DB (e.g., "TENANT_ADMIN"). Enum prevents invalid role values.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 64)
    private UserRole role;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * DB-level timestamp via @CreationTimestamp. Uses Instant for timezone-safe compliance auditing.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    /**
     * When set, the account auto-unlocks after this time passes.
     * Null means the account is not on a timed lockout.
     */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Builder.Default
    @Column(name = "is_temporary_password", nullable = false)
    private Boolean isTemporaryPassword = true;

    @Builder.Default
    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts = 0;

    /**
     * Entity equality based on primary key only.
     * Prevents Hibernate issues with @Data-generated equals/hashCode that use all fields.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity that = (UserEntity) o;
        return userId != null && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}