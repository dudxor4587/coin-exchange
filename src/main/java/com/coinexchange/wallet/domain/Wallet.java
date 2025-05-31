package com.coinexchange.wallet.domain;

import com.coinexchange.common.domain.BaseTimeEntity;
import com.coinexchange.wallet.exception.WalletException;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static com.coinexchange.wallet.exception.WalletExceptionType.INSUFFICIENT_BALANCE;

@Entity
@Getter
@NoArgsConstructor
public class Wallet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    private BigDecimal balance;

    public enum Currency {
        KRW
    }

    @Builder
    public Wallet(Long userId, Currency currency) {
        this.userId = userId;
        this.currency = currency;
        this.balance = BigDecimal.ZERO;
    }

    public void increaseBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void decreaseBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new WalletException(INSUFFICIENT_BALANCE);
        }
        this.balance = this.balance.subtract(amount);
    }
}
