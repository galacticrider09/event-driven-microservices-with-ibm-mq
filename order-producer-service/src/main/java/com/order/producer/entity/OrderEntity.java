package com.order.producer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name="orders")
public class OrderEntity {

    @Id
    private String orderId;

    private String product;
    private Integer quantity;
    private String status; // PENDING, CONFIRMED (INVENTORY_CONFIRMED/PAYMENT_SUCCESS), COMPLETED, CANCELLED
    private String failureReason;
}