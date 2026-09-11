package com.aml.system.repository;

import com.aml.system.model.UserEntity;
import com.aml.system.model.UserRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Standard read query — no lock. Use for non-critical reads.
     */
    Optional<UserEntity> findByTenantIdAndUsername(String tenantId, String username);

    /**
     * Pessimistic write lock — prevents concurrent login attempts from causing
     * lost updates on the failed_attempts counter (audit finding #3).
     * Must be used inside a @Transactional method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId AND u.username = :username")
    Optional<UserEntity> findByTenantIdAndUsernameForUpdate(
            @Param("tenantId") String tenantId,
            @Param("username") String username
    );

    Page<UserEntity> findByTenantId(String tenantId, Pageable pageable);
    Page<UserEntity> findByTenantIdAndRole(String tenantId, UserRole role, Pageable pageable);
    java.util.List<UserEntity> findByTenantIdAndRole(String tenantId, UserRole role);
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByTenantIdAndUserId(String tenantId, UUID userId);
}
