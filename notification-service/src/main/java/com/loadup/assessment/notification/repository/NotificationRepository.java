package com.loadup.assessment.notification.repository;

import com.loadup.assessment.notification.domain.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findAllByOrderByCreatedAtDesc();
}
