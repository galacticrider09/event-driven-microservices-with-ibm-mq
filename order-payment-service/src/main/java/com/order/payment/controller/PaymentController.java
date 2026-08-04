package com.order.payment.controller;

import com.order.payment.entity.PaymentEntity;
import com.order.payment.repository.PaymentRepository;
import com.order.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    // Manual/direct testing endpoint - the automated flow arrives via MQ (PaymentConsumer)
    @PostMapping("/process")
    public Map<String, Object> process(@RequestBody Map<String, Object> req) {
        String orderId = (String) req.get("orderId");
        double amount = Double.parseDouble(req.get("amount").toString());
        String product = (String) req.getOrDefault("product", "Unknown");

        String result = paymentService.processPayment(orderId, amount, product);
        return Map.of("status", "SUCCESS", "message", result, "orderId", orderId);
    }

    @GetMapping("/records")
    public List<PaymentEntity> getAll() {
        return paymentRepository.findAll();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("service", "order-payment-service", "status", "UP", "port", 8082);
    }
}
