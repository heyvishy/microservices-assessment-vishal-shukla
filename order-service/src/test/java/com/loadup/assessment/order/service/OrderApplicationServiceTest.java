package com.loadup.assessment.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loadup.assessment.order.config.DataSourceConfig;
import com.loadup.assessment.order.dto.OrderCreateRequest;
import com.loadup.assessment.order.dto.OrderUpdateRequest;
import com.loadup.assessment.order.constants.OrderStatus;
import com.loadup.assessment.order.exception.OrderConflictException;
import com.loadup.assessment.order.repository.OrderOutboxRepository;
import com.loadup.assessment.order.repository.OrderRepository;
import com.loadup.assessment.order.service.impl.OrderApplicationServiceImpl;
import com.loadup.assessment.order.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DataSourceConfig.class, OrderApplicationServiceImpl.class, OrderApplicationServiceTest.TestConfig.class})
@org.springframework.test.context.TestPropertySource(properties = {
        "app.tenants.tenant-a.url=jdbc:h2:mem:order-tenant-a;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.tenants.tenant-a.username=sa",
        "app.tenants.tenant-a.password=",
        "app.tenants.tenant-a.driver-class-name=org.h2.Driver",
        "app.tenants.tenant-b.url=jdbc:h2:mem:order-tenant-b;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.tenants.tenant-b.username=sa",
        "app.tenants.tenant-b.password=",
        "app.tenants.tenant-b.driver-class-name=org.h2.Driver"
})
@org.springframework.transaction.annotation.Transactional(propagation = Propagation.NOT_SUPPORTED)
class OrderApplicationServiceTest {
    @Autowired
    private OrderApplicationService service;
    @Autowired
    private OrderOutboxRepository outboxRepository;
    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void tearDown() {
        TenantContext.setTenantId("tenant-a");
        orderRepository.deleteAll();
        outboxRepository.deleteAll();
        TenantContext.setTenantId("tenant-b");
        orderRepository.deleteAll();
        outboxRepository.deleteAll();
        TenantContext.clear();
    }

    @Test
    void createWritesOrderAndOutbox() {
        TenantContext.setTenantId("tenant-a");
        var order = service.create(new OrderCreateRequest("cust-1", "cust@example.com", "Shoes", BigDecimal.valueOf(42.50), "usd"));
        assertNotNull(order.getCreatedAt());
        assertNotNull(order.getUpdatedAt());
        assertEquals(order.getCreatedAt(), order.getUpdatedAt());
        assertEquals(1, outboxRepository.count());
        TenantContext.clear();
        TenantContext.setTenantId("tenant-b");
        assertEquals(0, orderRepository.count());
    }

    @Test
    void routesOrdersPerTenantDatabase() {
        TenantContext.setTenantId("tenant-a");
        var tenantAOrder = service.create(new OrderCreateRequest("cust-1", "cust@example.com", "Shoes", BigDecimal.valueOf(42.50), "usd"));
        assertEquals(1, orderRepository.count());

        TenantContext.setTenantId("tenant-b");
        assertEquals(0, orderRepository.count());
        var tenantBOrder = service.create(new OrderCreateRequest("cust-2", "cust2@example.com", "Hat", BigDecimal.valueOf(19.99), "usd"));
        assertEquals(1, orderRepository.count());

        TenantContext.setTenantId("tenant-a");
        assertEquals(tenantAOrder.getId(), service.get(tenantAOrder.getId()).getId());
        assertEquals(1, orderRepository.count());

        TenantContext.setTenantId("tenant-b");
        assertEquals(tenantBOrder.getId(), service.get(tenantBOrder.getId()).getId());
        assertEquals(1, orderRepository.count());
    }

    @Test
    void updateRejectsTerminalOrders() {
        TenantContext.setTenantId("tenant-a");
        var created = service.create(new OrderCreateRequest("cust-1", "cust@example.com", "Shoes", BigDecimal.valueOf(42.50), "usd"));
        service.update(created.getId(), new OrderUpdateRequest(OrderStatus.COMPLETED, "done"));
        assertThrows(OrderConflictException.class,
                () -> service.update(created.getId(), new OrderUpdateRequest(OrderStatus.PROCESSING, "later")));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}
