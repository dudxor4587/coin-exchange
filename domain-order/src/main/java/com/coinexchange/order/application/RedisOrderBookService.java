package com.coinexchange.order.application;

import com.coinexchange.order.domain.OrderBook;
import com.coinexchange.order.domain.repository.RedisOrderBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "matching.engine", havingValue = "redis", matchIfMissing = true)
public class RedisOrderBookService implements OrderBookService {

    private final RedisOrderBookRepository redisOrderBookRepository;

    // 매칭엔진은 DB Order 엔티티를 모른다. 주문의 raw 필드만 받아 Redis OrderBook에 등록한다.
    @Override
    public void placeOrder(Long orderId, Long coinId, BigDecimal price, Long amount, String side, Long userId) {
        OrderBook.Type type = "BUY".equals(side) ? OrderBook.Type.BUY : OrderBook.Type.SELL;
        OrderBook orderBook = OrderBook.builder()
                .id(orderId)
                .coinId(coinId)
                .price(price)
                .type(type)
                .remainingAmount(amount)
                .userId(userId)
                .orderId(orderId)
                .build();

        redisOrderBookRepository.saveOrder(orderBook);
        log.info("{} 주문 등록 완료: orderId={}, coinId={}, price={}, amount={}",
                type == OrderBook.Type.BUY ? "매수" : "매도", orderId, coinId, price, amount);
    }
}
