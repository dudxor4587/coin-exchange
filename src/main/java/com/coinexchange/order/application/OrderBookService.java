package com.coinexchange.order.application;

import com.coinexchange.order.event.BuyOrderReadyEvent;
import com.coinexchange.order.event.OrderBookRollbackEvent;
import com.coinexchange.order.event.SellOrderReadyEvent;

public interface OrderBookService {

    void processBuyOrder(BuyOrderReadyEvent event);

    void processSellOrder(SellOrderReadyEvent event);

    void rollbackOrderBook(OrderBookRollbackEvent event);
}
