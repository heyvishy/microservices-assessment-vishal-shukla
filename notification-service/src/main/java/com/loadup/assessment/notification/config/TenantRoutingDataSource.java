package com.loadup.assessment.notification.config;

import com.loadup.assessment.notification.exception.MissingTenantContextException;
import com.loadup.assessment.notification.exception.UnknownTenantException;
import com.loadup.assessment.notification.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.Set;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {
    private static final Logger log = LoggerFactory.getLogger(TenantRoutingDataSource.class);
    private final String defaultTenantId;
    private final Set<String> knownTenantIds;

    public TenantRoutingDataSource(final String defaultTenantId, final Set<String> knownTenantIds) {
        this.defaultTenantId = defaultTenantId;
        this.knownTenantIds = Set.copyOf(knownTenantIds);
        setLenientFallback(false);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new MissingTenantContextException("Missing tenant context");
        }
        if (!knownTenantIds.contains(tenantId)) {
            log.warn("Unknown tenant requested tenantId={}", tenantId);
            throw new UnknownTenantException("Unknown tenant id: " + tenantId);
        }
        return tenantId;
    }
}
