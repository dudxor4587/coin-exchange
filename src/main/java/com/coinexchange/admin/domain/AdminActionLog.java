package com.coinexchange.admin.domain;

import com.coinexchange.coin.domain.Coin;
import com.coinexchange.common.domain.BaseTimeEntity;
import com.coinexchange.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class AdminActionLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @ManyToOne
    private User targetUser;

    @ManyToOne
    private Coin coin;

    private String detail;

    public enum ActionType {
        COIN_LISTING, COIN_DELISTING, WALLET_ADJUST
    }
}
