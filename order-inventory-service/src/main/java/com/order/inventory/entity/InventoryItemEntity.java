package com.order.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Product catalog / stock table. This is what "Product unavailable"
 * is checked against when an order comes in.
 */
@Entity
@Table(name = "inventory_items")
@Data
public class InventoryItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product", unique = true, nullable = false)
    private String product;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "price")
    private double price;
}
