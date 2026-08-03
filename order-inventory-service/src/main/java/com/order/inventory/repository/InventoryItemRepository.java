package com.order.inventory.repository;

import com.order.inventory.entity.InventoryItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItemEntity, Long> {

    Optional<InventoryItemEntity> findByProductIgnoreCase(String product);

    // Pessimistic lock so two concurrent orders can't both "see" the same
    // stock as available and both get reserved (classic overselling bug).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItemEntity> findWithLockByProductIgnoreCase(String product);
}
