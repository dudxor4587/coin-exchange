package com.coinexchange.user.domain;

import com.coinexchange.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor
public class Wallet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    private BigDecimal balance;

    public enum Currency {
        KRW, BTC, ETH
    }

    @Builder
    public Wallet(User user, Currency currency) {
        this.user = user;
        this.currency = currency;
        this.balance = BigDecimal.ZERO;
    }

    public void increaseBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}
