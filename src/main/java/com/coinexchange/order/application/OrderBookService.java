package com.coinexchange.order.application;

import com.coinexchange.order.domain.OrderBook;
import com.coinexchange.order.domain.repository.RedisOrderBookRepository;
import com.coinexchange.order.event.BuyOrderReadyEvent;
import com.coinexchange.order.event.OrderBookRollbackEvent;
import com.coinexchange.order.event.SellOrderReadyEvent;
import com.coinexchange.order.exception.OrderBookException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.coinexchange.order.exception.OrderBookExceptionType.ORDER_BOOK_NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderBookService {

    private final RedisOrderBookRepository redisOrderBookRepository;

    @Transactional
    public void processBuyOrder(BuyOrderReadyEvent event) {
        OrderBook orderBook = OrderBook.builder()
                .id(event.orderId())
                .coinId(event.coinId())
                .price(event.price())
                .type(OrderBook.Type.BUY)
                .remainingAmount(event.amount())
                .userId(event.userId())
                .orderId(event.orderId())
                .build();

        redisOrderBookRepository.saveOrder(orderBook);
        log.info("매수 주문 등록 완료: orderId={}, coinId={}, price={}, amount={}",
                event.orderId(), event.coinId(), event.price(), event.amount());
    }

    @Transactional
    public void processSellOrder(SellOrderReadyEvent event) {
        OrderBook orderBook = OrderBook.builder()
                .id(event.orderId())
                .coinId(event.coinId())
                .price(event.price())
                .type(OrderBook.Type.SELL)
                .remainingAmount(event.amount())
                .userId(event.userId())
                .orderId(event.orderId())
                .build();

        redisOrderBookRepository.saveOrder(orderBook);
        log.info("매도 주문 등록 완료: orderId={}, coinId={}, price={}, amount={}",
                event.orderId(), event.coinId(), event.price(), event.amount());
    }

    public void rollbackOrderBook(OrderBookRollbackEvent event) {
        OrderBook buyOrder = redisOrderBookRepository.findById(event.buyOrderId())
                .orElseThrow(() -> new OrderBookException(ORDER_BOOK_NOT_FOUND));
        OrderBook sellOrder = redisOrderBookRepository.findById(event.sellOrderId())
                .orElseThrow(() -> new OrderBookException(ORDER_BOOK_NOT_FOUND));

        buyOrder.increaseAmount(event.matchedAmount());
        sellOrder.increaseAmount(event.matchedAmount());

        redisOrderBookRepository.saveOrder(buyOrder);
        redisOrderBookRepository.saveOrder(sellOrder);

        log.info("주문서 롤백 완료: 매수주문 ID={}, 매도주문 ID={}, 롤백수량={}",
                event.buyOrderId(), event.sellOrderId(), event.matchedAmount());
    }
}
