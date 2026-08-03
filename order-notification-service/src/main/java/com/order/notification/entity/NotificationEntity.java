package com.order.notification.entity;
import jakarta.persistence.*;
import lombok.Data; import java.time.LocalDateTime;
@Entity @Table(name = "notifications") @Data
public class NotificationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_id") private String orderId;
    @Column(name = "message") private String message;
    @Column(name = "status") private String status = "SENT";
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
}
