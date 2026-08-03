package com.order.inventory.service;

import com.order.inventory.entity.InventoryEntity;
import com.order.inventory.entity.InventoryItemEntity;
import com.order.inventory.model.OrderMessage;
import com.order.inventory.repository.InventoryItemRepository;
import com.order.inventory.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class InventoryConsumer {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${app.mq.output-queue}")
    private String paymentQueue;

    @Value("${app.mq.failed-queue}")
    private String failedQueue;

    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);

    /**
     * Consumes every new order coming from the Producer service and decides,
     * based on real stock levels, whether it can move forward to Payment or
     * has to be cancelled as "Product unavailable".
     */
    @JmsListener(destination = "${app.mq.input-queue}", containerFactory = "jmsListenerContainerFactory")
    @Transactional
    public void receiveOrder(OrderMessage order) {
        log.info("INVENTORY received: orderId={}, product={}, qty={}",
                order.getOrderId(), order.getProduct(), order.getQuantity());

        try {
            if (order.getQuantity() <= 0) {
                rejectOrder(order, "Invalid quantity: " + order.getQuantity());
                return;
            }

            Optional<InventoryItemEntity> itemOpt =
                    inventoryItemRepository.findWithLockByProductIgnoreCase(order.getProduct());

            if (itemOpt.isEmpty()) {
                rejectOrder(order, "Product not found in catalog: " + order.getProduct());
                return;
            }

            InventoryItemEntity item = itemOpt.get();

            if (item.getAvailableQuantity() < order.getQuantity()) {
                rejectOrder(order, "Product unavailable - requested " + order.getQuantity()
                        + " but only " + item.getAvailableQuantity() + " in stock for " + item.getProduct());
                return;
            }

            // Enough stock -> reserve it
            item.setAvailableQuantity(item.getAvailableQuantity() - order.getQuantity());
            inventoryItemRepository.save(item);

            InventoryEntity reservation = new InventoryEntity();
            reservation.setOrderId(order.getOrderId());
            reservation.setProduct(order.getProduct());
            reservation.setQuantity(order.getQuantity());
            reservation.setStatus("RESERVED");
            reservation.setReason("Stock reserved successfully");
            inventoryRepository.save(reservation);

            log.info("Inventory reserved for orderId={} ({} left in stock for {})",
                    order.getOrderId(), item.getAvailableQuantity(), item.getProduct());

            order.setStatus("INVENTORY_CONFIRMED");
            jmsTemplate.convertAndSend(paymentQueue, order);
            processedCount.incrementAndGet();

            log.info("Inventory forwarded order {} to {}", order.getOrderId(), paymentQueue);

        } catch (Exception e) {
            log.error("Inventory failed to process orderId={}: {}", order.getOrderId(), e.getMessage());
            rejectOrder(order, "Unexpected inventory error: " + e.getMessage());
        }
    }

    private void rejectOrder(OrderMessage order, String reason) {
        InventoryEntity reservation = new InventoryEntity();
        reservation.setOrderId(order.getOrderId());
        reservation.setProduct(order.getProduct());
        reservation.setQuantity(order.getQuantity());
        reservation.setStatus("UNAVAILABLE");
        reservation.setReason(reason);
        reservation.setCreatedAt(LocalDateTime.now());
        inventoryRepository.save(reservation);

        order.setStatus("CANCELLED");
        order.setFailureReason(reason);
        jmsTemplate.convertAndSend(failedQueue, order);
        failedCount.incrementAndGet();

        log.warn("Order {} CANCELLED - {}", order.getOrderId(), reason);
    }

    public int getProcessedCount() {
        return processedCount.get();
    }

    public int getFailedCount() {
        return failedCount.get();
    }
}
