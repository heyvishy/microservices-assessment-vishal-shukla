package com.loadup.assessment.notification.tenant;

import com.loadup.assessment.notification.exception.MissingTenantContextException;

public final class TenantContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(final String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT.get();
    }

    public static String requireTenantId() {
        String tenantId = CURRENT.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new MissingTenantContextException("Missing tenant context");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
