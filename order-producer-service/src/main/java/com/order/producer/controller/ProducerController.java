package com.order.producer.controller;

import com.order.producer.entity.OrderEntity;
import com.order.producer.model.OrderMessage;
import com.order.producer.repository.OrderRepository;
import com.order.producer.service.OutboxOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class ProducerController {

    @Autowired
    private OutboxOrderService outboxOrderService;

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping("/send")
    public Map<String, Object> sendOrder(
            @RequestParam(defaultValue = "Laptop") String product,
            @RequestParam(defaultValue = "2") int quantity) {

        // Outbox pattern: saves to outbox_events table only, NO direct MQ call
        OrderMessage order = outboxOrderService.placeOrder(product, quantity, "MEDIUM");

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Order queued via Outbox pattern! Watch it flow through Inventory -> Payment -> Notification.");
        res.put("orderId", order.getOrderId());
        res.put("product", product);
        res.put("quantity", quantity);
        res.put("note", "Relay publishes to MQ within a few seconds; check GET /api/producer/status/{orderId}");
        return res;
    }

    @PostMapping("/send-bad")
    public Map<String, Object> sendBadOrder() {
        OrderMessage order = outboxOrderService.placeOrder("BadProduct", -1, "HIGH");
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Bad order queued (quantity=-1) - Inventory will reject it immediately");
        res.put("orderId", order.getOrderId());
        return res;
    }

    // Convenience endpoint to demo the "Product unavailable" path without
    // needing to know the exact catalog - orders something guaranteed to fail.
    @PostMapping("/send-unavailable")
    public Map<String, Object> sendUnavailableOrder() {
        OrderMessage order = outboxOrderService.placeOrder("Discontinued Printer", 1, "MEDIUM");
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Order queued for an out-of-stock product - Inventory will cancel it as 'Product unavailable'");
        res.put("orderId", order.getOrderId());
        return res;
    }

    @GetMapping("/status/{orderId}")
    public Map<String, Object> status(@PathVariable String orderId) {
        return orderRepository.findById(orderId)
                .<Map<String, Object>>map(o -> Map.of(
                        "orderId", o.getOrderId(),
                        "product", o.getProduct(),
                        "quantity", o.getQuantity(),
                        "status", o.getStatus(),
                        "failureReason", o.getFailureReason() == null ? "" : o.getFailureReason()))
                .orElse(Map.of("error", "Order not found"));
    }

    @GetMapping("/orders")
    public List<OrderEntity> allOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("service", "order-producer-service", "status", "UP", "port", 8080);
    }
}
