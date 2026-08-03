package com.order.inventory.service;

import com.order.inventory.entity.InventoryEntity;
import com.order.inventory.entity.InventoryItemEntity;
import com.order.inventory.model.OrderMessage;
import com.order.inventory.repository.InventoryItemRepository;
import com.order.inventory.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class InventoryCompensationConsumer {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    /**
     * Payment publishes here if it fails after Inventory already reserved
     * stock. We give the stock back and mark the reservation RELEASED.
     */
    @JmsListener(destination = "${app.mq.compensate-queue}", containerFactory = "jmsListenerContainerFactory")
    @Transactional
    public void compensate(OrderMessage order) {
        inventoryRepository.findByOrderId(order.getOrderId()).ifPresentOrElse(reservation -> {

            if (!"RESERVED".equals(reservation.getStatus())) {
                log.info("Reservation for orderId={} is already {}, nothing to release",
                        order.getOrderId(), reservation.getStatus());
                return;
            }

            inventoryItemRepository.findByProductIgnoreCase(reservation.getProduct())
                    .ifPresent(item -> {
                        item.setAvailableQuantity(item.getAvailableQuantity() + reservation.getQuantity());
                        inventoryItemRepository.save(item);
                    });

            reservation.setStatus("RELEASED");
            reservation.setReason("Released - " + order.getFailureReason());
            inventoryRepository.save(reservation);

            log.info("Inventory released for orderId={} (product={}, qty={})",
                    order.getOrderId(), reservation.getProduct(), reservation.getQuantity());

        }, () -> log.warn("Compensation received for unknown orderId={}", order.getOrderId()));
    }
}
