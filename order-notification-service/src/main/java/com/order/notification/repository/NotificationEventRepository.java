package com.order.notification.repository;

import com.order.notification.entity.NotificationEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventRepository extends JpaRepository<NotificationEventEntity, Long> {
}
