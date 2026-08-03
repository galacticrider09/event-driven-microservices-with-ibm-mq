package com.order.payment.service;

import com.order.payment.entity.PaymentEntity;
import com.order.payment.entity.PaymentEventEntity;
import com.order.payment.model.OrderMessage;
import com.order.payment.repository.PaymentEventRepository;
import com.order.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventRepository paymentEventRepository;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${app.mq.output-queue}")
    private String notificationQueue;

    @Value("${app.mq.failed-queue}")
    private String failedQueue;

    @Value("${app.mq.compensate-queue}")
    private String compensateQueue;

    public String processPayment(String orderId, double amount, String product) {
        return processPayment(orderId, amount, product, null);
    }

    /**
     * Processes payment for an order that Inventory already confirmed stock
     * for. On success it forwards the order to Notification over MQ. On any
     * failure it tells Inventory to release the stock it reserved AND tells
     * the Producer the order is cancelled.
     */
    @Transactional
    public String processPayment(String orderId, double amount, String product, OrderMessage inboundOrder) {
        log.info("Processing payment for orderId={}, amount={}", orderId, amount);

        if (paymentRepository.existsByOrderId(orderId)) {
            log.warn("Duplicate payment ignored for {}", orderId);
            return "Duplicate payment ignored";
        }

        try {
            PaymentEntity payment = new PaymentEntity();
            payment.setOrderId(orderId);
            payment.setAmount(amount);
            payment.setProduct(product);
            payment.setStatus("SUCCESS");
            paymentRepository.save(payment);

            PaymentEventEntity event = new PaymentEventEntity();
            event.setOrderId(orderId);
            event.setEventType("PAYMENT_SUCCESS");
            event.setCreatedAt(LocalDateTime.now());
            paymentEventRepository.save(event);

            log.info("Payment saved for orderId={}", orderId);

            if (inboundOrder != null) {
                inboundOrder.setStatus("PAYMENT_SUCCESS");
                jmsTemplate.convertAndSend(notificationQueue, inboundOrder);
                log.info("Payment forwarded order {} to {}", orderId, notificationQueue);
            }

            return "Payment processed for " + orderId;

        } catch (Exception e) {
            log.error("Payment failed for orderId={}: {}", orderId, e.getMessage());
            if (inboundOrder != null) {
                failPayment(inboundOrder, "Payment processing error: " + e.getMessage());
            }
            return "Payment failed for " + orderId;
        }
    }

    private void failPayment(OrderMessage order, String reason) {
        PaymentEventEntity event = new PaymentEventEntity();
        event.setOrderId(order.getOrderId());
        event.setEventType("PAYMENT_FAILED");
        event.setCreatedAt(LocalDateTime.now());
        paymentEventRepository.save(event);

        order.setStatus("CANCELLED");
        order.setFailureReason(reason);

        // Producer needs to know the order is cancelled...
        jmsTemplate.convertAndSend(failedQueue, order);
        // ...and Inventory needs to release the stock it reserved.
        jmsTemplate.convertAndSend(compensateQueue, order);

        log.warn("Order {} CANCELLED at payment stage - {}", order.getOrderId(), reason);
    }
}
