package com.coinexchange.coin.domain;

import com.coinexchange.common.domain.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
