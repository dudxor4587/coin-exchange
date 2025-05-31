package com.coinexchange.trade.application;

import com.coinexchange.order.event.OrderMatchedEvent;
import com.coinexchange.trade.domain.Trade;
import com.coinexchange.trade.domain.repository.TradeRepository;
import com.coinexchange.trade.event.TradeCreatedEvent;
import com.coinexchange.trade.exception.TradeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.coinexchange.trade.exception.TradeExceptionType.TRADE_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final TradeRepository tradeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processTradeCreation(TradeCreatedEvent event) {
        Trade trade = Trade.builder()
                .buyOrderId(event.buyOrderId())
                .sellOrderId(event.sellOrderId())
                .coinId(event.coinId())
                .amount(event.amount())
                .price(event.price())
                .build();

        tradeRepository.save(trade);

        log.info("거래 정보 생성 완료: 매수 주문 ID={}, 매도 주문 ID={}, 체결 수량={}",
                event.buyOrderId(), event.sellOrderId(), event.amount());

        eventPublisher.publishEvent(new OrderMatchedEvent(
                trade.getId(),
                event.buyOrderId(),
                event.sellOrderId(),
                event.amount()
        ));
    }

    @Transactional
    public void handleOrderMatchFailure(Long tradeId, String reason) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new TradeException(TRADE_NOT_FOUND));

        trade.updateFailedInfo(reason);
        tradeRepository.save(trade);

        log.info("거래 정보 복구 완료: tradeId={}", tradeId);
    }
}
