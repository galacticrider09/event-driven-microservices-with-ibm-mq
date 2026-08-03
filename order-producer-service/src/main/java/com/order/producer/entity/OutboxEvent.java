package com.order.producer.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
@Entity @Table(name = "outbox_events") @Data
public class OutboxEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_id") private String orderId;
    @Column(name = "topic") private String topic = "DEV.ORDER.QUEUE";
    @Column(name = "payload", columnDefinition = "TEXT") private String payload;
    @Column(name = "status") private String status = "PENDING";
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "sent_at") private LocalDateTime sentAt;
}
