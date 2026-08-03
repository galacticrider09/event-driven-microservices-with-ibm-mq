package com.order.payment.entity;
import jakarta.persistence.*;
import lombok.Data; import java.time.LocalDateTime;
@Entity
@Table(name = "payments")
@Data
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id",unique = true)
    private String orderId;

    @Column(name = "amount")
    private double amount;

    @Column(name = "product")
    private String product;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
