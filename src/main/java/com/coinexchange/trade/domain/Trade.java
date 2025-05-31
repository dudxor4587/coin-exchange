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

    @Builder
    public Trade(Long buyOrderId, Long sellOrderId, Long coinId, BigDecimal price, Long amount) {
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.coinId = coinId;
        this.price = price;
        this.amount = amount;
    }
}
