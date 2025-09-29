package com.coinexchange.order.domain;

import com.coinexchange.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class OrderBook extends BaseTimeEntity {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long coinId;

    @Enumerated(EnumType.STRING)
    private Type type;

    private BigDecimal price;

    private Long remainingAmount;

    private Long userId;

    private Long orderId;

    public enum Type {
        BUY, SELL
    }

    @Builder
    public OrderBook(Long id, Long coinId, Type type, BigDecimal price, Long remainingAmount, Long userId, Long orderId) {
        this.id = id;
        this.coinId = coinId;
        this.type = type;
        this.price = price;
        this.remainingAmount = remainingAmount;
        this.userId = userId;
        this.orderId = orderId;
    }

    public void decreaseAmount(Long filled) {
        this.remainingAmount -= filled;
    }

    public void increaseAmount(Long filled) {
        this.remainingAmount += filled;
    }

    public boolean isEmpty() {
        return this.remainingAmount <= 0;
    }
}
