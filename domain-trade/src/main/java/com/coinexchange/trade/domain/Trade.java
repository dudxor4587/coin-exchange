package com.coinexchange.trade.domain;

import com.coinexchange.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor
public class Trade extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long buyOrderId;

    private Long sellOrderId;

    private Long coinId;

    private BigDecimal price;

    private Long amount;

    @Column(nullable = true)
    private String failedReason; // 주문 실패 사유

    @Enumerated(EnumType.STRING)
    private Trade.Status status;

    public enum Status {
        SUCCESS, FAILED
    }

    @Builder
    public Trade(Long buyOrderId, Long sellOrderId, Long coinId, BigDecimal price, Long amount) {
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.coinId = coinId;
        this.price = price;
        this.amount = amount;
        this.status = Status.SUCCESS;
    }

    public void updateFailedInfo(String reason) {
        this.failedReason = reason;
        this.status = Trade.Status.FAILED;
    }
}
