package com.coinexchange.order.domain;

import com.coinexchange.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor
public class OrderBook extends BaseTimeEntity {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private Status status;

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

    public enum Status {
        ACTIVE, COMPLETED
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
        this.status = Status.ACTIVE;
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

    public void complete() {
        this.status = Status.COMPLETED;
    }
}
