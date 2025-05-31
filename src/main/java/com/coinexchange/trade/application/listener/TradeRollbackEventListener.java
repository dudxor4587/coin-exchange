package com.coinexchange.trade.application.listener;

import com.coinexchange.order.event.TradeRollbackEvent;
import com.coinexchange.trade.application.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.TRADE_ROLLBACK_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeRollbackEventListener {

    private final TradeService tradeService;

    @RabbitListener(queues = TRADE_ROLLBACK_QUEUE)
    public void handleOrderMatchFailedEvent(TradeRollbackEvent event) {
        log.error("주문 매칭 실패 이벤트 수신: tradeId={}, 이유={}", event.tradeId(), event.reason());
        tradeService.handleOrderMatchFailure(event.tradeId(), event.reason());
    }
}
