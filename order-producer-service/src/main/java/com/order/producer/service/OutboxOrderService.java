package com.order.producer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.producer.entity.OrderEntity;
import com.order.producer.entity.OutboxEvent;
import com.order.producer.model.OrderMessage;
import com.order.producer.repository.OrderRepository;
import com.order.producer.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class OutboxOrderService {

    @Autowired
    private OutboxEventRepository outboxRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Value("${app.mq.output-queue}")
    private String orderQueue;

    // Single @Transactional — only DB write, NO MQ call here
    @Transactional
    public OrderMessage placeOrder(String product, int quantity, String priority) {
        try {

            OrderMessage order = new OrderMessage();
            order.setOrderId(UUID.randomUUID().toString());
            order.setCustomerName("Customer");
            order.setProduct(product);
            order.setQuantity(quantity);
            order.setTotalAmount(quantity * 99.99);
            order.setCreatedAt(LocalDateTime.now());
            order.setStatus("PENDING");
            order.setPriority(priority);

            OrderEntity orderEntity = new OrderEntity();
            orderEntity.setOrderId(order.getOrderId());
            orderEntity.setProduct(order.getProduct());
            orderEntity.setQuantity(order.getQuantity());
            orderEntity.setStatus("PENDING");

            orderRepository.save(orderEntity);

            OutboxEvent event = new OutboxEvent();
            event.setOrderId(order.getOrderId());
            event.setTopic(orderQueue);
            event.setPayload(objectMapper.writeValueAsString(order));
            event.setStatus("PENDING");

            outboxRepo.save(event);

            log.info("Order saved to outbox: {}", order.getOrderId());
            return order;
        }

        catch (Exception e) {
            throw new RuntimeException("Failed to save order to outbox", e);
        }
    }
}
