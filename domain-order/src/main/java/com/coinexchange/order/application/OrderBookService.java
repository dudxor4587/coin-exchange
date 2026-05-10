package com.coinexchange.order.application;

import com.coinexchange.order.domain.Order;
import com.coinexchange.order.event.OrderBookRollbackEvent;

public interface OrderBookService {

    void placeOrder(Order order);

    void rollbackOrderBook(OrderBookRollbackEvent event);
}
