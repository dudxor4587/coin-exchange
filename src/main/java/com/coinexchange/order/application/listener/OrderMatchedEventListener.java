package com.coinexchange.order.application.listener;

import com.coinexchange.order.application.OrderService;
import com.coinexchange.order.event.OrderBookRollbackEvent;
import com.coinexchange.order.event.OrderMatchedEvent;
import com.coinexchange.order.event.TradeRollbackEvent;
import com.coinexchange.order.exception.OrderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.ORDER_MATCHED_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMatchedEventListener {

    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    @RabbitListener(queues = ORDER_MATCHED_QUEUE)
    public void handleOrderMatchedEvent(OrderMatchedEvent event) {
        log.info("주문 매칭 이벤트 수신: 매수주문 ID={}, 매도주문 ID={}, 체결수량={}",
                event.buyOrderId(), event.sellOrderId(), event.matchedAmount());
        try {
            orderService.processOrderMatch(event);
        } catch (OrderException e) {
            log.error("주문 매칭 실패, 복구 작업 시작: {}", e.getMessage());
            eventPublisher.publishEvent(new TradeRollbackEvent(
                    event.tradeId(),
                    e.getMessage()
            ));
            eventPublisher.publishEvent(new OrderBookRollbackEvent(
                    event.buyOrderId(),
                    event.sellOrderId(),
                    event.matchedAmount()
            ));
        }
    }
}
