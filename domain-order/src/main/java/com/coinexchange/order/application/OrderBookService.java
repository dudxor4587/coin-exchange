package com.coinexchange.order.application;

import com.coinexchange.events.order.BuyOrderReadyEvent;
import com.coinexchange.order.event.OrderBookRollbackEvent;
import com.coinexchange.events.order.SellOrderReadyEvent;

public interface OrderBookService {

    void processBuyOrder(BuyOrderReadyEvent event);

    void processSellOrder(SellOrderReadyEvent event);

    void rollbackOrderBook(OrderBookRollbackEvent event);
}
