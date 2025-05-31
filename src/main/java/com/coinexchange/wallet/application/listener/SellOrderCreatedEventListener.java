package com.coinexchange.wallet.application.listener;

import com.coinexchange.order.event.OrderProcessingFailedEvent;
import com.coinexchange.order.event.SellOrderCreatedEvent;
import com.coinexchange.wallet.application.CoinWalletService;
import com.coinexchange.wallet.exception.CoinWalletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.SELL_ORDER_CREATED_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class SellOrderCreatedEventListener {

    private final CoinWalletService coinWalletService;
    private final ApplicationEventPublisher eventPublisher;

    @RabbitListener(queues = SELL_ORDER_CREATED_QUEUE)
    public void handleSellOrderCreated(SellOrderCreatedEvent event) {
        log.info("매도 주문 생성 이벤트 수신: userId={}, price={}, amount={}", event.userId(), event.price(), event.amount());
        try {
            coinWalletService.processSellOrder(event.userId(), event.orderId(), event.coinId(), event.price(), event.amount());
        } catch (CoinWalletException e) {
            log.warn("매도 주문 처리 실패: {}", e.getMessage());
            eventPublisher.publishEvent(new OrderProcessingFailedEvent(
                    event.orderId(),
                    e.getMessage()
            ));
        }
    }

}
