package com.coinexchange.order.domain;

import com.coinexchange.coin.domain.Coin;
import com.coinexchange.common.domain.BaseTimeEntity;
import com.coinexchange.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Getter
@Table(name = "`order`")
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Coin coin;

    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    private BigDecimal price;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum OrderType {
        LIMIT, MARKET
    }

    public enum Status {
        PENDING, PARTIAL, FILLED, CANCELLED
    }
}
