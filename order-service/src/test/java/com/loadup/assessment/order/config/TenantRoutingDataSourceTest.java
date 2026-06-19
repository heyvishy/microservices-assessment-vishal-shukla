package com.loadup.assessment.order.config;

import com.loadup.assessment.order.exception.MissingTenantContextException;
import com.loadup.assessment.order.exception.UnknownTenantException;
import com.loadup.assessment.order.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantRoutingDataSourceTest {
    private final TestTenantRoutingDataSource dataSource = new TestTenantRoutingDataSource("tenant-a", Set.of("tenant-a", "tenant-b"));

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void failsClosedWhenTenantContextIsMissing() {
        assertThrows(MissingTenantContextException.class, dataSource::lookupKey);
    }

    @Test
    void resolvesKnownTenant() {
        TenantContext.setTenantId("tenant-b");
        assertEquals("tenant-b", dataSource.lookupKey());
    }

    @Test
    void rejectsUnknownTenant() {
        TenantContext.setTenantId("tenant-c");
        assertThrows(UnknownTenantException.class, dataSource::lookupKey);
    }

    private static final class TestTenantRoutingDataSource extends TenantRoutingDataSource {
        private TestTenantRoutingDataSource(final String defaultTenantId, final Set<String> knownTenantIds) {
            super(defaultTenantId, knownTenantIds);
        }

        private Object lookupKey() {
            return determineCurrentLookupKey();
        }
    }
}
