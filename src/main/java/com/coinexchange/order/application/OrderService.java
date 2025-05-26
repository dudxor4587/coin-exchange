package com.coinexchange.order.application;

import com.coinexchange.notification.application.NotificationService;
import com.coinexchange.order.domain.Order;
import com.coinexchange.order.domain.repository.OrderRepository;
import com.coinexchange.order.event.BuyOrderCreatedEvent;
import com.coinexchange.order.exception.OrderException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.coinexchange.order.exception.OrderExceptionType.ORDER_NOT_FOUND;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

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
                order.getLockedFunds()
        ));
    }

    @Transactional
    public void handleOrderProcessingFailure(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ORDER_NOT_FOUND));

        order.updateFailedInfo(reason);
        orderRepository.save(order);

        notificationService.sendOrderFailureNotification(
                order.getUserId(),
                reason
        );
    }
}
