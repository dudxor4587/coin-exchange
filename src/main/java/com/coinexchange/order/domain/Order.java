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
@Table(name = "`order`")
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long coinId;

    private BigDecimal price;

    private Long orderAmount;

    private Long filledAmount;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(nullable = true)
    private BigDecimal lockedFunds; // 사용자 예치금

    @Column(nullable = true)
    private String failedReason; // 주문 실패 사유

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        PENDING, PARTIAL, FILLED, CANCELLED, FAILED
    }

    public enum Type {
        BUY, SELL
    }

    @Builder
    public Order(Long userId,
                 Long coinId,
                 BigDecimal price,
                 Long orderAmount,
                 Long filledAmount,
                 Type type,
                 BigDecimal lockedFunds,
                 Status status) {
        this.userId = userId;
        this.coinId = coinId;
        this.price = price;
        this.orderAmount = orderAmount;
        this.filledAmount = filledAmount;
        this.type = type;
        this.lockedFunds = lockedFunds;
        this.status = status;
    }

    public void updateFailedInfo(String reason) {
        this.failedReason = reason;
        this.status = Status.FAILED;
    }
}
