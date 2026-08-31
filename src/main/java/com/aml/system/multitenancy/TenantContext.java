package com.aml.system.multitenancy;

/**
 * ThreadLocal-based context holder for Tenant Identification.
 *
 * ARCHITECTURAL NOTE ON MULTI-TENANCY AT 10M SCALE:
 * For multi-tenant banking compliance:
 * 1. Web/REST layer uses TenantContext populated via JWT / HTTP Headers ('X-Tenant-ID').
 * 2. Spring Batch processing isolates tenant processing per batch partition/job parameters.
 * 3. In multithreaded/async execution, TenantContext MUST be propagated using TaskDecorators
 *    or passed explicitly in Spring Batch StepExecutionContext to avoid context bleeding.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
