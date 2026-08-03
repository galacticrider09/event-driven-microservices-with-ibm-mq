package com.order.notification.repository;
import com.order.notification.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {}
