package com.order.producer.service;
import com.order.producer.model.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;


@Service
@Slf4j
public class OrderProducer {
    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${app.mq.output-queue}")
    private String orderQueue;

    @CircuitBreaker(name = "mqProducerService", fallbackMethod = "sendOrderFallback")
    @Retry(name = "mqProducerRetry")
    public void sendOrder(OrderMessage order) {
        log.info("Sending to MQ: {}", order.getOrderId());
        jmsTemplate.convertAndSend(orderQueue, order);
        log.info("Sent to MQ: {}", order.getOrderId());
    }

    public void sendOrderFallback(OrderMessage order, Throwable t) {
        log.error("MQ unavailable for orderId={}. Reason={}", order.getOrderId(), t.getMessage());
        throw new RuntimeException("MQ unavailable", t); // OutboxRelayService already catches this and marks event FAILED
    }
}
