package com.coinexchange.wallet.application.listener;

import com.coinexchange.order.event.SellOrderCompletedEvent;
import com.coinexchange.wallet.application.CoinWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.SELL_ORDER_COMPLETED_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class SellOrderCompletedEventListener {

    private final CoinWalletService coinWalletService;

    @RabbitListener(queues = SELL_ORDER_COMPLETED_QUEUE)
    public void handleBuyOrderCompleted(SellOrderCompletedEvent event) {
        log.info("매도 주문 완료 이벤트 수신: userId={}, orderId={}, amount={}",
                event.userId(), event.orderId(), event.amount());
        coinWalletService.processSellOrderCompletion(event.userId(), event.coinId(), event.amount());
    }
}
