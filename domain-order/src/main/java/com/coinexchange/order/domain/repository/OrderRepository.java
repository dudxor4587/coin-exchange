package com.coinexchange.order.domain.repository;

import com.coinexchange.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
