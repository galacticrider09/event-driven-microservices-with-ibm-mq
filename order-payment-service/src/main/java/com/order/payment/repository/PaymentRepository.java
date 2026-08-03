package com.order.payment.repository;
import com.order.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long>
{
    boolean existsByOrderId(String orderId);

    Optional<PaymentEntity> findByOrderId(String orderId);
}
