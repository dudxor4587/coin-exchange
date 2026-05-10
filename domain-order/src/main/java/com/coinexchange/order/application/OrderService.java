package com.coinexchange.order.application;

import com.coinexchange.order.domain.Order;
import com.coinexchange.order.domain.repository.OrderRepository;
import com.coinexchange.order.exception.OrderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.coinexchange.order.exception.OrderExceptionType.ORDER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order createBuyOrder(Long coinId, BigDecimal price, Long amount, Long userId, BigDecimal lockedFunds) {
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
        return orderRepository.save(order);
    }

    @Transactional
    public Order createSellOrder(Long coinId, BigDecimal price, Long amount, Long userId) {
        Order order = Order.builder()
                .coinId(coinId)
                .price(price)
                .orderAmount(amount)
                .filledAmount(0L)
                .type(Order.Type.SELL)
                .userId(userId)
                .status(Order.Status.PENDING)
                .build();
        return orderRepository.save(order);
    }

    @Transactional
    public void fillOrder(Long orderId, Long amount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ORDER_NOT_FOUND));
        order.fill(amount);
        orderRepository.save(order);
    }
}
