package com.loadup.assessment.notification.service;

import com.loadup.assessment.contracts.OrderEvent;
import com.loadup.assessment.notification.domain.NotificationEntity;

import java.util.List;

public interface NotificationService {
    List<NotificationEntity> list();

    void consume(final OrderEvent event);
}
