package com.coinexchange.order.application;

import com.coinexchange.order.domain.Order;
import com.coinexchange.order.domain.OrderBook;
import com.coinexchange.order.domain.repository.OrderBookRepository;
import com.coinexchange.order.event.OrderBookRollbackEvent;
import com.coinexchange.order.exception.OrderBookException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.coinexchange.order.exception.OrderBookExceptionType.ORDER_BOOK_NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "matching.engine", havingValue = "db")
public class JpaOrderBookService implements OrderBookService {

    private final OrderBookRepository orderBookRepository;

    @Override
    @Transactional
    public void placeOrder(Order order) {
        OrderBook.Type type = order.getType() == Order.Type.BUY ? OrderBook.Type.BUY : OrderBook.Type.SELL;
        OrderBook orderBook = OrderBook.builder()
                .id(order.getId())
                .coinId(order.getCoinId())
                .price(order.getPrice())
                .type(type)
                .remainingAmount(order.getOrderAmount())
                .userId(order.getUserId())
                .orderId(order.getId())
                .build();

        orderBookRepository.save(orderBook);
        log.info("{} 주문 등록 완료: orderId={}, coinId={}, price={}, amount={}",
                type == OrderBook.Type.BUY ? "매수" : "매도",
                order.getId(), order.getCoinId(), order.getPrice(), order.getOrderAmount());
    }

    @Override
    @Transactional
    public void rollbackOrderBook(OrderBookRollbackEvent event) {
        OrderBook buyOrder = orderBookRepository.findById(event.buyOrderId())
                .orElseThrow(() -> new OrderBookException(ORDER_BOOK_NOT_FOUND));
        OrderBook sellOrder = orderBookRepository.findById(event.sellOrderId())
                .orElseThrow(() -> new OrderBookException(ORDER_BOOK_NOT_FOUND));

        buyOrder.increaseAmount(event.matchedAmount());
        sellOrder.increaseAmount(event.matchedAmount());

        log.info("주문서 롤백 완료: 매수주문 ID={}, 매도주문 ID={}, 롤백수량={}",
                event.buyOrderId(), event.sellOrderId(), event.matchedAmount());
    }
}
