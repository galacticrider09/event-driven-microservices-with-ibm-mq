package com.order.producer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Message that travels across every hop of the pipeline:
 * Producer -> Inventory -> Payment -> Notification
 * (and back again on the failed / completed status queues).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage {
    private String orderId;
    private String customerName;
    private String product;
    private int quantity;
    private double totalAmount;
    private LocalDateTime createdAt;
    private String status;
    private String priority;
    private String failureReason;
}
