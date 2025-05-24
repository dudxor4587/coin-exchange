package com.coinexchange.withdraw.domain;

import com.coinexchange.common.domain.BaseTimeEntity;
import com.coinexchange.user.domain.User;
import com.coinexchange.withdraw.exception.WithdrawException;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static com.coinexchange.withdraw.exception.WithdrawExceptionType.WITHDRAW_STATUS_NOT_PENDING;

@Entity
@Getter
@NoArgsConstructor
public class Withdraw extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String bank;

    private String accountNumber;

    @Column(nullable = true)
    private String rejectReason;

    @Column(nullable = true)
    private String failureReason;

    public enum Status {
        PENDING, COMPLETED, REJECTED, FAILED
    }

    @Builder
    public Withdraw(User user, BigDecimal amount, String bank, String accountNumber, Withdraw.Status status) {
        this.user = user;
        this.amount = amount;
        this.bank = bank;
        this.accountNumber = accountNumber;
        this.status = status;
    }

    public void approve() {
        validatePendingStatus();
        this.status = Withdraw.Status.COMPLETED;
    }

    public void reject(String reason) {
        validatePendingStatus();
        this.status = Withdraw.Status.REJECTED;
        this.rejectReason = reason;
    }

    public void fail(String reason) {
        this.status = Withdraw.Status.FAILED;
        this.failureReason = reason;
    }

    private void validatePendingStatus() {
        if (this.status != Withdraw.Status.PENDING) {
            throw new WithdrawException(WITHDRAW_STATUS_NOT_PENDING);
        }
    }
}
