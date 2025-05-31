package com.coinexchange.order.application.listener;


import com.coinexchange.order.application.OrderBookService;
import com.coinexchange.order.event.BuyOrderReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.BUY_ORDER_READY_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class BuyOrderReadyEventListener {

    private final OrderBookService orderBookService;

    @RabbitListener(queues = BUY_ORDER_READY_QUEUE)
    public void handleBuyOrderReadyEvent(BuyOrderReadyEvent event) {
        log.info("매수 요청 이벤트 수신: orderId={}, coinId={}, price={}, amount={}",
                event.orderId(), event.coinId(), event.price(), event.amount());

        orderBookService.processBuyOrder(event);
    }

}
