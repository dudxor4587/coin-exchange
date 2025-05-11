package com.coinexchange.order.domain;

import com.coinexchange.coin.domain.Coin;
import com.coinexchange.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Getter
public class Trade extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Order order;

    @ManyToOne
    private Coin coin;

    private BigDecimal price;

    private BigDecimal amount;
}
