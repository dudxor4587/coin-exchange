package com.coinexchange.wallet.domain;

import com.coinexchange.common.domain.BaseTimeEntity;
import com.coinexchange.wallet.exception.CoinWalletException;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import static com.coinexchange.wallet.exception.CoinWalletExceptionType.INSUFFICIENT_COIN_BALANCE;

@Entity
@Getter
@NoArgsConstructor
public class CoinWallet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long coinId;

    private Long amount;

    @Builder
    public CoinWallet(Long userId, Long coinId, Long amount) {
        this.userId = userId;
        this.coinId = coinId;
        this.amount = amount;
    }

    public void increaseAmount(Long amount) {
        this.amount += amount;
    }

    public void decreaseAmount(Long amount) {
        if (this.amount < amount) {
            throw new CoinWalletException(INSUFFICIENT_COIN_BALANCE);
        }
        this.amount -= amount;
    }
}
