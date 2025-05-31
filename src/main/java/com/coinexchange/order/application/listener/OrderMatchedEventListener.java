package com.coinexchange.order.application.listener;

import com.coinexchange.order.application.OrderService;
import com.coinexchange.order.event.OrderMatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.ORDER_MATCHED_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMatchedEventListener {

    private final OrderService orderService;

    @RabbitListener(queues = ORDER_MATCHED_QUEUE)
    public void handleOrderMatchedEvent(OrderMatchedEvent event) {
        log.info("주문 매칭 이벤트 수신: 매수주문 ID={}, 매도주문 ID={}, 체결수량={}",
                event.buyOrderId(), event.sellOrderId(), event.matchedAmount());
        orderService.processOrderMatch(event);
    }
}
