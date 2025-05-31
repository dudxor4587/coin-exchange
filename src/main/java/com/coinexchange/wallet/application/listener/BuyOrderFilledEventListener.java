package com.coinexchange.wallet.application.listener;

import com.coinexchange.order.event.BuyOrderFilledEvent;
import com.coinexchange.wallet.application.CoinWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.BUY_ORDER_FILLED_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class BuyOrderFilledEventListener {

    private final CoinWalletService coinWalletService;

    @RabbitListener(queues = BUY_ORDER_FILLED_QUEUE)
    public void handleBuyOrderFilled(BuyOrderFilledEvent event) {
        log.info("매수 주문 체결 이벤트 수신: userId={}, orderId={}, amount={}",
                event.userId(), event.buyOrderId(), event.amount());
        coinWalletService.processBuyOrderFill(event.userId(), event.coinId(), event.amount());
    }
}
