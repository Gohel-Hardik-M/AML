package com.aml.system.service;

import com.aml.system.dto.admin.AlertAssignmentDto;
import com.aml.system.exception.AmlBusinessException;
import com.aml.system.model.Alert;
import com.aml.system.model.UserEntity;
import com.aml.system.model.UserRole;
import com.aml.system.repository.AlertRepository;
import com.aml.system.repository.BatchRepository;
import com.aml.system.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertAssignmentServiceTest {
    private static final String TENANT = "BANK_A";
    private final UUID officerId = UUID.randomUUID();
    private final UUID alertId = UUID.randomUUID();

    @Mock AlertRepository alertRepository;
    @Mock UserRepository userRepository;
    @Mock BatchRepository batchRepository;
    @Mock AlertPdfService alertPdfService;

    @AfterEach
    void clearTenant() {
        com.aml.system.multitenancy.TenantContextHolder.clear();
    }

    @Test
    void assignsUnassignedOpenAlertToActiveComplianceOfficer() {
        com.aml.system.multitenancy.TenantContextHolder.setTenantId(TENANT);
        UserEntity officer = officer(UserRole.COMPLIANCE_OFFICER, true);
        Alert alert = Alert.builder().alertId(alertId).reviewed(false).assignedOfficerId(null).build();
        when(userRepository.findByTenantIdAndUserId(TENANT, officerId)).thenReturn(Optional.of(officer));
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = service().assignAlerts(dto(alertId));

        assertTrue(result.contains("Successfully assigned 1"));
        assertEquals(officerId, alert.getAssignedOfficerId());
    }

    @Test
    void rejectsAlreadyAssignedAlertWithConflictAndDoesNotOverwriteOwner() {
        com.aml.system.multitenancy.TenantContextHolder.setTenantId(TENANT);
        UserEntity officer = officer(UserRole.COMPLIANCE_OFFICER, true);
        UUID existingOfficer = UUID.randomUUID();
        Alert alert = Alert.builder().alertId(alertId).reviewed(false).assignedOfficerId(existingOfficer).build();
        when(userRepository.findByTenantIdAndUserId(TENANT, officerId)).thenReturn(Optional.of(officer));
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));

        AmlBusinessException error = assertThrows(AmlBusinessException.class, () -> service().assignAlerts(dto(alertId)));

        assertEquals(409, error.getStatus().value());
        assertTrue(error.getMessage().contains("already assigned"));
        assertEquals(existingOfficer, alert.getAssignedOfficerId());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void rejectsClosedAlertInactiveOfficerAndWrongRole() {
        com.aml.system.multitenancy.TenantContextHolder.setTenantId(TENANT);
        UserEntity inactive = officer(UserRole.COMPLIANCE_OFFICER, false);
        when(userRepository.findByTenantIdAndUserId(TENANT, officerId)).thenReturn(Optional.of(inactive));
        AmlBusinessException inactiveError = assertThrows(AmlBusinessException.class, () -> service().assignAlerts(dto(alertId)));
        assertEquals(400, inactiveError.getStatus().value());

        UserEntity wrongRole = officer(UserRole.TENANT_ADMIN, true);
        when(userRepository.findByTenantIdAndUserId(TENANT, officerId)).thenReturn(Optional.of(wrongRole));
        AmlBusinessException roleError = assertThrows(AmlBusinessException.class, () -> service().assignAlerts(dto(alertId)));
        assertEquals(400, roleError.getStatus().value());

        UserEntity active = officer(UserRole.COMPLIANCE_OFFICER, true);
        Alert closed = Alert.builder().alertId(alertId).reviewed(true).build();
        when(userRepository.findByTenantIdAndUserId(TENANT, officerId)).thenReturn(Optional.of(active));
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(closed));
        AmlBusinessException closedError = assertThrows(AmlBusinessException.class, () -> service().assignAlerts(dto(alertId)));
        assertEquals(409, closedError.getStatus().value());
    }

    @Test
    void rejectsEmptyUnassignRequest() {
        AmlBusinessException error = assertThrows(AmlBusinessException.class, () -> service().unassignAlerts(List.of()));
        assertEquals(400, error.getStatus().value());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void leavesAlertOpenWhenPdfGenerationFails() throws Exception {
        com.aml.system.multitenancy.TenantContextHolder.setTenantId(TENANT);
        UserEntity officer = officer(UserRole.COMPLIANCE_OFFICER, true);
        Alert alert = Alert.builder().alertId(alertId).reviewed(false).assignedOfficerId(officerId).build();
        when(userRepository.findByTenantIdAndUsername(TENANT, "officer")).thenReturn(Optional.of(officer));
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(alertPdfService.generate(any(Alert.class))).thenThrow(new java.io.IOException("pdf failed"));

        assertThrows(java.io.IOException.class,
                () -> service().closeMyAlertAndGeneratePdf(alertId, "officer", "Reviewed details"));

        assertFalse(alert.isReviewed());
        verify(alertRepository, never()).save(any());
    }

    private AlertAssignmentService service() {
        return new AlertAssignmentService(alertRepository, userRepository, batchRepository, alertPdfService);
    }

    private AlertAssignmentDto dto(UUID id) {
        AlertAssignmentDto dto = new AlertAssignmentDto();
        dto.setOfficerId(officerId);
        dto.setAlertIds(List.of(id));
        return dto;
    }

    private UserEntity officer(UserRole role, boolean active) {
        UserEntity user = new UserEntity();
        user.setUserId(officerId);
        user.setUsername("officer");
        user.setFullName("Test Officer");
        user.setRole(role);
        user.setIsActive(active);
        return user;
    }
}
