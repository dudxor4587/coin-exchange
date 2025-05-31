package com.coinexchange.wallet.application.listener;

import com.coinexchange.order.event.SellOrderFilledEvent;
import com.coinexchange.wallet.application.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.coinexchange.common.config.RabbitMQConfig.SELL_ORDER_FILLED_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class SellOrderFilledEventListener {

    private final WalletService walletService;

    @RabbitListener(queues = SELL_ORDER_FILLED_QUEUE)
    public void handleSellOrderFilled(SellOrderFilledEvent event) {
        log.info("매도 주문 체결 이벤트 수신: userId={}, orderId={}, price={}",
                event.userId(), event.sellOrderId(), event.price());
        walletService.processSellOrderFill(event.userId(), event.price());
    }
}
