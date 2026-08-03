package com.order.inventory.repository;
import com.order.inventory.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryEntity, Long>
{
    Optional<InventoryEntity> findByOrderId(String orderId);
}
