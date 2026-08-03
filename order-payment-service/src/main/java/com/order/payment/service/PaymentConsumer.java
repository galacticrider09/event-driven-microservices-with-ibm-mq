package com.order.payment.service;

import com.order.payment.model.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentConsumer {

    @Autowired
    private PaymentService paymentService;

    @JmsListener(destination = "${app.mq.input-queue}", containerFactory = "jmsListenerContainerFactory")
    public void receiveConfirmedOrder(OrderMessage order) {
        log.info("PAYMENT received: orderId={}, product={}, amount={}",
                order.getOrderId(), order.getProduct(), order.getTotalAmount());

        paymentService.processPayment(order.getOrderId(), order.getTotalAmount(), order.getProduct(), order);
    }
}
