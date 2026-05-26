package com.petgym.repository;

import com.petgym.domain.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByAlfaOrderId(String alfaOrderId);

    Optional<PaymentOrder> findByOrderNumber(String orderNumber);
}
