package com.order.producer.consumer;

import com.order.producer.model.OrderMessage;
import com.order.producer.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderFailureConsumer {

    @Autowired
    private OrderRepository orderRepository;

    // Inventory sends here for "Product unavailable"; Payment sends here if payment itself fails.
    @JmsListener(destination = "${app.mq.failed-queue}", containerFactory = "jmsListenerContainerFactory")
    public void handleFailure(OrderMessage order) {

        log.info("Received failure for order {}: {}", order.getOrderId(), order.getFailureReason());

        orderRepository.findById(order.getOrderId())
                .ifPresent(o -> {
                    o.setStatus("CANCELLED");
                    o.setFailureReason(order.getFailureReason());
                    orderRepository.save(o);

                    log.info("Order {} marked CANCELLED - {}",
                            order.getOrderId(), order.getFailureReason());
                });
    }
}
