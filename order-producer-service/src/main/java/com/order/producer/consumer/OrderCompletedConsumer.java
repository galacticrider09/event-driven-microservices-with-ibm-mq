package com.order.producer.consumer;

import com.order.producer.model.OrderMessage;
import com.order.producer.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderCompletedConsumer {

    @Autowired
    private OrderRepository orderRepository;

    // Notification sends here once it has sent the final notification -
    // this closes the loop: Producer -> Inventory -> Payment -> Notification -> Producer
    @JmsListener(destination = "${app.mq.completed-queue}", containerFactory = "jmsListenerContainerFactory")
    public void handleCompleted(OrderMessage order) {

        log.info("Order {} completed end-to-end", order.getOrderId());

        orderRepository.findById(order.getOrderId())
                .ifPresent(o -> {
                    o.setStatus("COMPLETED");
                    orderRepository.save(o);
                });
    }
}
