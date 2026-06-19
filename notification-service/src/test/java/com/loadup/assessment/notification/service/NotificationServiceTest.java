package com.loadup.assessment.notification.service;

import com.loadup.assessment.contracts.OrderEvent;
import com.loadup.assessment.contracts.OrderEventType;
import com.loadup.assessment.contracts.OrderStatus;
import com.loadup.assessment.notification.config.DataSourceConfig;
import com.loadup.assessment.notification.repository.NotificationRepository;
import com.loadup.assessment.notification.repository.ProcessedEventRepository;
import com.loadup.assessment.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DataSourceConfig.class, NotificationServiceImpl.class})
@TestPropertySource(properties = {
        "app.tenants.tenant-a.url=jdbc:h2:mem:notification-tenant-a;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.tenants.tenant-a.username=sa",
        "app.tenants.tenant-a.password=",
        "app.tenants.tenant-a.driver-class-name=org.h2.Driver",
        "app.tenants.tenant-b.url=jdbc:h2:mem:notification-tenant-b;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.tenants.tenant-b.username=sa",
        "app.tenants.tenant-b.password=",
        "app.tenants.tenant-b.driver-class-name=org.h2.Driver"
})
@org.springframework.transaction.annotation.Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationServiceTest {
    @Autowired
    private NotificationService service;
    @Autowired
    private NotificationRepository notifications;
    @Autowired
    private ProcessedEventRepository processedEvents;

    @AfterEach
    void tearDown() {
        com.loadup.assessment.notification.tenant.TenantContext.setTenantId("tenant-a");
        notifications.deleteAll();
        processedEvents.deleteAll();
        com.loadup.assessment.notification.tenant.TenantContext.setTenantId("tenant-b");
        notifications.deleteAll();
        processedEvents.deleteAll();
        com.loadup.assessment.notification.tenant.TenantContext.clear();
    }

    @Test
    void consumeIsIdempotent() {
        var event = new OrderEvent(UUID.randomUUID(), OrderEventType.ORDER_CREATED.name(), UUID.randomUUID(), "tenant-a",
                "cust-1", "cust@example.com", OrderStatus.CONFIRMED, BigDecimal.TEN, "USD", Instant.now(), false);
        service.consume(event);
        service.consume(event);
        com.loadup.assessment.notification.tenant.TenantContext.setTenantId("tenant-a");
        assertEquals(1, notifications.count());
        assertEquals(1, processedEvents.count());
    }

    @Test
    void routesNotificationsPerTenantDatabase() {
        var tenantAEvent = new OrderEvent(UUID.randomUUID(), OrderEventType.ORDER_CREATED.name(), UUID.randomUUID(), "tenant-a",
                "cust-1", "cust@example.com", OrderStatus.CONFIRMED, BigDecimal.TEN, "USD", Instant.now(), false);
        var tenantBEvent = new OrderEvent(UUID.randomUUID(), OrderEventType.ORDER_CREATED.name(), UUID.randomUUID(), "tenant-b",
                "cust-2", "cust2@example.com", OrderStatus.CONFIRMED, BigDecimal.TEN, "USD", Instant.now(), false);

        service.consume(tenantAEvent);
        service.consume(tenantBEvent);

        com.loadup.assessment.notification.tenant.TenantContext.setTenantId("tenant-a");
        assertEquals(1, notifications.count());
        assertEquals(1, processedEvents.count());

        com.loadup.assessment.notification.tenant.TenantContext.setTenantId("tenant-b");
        assertEquals(1, notifications.count());
        assertEquals(1, processedEvents.count());
    }
}
