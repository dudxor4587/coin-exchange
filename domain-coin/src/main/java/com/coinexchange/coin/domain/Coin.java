package com.coinexchange.coin.domain;

import com.coinexchange.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Coin extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Symbol symbol;

    private String name;

    public enum Symbol {
        BTC, ETH
    }

    @Builder
    public Coin(Symbol symbol, String name) {
        this.symbol = symbol;
        this.name = name;
    }
}
