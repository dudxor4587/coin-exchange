package com.coinexchange.order.application;

import com.coinexchange.order.domain.OrderBook;
import com.coinexchange.order.domain.repository.OrderBookRepository;
import com.coinexchange.trade.event.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingEngineService {

    private final OrderBookRepository orderBookRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 5000) // 5초마다 매칭 엔진 실행
    @Transactional
    public void matchOrders() {
        List<OrderBook> buyOrders = orderBookRepository
                .findByStatusAndTypeAndRemainingAmountGreaterThanOrderByCreatedAtAsc(OrderBook.Status.ACTIVE, OrderBook.Type.BUY, 0L);
        List<OrderBook> sellOrders = orderBookRepository
                .findByStatusAndTypeAndRemainingAmountGreaterThanOrderByCreatedAtAsc(OrderBook.Status.ACTIVE, OrderBook.Type.SELL, 0L);

        for (OrderBook buy : buyOrders) {
            for (OrderBook sell : sellOrders) {
                if ((buy.getPrice().compareTo(sell.getPrice()) == 0) && !buy.getUserId().equals(sell.getUserId())) {
                    Long matchedAmount = Math.min(buy.getRemainingAmount(), sell.getRemainingAmount());

                    buy.decreaseAmount(matchedAmount);
                    sell.decreaseAmount(matchedAmount);

                    orderBookRepository.save(buy);
                    orderBookRepository.save(sell);

                    log.info("매칭 체결: 매수호가 ID={}, 매도호가 ID={}, 체결수량={}, 가격={}",
                            buy.getId(), sell.getId(), matchedAmount, buy.getPrice());

                    eventPublisher.publishEvent(new TradeCreatedEvent(
                            buy.getId(),
                            sell.getId(),
                            buy.getUserId(),
                            sell.getUserId(),
                            buy.getCoinId(),
                            buy.getPrice(),
                            matchedAmount
                    ));
                    if (buy.isEmpty()) {
                        buy.complete();
                    }
                    if (sell.isEmpty()) {
                        sell.complete();
                    }
                }
            }
        }
    }
}
