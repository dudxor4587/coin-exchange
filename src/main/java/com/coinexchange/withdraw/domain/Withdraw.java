package com.coinexchange.withdraw.domain;

import com.coinexchange.common.domain.BaseTimeEntity;
import com.coinexchange.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Getter
public class Withdraw extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        PENDING, COMPLETED, CANCELLED, REJECTED
    }
}
