package com.coinexchange.order.application.listener;

import com.coinexchange.order.application.OrderBookService;
import com.coinexchange.order.event.OrderBookRollbackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.ORDER_BOOK_ROLLBACK_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderBookRollbackEventListener {

    private final OrderBookService orderBookService;

    @RabbitListener(queues = ORDER_BOOK_ROLLBACK_QUEUE)
    public void handleOrderBookRollbackEvent(OrderBookRollbackEvent event) {
        log.info("주문서 롤백 이벤트 수신: 매수주문 ID={}, 매도주문 ID={}, 체결수량={}",
                event.buyOrderId(), event.sellOrderId(), event.matchedAmount());
        orderBookService.rollbackOrderBook(event);
    }
}
