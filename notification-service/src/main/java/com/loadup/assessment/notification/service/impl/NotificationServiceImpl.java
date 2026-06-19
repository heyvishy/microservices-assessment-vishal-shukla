package com.loadup.assessment.notification.service.impl;

import com.loadup.assessment.contracts.OrderEvent;
import com.loadup.assessment.contracts.OrderEventType;
import com.loadup.assessment.contracts.OrderStatus;
import com.loadup.assessment.notification.domain.NotificationEntity;
import com.loadup.assessment.notification.domain.ProcessedEventEntity;
import com.loadup.assessment.notification.repository.NotificationRepository;
import com.loadup.assessment.notification.repository.ProcessedEventRepository;
import com.loadup.assessment.notification.service.NotificationService;
import com.loadup.assessment.notification.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final TransactionTemplate transactionTemplate;

    public NotificationServiceImpl(final NotificationRepository notificationRepository,
                                   final ProcessedEventRepository processedEventRepository,
                                   final PlatformTransactionManager transactionManager) {
        this.notificationRepository = notificationRepository;
        this.processedEventRepository = processedEventRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationEntity> list() {
        TenantContext.requireTenantId();
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @KafkaListener(topics = "${app.kafka.order-topic:orders.events.v1}", groupId = "notification-service")
    public void consume(final OrderEvent event) {
        TenantContext.setTenantId(event.tenantId());
        MDC.put("tenantId", event.tenantId());
        try {
            log.debug("Consuming order event tenantId={} eventId={} eventType={} orderId={}",
                    event.tenantId(), event.eventId(), event.eventType(), event.orderId());
            transactionTemplate.executeWithoutResult(status -> consumeInTransaction(event));
        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
        }
    }

    private void consumeInTransaction(final OrderEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.debug("Duplicate event ignored tenantId={} eventId={}", TenantContext.requireTenantId(), event.eventId());
            return;
        }
        String message = buildMessage(event);
        if (message == null) {
            processedEventRepository.save(ProcessedEventEntity.of(event.eventId()));
            log.info("Event recorded without notification tenantId={} eventId={} eventType={}",
                    TenantContext.requireTenantId(), event.eventId(), event.eventType());
            return;
        }
        notificationRepository.save(NotificationEntity.create(
                event.orderId(),
                event.eventType(),
                "SIMULATED_EMAIL",
                event.customerEmail(),
                message));
        processedEventRepository.save(ProcessedEventEntity.of(event.eventId()));
        log.info("Notification created tenantId={} eventId={} eventType={} orderId={}",
                TenantContext.requireTenantId(), event.eventId(), event.eventType(), event.orderId());
    }

    private String buildMessage(final OrderEvent event) {
        boolean terminal = event.terminal() || event.status() == OrderStatus.COMPLETED || event.status() == OrderStatus.CANCELED;
        if (event.eventType().equals(OrderEventType.ORDER_CREATED.name()) || event.eventType().equals(OrderEventType.ORDER_UPDATED.name())) {
            return "Order " + event.orderId() + " for " + event.customerEmail() + " is now " + event.status();
        }
        if (terminal) {
            return "Order " + event.orderId() + " reached terminal state " + event.status();
        }
        return null;
    }
}
