package com.order.producer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.producer.entity.OutboxEvent;
import com.order.producer.model.OrderMessage;
import com.order.producer.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OutboxPoller {

    @Autowired
    private OutboxEventRepository outboxRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderProducer orderProducer;

    // Runs every 5 seconds — replaces Debezium's CDC trigger
    @Scheduled(fixedDelay = 5000)
    public void relayPendingEvents() {
        List<OutboxEvent> pending = outboxRepo.findByStatus("PENDING");
        for (OutboxEvent event : pending) {
            try {
                OrderMessage order = objectMapper.readValue(event.getPayload(), OrderMessage.class);
                orderProducer.sendOrder(order); // circuit breaker + retry still apply here

                event.setStatus("SENT");
                event.setSentAt(LocalDateTime.now());
                outboxRepo.save(event);

                log.info("Relayed order {} to MQ", order.getOrderId());
            } catch (Exception e) {
                log.error("Failed to relay outbox event id={}: {}", event.getId(), e.getMessage());
                // leave status=PENDING so it's retried on the next poll cycle
            }
        }
    }
}