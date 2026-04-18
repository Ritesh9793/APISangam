package com.apimarketplace.repository;

import com.apimarketplace.entity.NotificationEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, UUID>, JpaSpecificationExecutor<NotificationEvent> {
    List<NotificationEvent> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<NotificationEvent> findAllByOrderByCreatedAtDesc();
}
