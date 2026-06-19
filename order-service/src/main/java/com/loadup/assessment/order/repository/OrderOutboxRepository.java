package com.loadup.assessment.order.repository;

import com.loadup.assessment.order.domain.OrderOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderOutboxRepository extends JpaRepository<OrderOutboxEntity, UUID> {
    List<OrderOutboxEntity> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
