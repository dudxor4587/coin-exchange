package com.coinexchange.trade.application.listener;

import com.coinexchange.trade.application.TradeService;
import com.coinexchange.trade.event.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.TRADE_CREATED_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeCreatedEventListener {

    private final TradeService tradeService;

    @RabbitListener(queues = TRADE_CREATED_QUEUE)
    public void handleTradeCreatedEvent(TradeCreatedEvent event) {
        log.info("거래 생성 이벤트 수신: 매수 주문 ID={}, 매도 주문 ID={}, 체결 수량={}",
                event.buyOrderId(), event.sellOrderId(), event.amount());
        tradeService.processTradeCreation(event);
    }

}
