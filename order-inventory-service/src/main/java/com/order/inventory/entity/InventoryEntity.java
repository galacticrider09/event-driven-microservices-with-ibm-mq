package com.order.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * One row per order that passed through Inventory - whether it was
 * successfully reserved, released again (compensation), or rejected
 * outright because the product wasn't available.
 */
@Entity
@Table(name = "inventory_reservations")
@Data
public class InventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "product")
    private String product;

    @Column(name = "quantity")
    private int quantity;

    // RESERVED, RELEASED, UNAVAILABLE
    @Column(name = "status")
    private String status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
