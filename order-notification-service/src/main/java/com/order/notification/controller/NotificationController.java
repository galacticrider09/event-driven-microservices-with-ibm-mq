package com.order.notification.controller;
import com.order.notification.entity.NotificationEntity;
import com.order.notification.repository.NotificationRepository;
import com.order.notification.service.NotificationService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List; import java.util.Map;
@RestController
@RequestMapping("/api/notification")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j

public class NotificationController {
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @PostMapping("/send")
    public Map<String, Object> send(@RequestBody Map<String, Object> req) {
        String orderId = (String) req.get("orderId");
        String status = (String) req.getOrDefault("status", "UNKNOWN");
        double amount = Double.parseDouble(req.getOrDefault("amount", 0).toString());
        String result = notificationService.sendNotification(orderId, status, amount);
        return Map.of("status", "SENT", "message", result, "orderId", orderId);
    }

    @GetMapping("/records")
    public List<NotificationEntity> getAll() {
        return notificationRepository.findAll();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("service", "order-notification-service", "status", "UP", "port", 8083);
    }

    @GetMapping("/circuit-status")
    public Map<String, Object> circuitStatus() {

        var cb = circuitBreakerRegistry
                .circuitBreaker("notificationDb");

        return Map.of(
                "service", "notificationDb",
                "state", cb.getState().name()
        );
    }
}
