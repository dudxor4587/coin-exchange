package com.coinexchange.order.application;

import com.coinexchange.events.notification.NotificationRequestedEvent;
import com.coinexchange.order.domain.Order;
import com.coinexchange.order.domain.repository.OrderRepository;
import com.coinexchange.events.order.*;
import com.coinexchange.order.exception.OrderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.coinexchange.order.exception.OrderExceptionType.ORDER_NOT_FOUND;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void createBuyOrder(Long coinId,
                               BigDecimal price,
                               Long amount,
                               Long userId) {
        BigDecimal lockedFunds = price.multiply(BigDecimal.valueOf(amount));
        Order order = Order.builder()
                .coinId(coinId)
                .price(price)
                .orderAmount(amount)
                .filledAmount(0L)
                .lockedFunds(lockedFunds)
                .type(Order.Type.BUY)
                .userId(userId)
                .status(Order.Status.PENDING)
                .build();

        orderRepository.save(order);
        eventPublisher.publishEvent(new BuyOrderCreatedEvent(
                order.getId(),
                order.getUserId(),
                order.getLockedFunds(),
                order.getCoinId(),
                order.getPrice(),
                order.getOrderAmount()
        ));
    }

    @Transactional
    public void handleOrderProcessingFailure(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ORDER_NOT_FOUND));

        order.updateFailedInfo(reason);
        orderRepository.save(order);

        eventPublisher.publishEvent(new NotificationRequestedEvent(
                order.getUserId(),
                "거래소에 요청하신 주문이 실패하였습니다. 사유: " + reason
        ));
    }

    @Transactional
    public void processOrderMatch(OrderMatchedEvent event) {
        Order buyOrder = orderRepository.findById(event.buyOrderId())
                .orElseThrow(() -> new OrderException(ORDER_NOT_FOUND));
        Order sellOrder = orderRepository.findById(event.sellOrderId())
                .orElseThrow(() -> new OrderException(ORDER_NOT_FOUND));

        buyOrder.fill(event.matchedAmount());
        sellOrder.fill(event.matchedAmount());

        orderRepository.save(buyOrder);
        orderRepository.save(sellOrder);

        eventPublisher.publishEvent(new NotificationRequestedEvent(
                buyOrder.getUserId(),
                String.format("매수 완료: 매수자 ID: %d, 매칭 금액: %d, 코인 ID: %d, 가격: %s",
                        buyOrder.getUserId(), event.matchedAmount(), buyOrder.getCoinId(), buyOrder.getPrice())
        ));
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                sellOrder.getUserId(),
                String.format("매칭 완료: 매도자 ID: %d, 매칭 금액: %d, 코인 ID: %d, 가격: %s",
                        sellOrder.getUserId(), event.matchedAmount(), buyOrder.getCoinId(), buyOrder.getPrice())
        ));
    }

    @Transactional
    public void createSellOrder(Long coinId,
                                BigDecimal price,
                                Long amount,
                                Long userId) {
        Order order = Order.builder()
                .coinId(coinId)
                .price(price)
                .orderAmount(amount)
                .filledAmount(0L)
                .type(Order.Type.SELL)
                .userId(userId)
                .status(Order.Status.PENDING)
                .build();

        orderRepository.save(order);
        eventPublisher.publishEvent(new SellOrderCreatedEvent(
                order.getId(),
                order.getUserId(),
                order.getCoinId(),
                order.getPrice(),
                order.getOrderAmount()
        ));
    }
}
