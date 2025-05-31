package com.coinexchange.wallet.application.listener;

import com.coinexchange.order.event.BuyOrderCreatedEvent;
import com.coinexchange.order.event.OrderProcessingFailedEvent;
import com.coinexchange.wallet.application.WalletService;
import com.coinexchange.wallet.exception.WalletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.BUY_ORDER_CREATED_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class BuyOrderCreatedEventListener {

    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;

    @RabbitListener(queues = BUY_ORDER_CREATED_QUEUE)
    public void handleBuyOrderCreated(BuyOrderCreatedEvent event) {
        log.info("매수 주문 생성 이벤트 수신: userId={}, lockedFunds={}", event.userId(), event.lockedFunds());
        try {
            walletService.processBuyOrder(event.userId(), event.lockedFunds(), event.orderId(), event.coinId(), event.price(), event.amount());
        } catch (WalletException e) {
            log.warn("매수 주문 처리 실패: {}", e.getMessage());
            eventPublisher.publishEvent(new OrderProcessingFailedEvent(
                    event.orderId(),
                    e.getMessage()
            ));
        }
    }
}
