package com.coinexchange.matching.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// 매칭엔진의 작업 상태. Redis 해시로만 저장되며 DB 엔티티가 아니다(진실은 durable 로그).
@Getter
@NoArgsConstructor
public class OrderBook {

    private Long id;
    private Status status;
    private Long coinId;
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
