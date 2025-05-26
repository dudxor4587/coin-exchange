package com.coinexchange.deposit.domain;

import com.coinexchange.common.domain.BaseTimeEntity;
import com.coinexchange.deposit.exception.DepositException;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static com.coinexchange.deposit.exception.DepositExceptionType.DEPOSIT_STATUS_NOT_PENDING;

@Entity
@Getter
@NoArgsConstructor
public class Deposit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private BigDecimal amount;

    private String bank;

    private String accountNumber;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = true)
    private String rejectReason;

    public enum Status {
        PENDING, COMPLETED, REJECTED
    }

    @Builder
    public Deposit(Long userId, BigDecimal amount, String bank, String accountNumber, Status status) {
        this.userId = userId;
        this.amount = amount;
        this.bank = bank;
        this.accountNumber = accountNumber;
        this.status = status;
    }

    public void approve() {
        if (this.status != Status.PENDING) {
            throw new DepositException(DEPOSIT_STATUS_NOT_PENDING);
        }
        this.status = Status.COMPLETED;
    }

    public void reject(String reason) {
        if (this.status != Status.PENDING) {
            throw new DepositException(DEPOSIT_STATUS_NOT_PENDING);
        }
        this.status = Status.REJECTED;
        this.rejectReason = reason;
    }
}
