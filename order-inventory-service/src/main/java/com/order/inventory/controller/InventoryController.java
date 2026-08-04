package com.order.inventory.controller;

import com.order.inventory.entity.InventoryEntity;
import com.order.inventory.entity.InventoryItemEntity;
import com.order.inventory.repository.InventoryItemRepository;
import com.order.inventory.repository.InventoryRepository;
import com.order.inventory.service.InventoryConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "http://localhost:3000")
public class InventoryController {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryConsumer inventoryConsumer;

    // The product catalog - what's actually in stock right now
    @GetMapping("/catalog")
    public List<InventoryItemEntity> catalog() {
        return inventoryItemRepository.findAll();
    }

    // Per-order reservation history (RESERVED / RELEASED / UNAVAILABLE)
    @GetMapping("/records")
    public List<InventoryEntity> getAll() {
        return inventoryRepository.findAll();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of("processed", inventoryConsumer.getProcessedCount(),
                "failed", inventoryConsumer.getFailedCount(), "status", "Inventory running");
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("service", "order-inventory-service", "status", "UP", "port", 8081);
    }
}
