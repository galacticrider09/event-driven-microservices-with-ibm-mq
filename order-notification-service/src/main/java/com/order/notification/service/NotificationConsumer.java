package com.order.notification.service;

import com.order.notification.model.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationConsumer {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${app.mq.completed-queue}")
    private String completedQueue;

    @JmsListener(destination = "${app.mq.input-queue}", containerFactory = "jmsListenerContainerFactory")
    public void receivePaymentConfirmation(OrderMessage order) {
        log.info("NOTIFICATION received: orderId={}, status={}", order.getOrderId(), order.getStatus());

        notificationService.sendNotification(order.getOrderId(), order.getStatus(), order.getTotalAmount());

        order.setStatus("COMPLETED");
        jmsTemplate.convertAndSend(completedQueue, order);

        log.info("Notification forwarded orderId={} to {} (order fully completed)",
                order.getOrderId(), completedQueue);
    }
}
