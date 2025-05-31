package com.coinexchange.order.application;

import com.coinexchange.order.domain.OrderBook;
import com.coinexchange.order.domain.repository.OrderBookRepository;
import com.coinexchange.order.event.BuyOrderReadyEvent;
import com.coinexchange.order.event.SellOrderReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderBookService {

    private final OrderBookRepository orderBookRepository;

    @Transactional
    public void processBuyOrder(BuyOrderReadyEvent event) {
        OrderBook orderBook = OrderBook.builder()
                .coinId(event.coinId())
                .price(event.price())
                .type(OrderBook.Type.BUY)
                .remainingAmount(event.amount())
                .userId(event.userId())
                .orderId(event.orderId())
                .build();

        orderBookRepository.save(orderBook);
        log.info("매수 주문 등록 완료: orderId={}, coinId={}, price={}, amount={}",
                event.orderId(), event.coinId(), event.price(), event.amount());
    }

    @Transactional
    public void processSellOrder(SellOrderReadyEvent event) {
        OrderBook orderBook = OrderBook.builder()
                .coinId(event.coinId())
                .price(event.price())
                .type(OrderBook.Type.SELL)
                .remainingAmount(event.amount())
                .userId(event.userId())
                .orderId(event.orderId())
                .build();

        orderBookRepository.save(orderBook);
        log.info("매도 주문 등록 완료: orderId={}, coinId={}, price={}, amount={}",
                event.orderId(), event.coinId(), event.price(), event.amount());
    }
}
