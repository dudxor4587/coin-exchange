package com.coinexchange.order.application.listener;

import com.coinexchange.order.application.OrderBookService;
import com.coinexchange.order.event.SellOrderReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.SELL_ORDER_READY_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class SellOrderReadyEventListener {

    private final OrderBookService orderBookService;

    @RabbitListener(queues = SELL_ORDER_READY_QUEUE)
    public void handleSellOrderReadyEvent(SellOrderReadyEvent event) {
        log.info("매도 요청 이벤트 수신: orderId={}, coinId={}, price={}, amount={}",
                event.orderId(), event.coinId(), event.price(), event.amount());

        orderBookService.processSellOrder(event);
    }
}
