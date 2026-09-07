package com.aml.system.model;

/**
 * Enumerated user roles for the AML platform.
 * Stored as String in the database via @Enumerated(EnumType.STRING).
 * Spring Security expects the "ROLE_" prefix, which is added at runtime by JwtAuthenticationFilter.
 */
public enum UserRole {
    SYSTEM_ADMIN,
    TENANT_ADMIN,
    COMPLIANCE_OFFICER,
    COMPLIANCE_ANALYST;

    /**
     * Returns the Spring Security compatible role name.
     */
    public String toSpringRole() {
        return "ROLE_" + this.name();
    }
}
