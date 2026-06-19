package com.loadup.assessment.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loadup.assessment.order.config.TenantDatabaseProperties;
import com.loadup.assessment.contracts.OrderEvent;
import com.loadup.assessment.order.domain.OrderOutboxEntity;
import com.loadup.assessment.order.repository.OrderOutboxRepository;
import com.loadup.assessment.order.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OrderOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OrderOutboxPublisher.class);
    private final OrderOutboxRepository orderOutboxRepository;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TenantDatabaseProperties tenantDatabaseProperties;
    private final TransactionTemplate transactionTemplate;

    public OrderOutboxPublisher(final OrderOutboxRepository orderOutboxRepository,
                                final KafkaTemplate<String, OrderEvent> kafkaTemplate,
                                final ObjectMapper objectMapper,
                                final TenantDatabaseProperties tenantDatabaseProperties,
                                final PlatformTransactionManager transactionManager) {
        this.orderOutboxRepository = orderOutboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.tenantDatabaseProperties = tenantDatabaseProperties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox-publish-delay-ms:500}")
    public void publish() {
        for (String tenantId : tenantDatabaseProperties.getTenants().keySet()) {
            TenantContext.setTenantId(tenantId);
            try {
                log.debug("Starting outbox publish cycle tenantId={}", tenantId);
                transactionTemplate.executeWithoutResult(status -> publishTenantOutbox());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void publishTenantOutbox() {
        List<OrderOutboxEntity> orderOutboxEntities = orderOutboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        if (orderOutboxEntities.isEmpty()) {
            log.debug("No unpublished outbox rows tenantId={}", TenantContext.requireTenantId());
            return;
        }
        for (OrderOutboxEntity orderOutboxEntity : orderOutboxEntities) {
            try {
                OrderEvent event = objectMapper.readValue(orderOutboxEntity.getPayload(), OrderEvent.class);
                kafkaTemplate.send(orderOutboxEntity.getTopic(), orderOutboxEntity.getEventKey(), event).get(10, TimeUnit.SECONDS);
                orderOutboxEntity.markPublished();
                orderOutboxRepository.save(orderOutboxEntity);
                log.info("Outbox published tenantId={} outboxId={} eventType={} aggregateId={}",
                        TenantContext.requireTenantId(), orderOutboxEntity.getId(), orderOutboxEntity.getEventType(), orderOutboxEntity.getAggregateId());
            } catch (Exception ex) {
                orderOutboxEntity.markFailed(safeMessage(ex));
                orderOutboxRepository.save(orderOutboxEntity);
                log.warn("Outbox publish failed tenantId={} outboxId={} eventType={} error={}",
                        TenantContext.requireTenantId(), orderOutboxEntity.getId(), orderOutboxEntity.getEventType(), safeMessage(ex));
            }
        }
    }

    private String safeMessage(final Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.substring(0, Math.min(message.length(), 1000));
    }
}
