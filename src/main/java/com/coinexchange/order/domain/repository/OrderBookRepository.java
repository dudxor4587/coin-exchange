package com.coinexchange.order.domain.repository;

import com.coinexchange.order.domain.OrderBook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderBookRepository extends JpaRepository<OrderBook, Long> {

    List<OrderBook> findByStatusAndTypeAndRemainingAmountGreaterThanOrderByCreatedAtAsc(OrderBook.Status status, OrderBook.Type type, long l);
}
