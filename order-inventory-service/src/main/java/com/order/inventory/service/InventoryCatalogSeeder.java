package com.order.inventory.service;

import com.order.inventory.entity.InventoryItemEntity;
import com.order.inventory.repository.InventoryItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads a realistic product catalog the first time the service starts
 * against an empty database. Most items have healthy stock so the happy
 * path works; a couple are intentionally scarce/zero so you can trigger
 * "Product unavailable" for the failure/compensation demo just by
 * ordering them (or ordering a large quantity of a low-stock item).
 */
@Component
@Slf4j
public class InventoryCatalogSeeder implements CommandLineRunner {

    private final InventoryItemRepository inventoryItemRepository;

    public InventoryCatalogSeeder(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    public void run(String... args) {
        if (inventoryItemRepository.count() > 0) {
            log.info("Inventory catalog already seeded ({} products), skipping.",
                    inventoryItemRepository.count());
            return;
        }

        Map<String, Object[]> catalog = new LinkedHashMap<>();
        // product -> { stockQty, price }
        catalog.put("Laptop", new Object[]{25, 899.99});
        catalog.put("Wireless Mouse", new Object[]{100, 19.99});
        catalog.put("Mechanical Keyboard", new Object[]{60, 79.99});
        catalog.put("27-inch Monitor", new Object[]{15, 229.99});
        catalog.put("Noise Cancelling Headphones", new Object[]{40, 149.99});
        catalog.put("Webcam", new Object[]{35, 49.99});
        catalog.put("USB-C Hub", new Object[]{80, 29.99});
        catalog.put("External SSD 1TB", new Object[]{20, 109.99});
        catalog.put("Smartphone", new Object[]{10, 699.99});
        catalog.put("Tablet", new Object[]{12, 399.99});
        catalog.put("Smartwatch", new Object[]{18, 199.99});
        catalog.put("Office Chair", new Object[]{8, 249.99});
        catalog.put("Standing Desk", new Object[]{5, 349.99});
        catalog.put("Bluetooth Speaker", new Object[]{50, 59.99});
        catalog.put("Portable Charger", new Object[]{75, 24.99});
        catalog.put("Gaming Console", new Object[]{6, 499.99});
        catalog.put("4K Action Camera", new Object[]{9, 249.99});
        catalog.put("Router", new Object[]{22, 89.99});
        // Deliberately scarce / out-of-stock so "Product unavailable" is easy to demo
        catalog.put("Limited Edition GPU", new Object[]{2, 1499.99});
        catalog.put("Discontinued Printer", new Object[]{0, 129.99});

        catalog.forEach((product, info) -> {
            InventoryItemEntity item = new InventoryItemEntity();
            item.setProduct(product);
            item.setAvailableQuantity((Integer) info[0]);
            item.setPrice((Double) info[1]);
            inventoryItemRepository.save(item);
        });

        log.info("Seeded inventory catalog with {} products.", catalog.size());
    }
}
