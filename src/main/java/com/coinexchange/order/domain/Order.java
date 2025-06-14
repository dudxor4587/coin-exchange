package com.coinexchange.order.domain;

import com.coinexchange.common.domain.BaseTimeEntity;
import com.coinexchange.order.event.BuyOrderCompletedEvent;
import com.coinexchange.order.event.BuyOrderFilledEvent;
import com.coinexchange.order.event.SellOrderCompletedEvent;
import com.coinexchange.order.event.SellOrderFilledEvent;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "`order`")
public class Order extends BaseTimeEntity {

    @Transient
    private final List<Object> domainEvents = new ArrayList<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long coinId;

    private BigDecimal price;

    private Long orderAmount;

    private Long filledAmount;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(nullable = true)
    private BigDecimal lockedFunds; // 사용자 예치금

    @Column(nullable = true)
    private String failedReason; // 주문 실패 사유

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        PENDING, PARTIAL, FILLED, CANCELLED, FAILED
    }

    public enum Type {
        BUY, SELL
    }

    @Builder
    public Order(Long userId,
                 Long coinId,
                 BigDecimal price,
                 Long orderAmount,
                 Long filledAmount,
                 Type type,
                 BigDecimal lockedFunds,
                 Status status) {
        this.userId = userId;
        this.coinId = coinId;
        this.price = price;
        this.orderAmount = orderAmount;
        this.filledAmount = filledAmount;
        this.type = type;
        this.lockedFunds = lockedFunds;
        this.status = status;
    }

    public void updateFailedInfo(String reason) {
        this.failedReason = reason;
        this.status = Status.FAILED;
    }

    public void fill(Long amount) {
        this.filledAmount += amount;
        registerFillEvent(amount);

        if (isFilled()) {
            this.status = Status.FILLED;
            registerCompletionEvent();
            return;
        }
        this.status = Status.PARTIAL;
    }

    private void registerFillEvent(Long amount) {
        if (this.type == Type.BUY) {
            domainEvents.add(new BuyOrderFilledEvent(this.id, this.userId, this.coinId, amount));
        } else {
            domainEvents.add(new SellOrderFilledEvent(this.id, this.userId, this.price.multiply(BigDecimal.valueOf(amount))));
        }
    }

    private void registerCompletionEvent() {
        if (this.type == Type.BUY) {
            domainEvents.add(new BuyOrderCompletedEvent(this.id, this.userId, this.coinId, this.filledAmount));
        } else {
            domainEvents.add(new SellOrderCompletedEvent(this.id, this.userId, this.coinId, this.filledAmount));
        }
    }

    private boolean isFilled() {
        return this.filledAmount.equals(this.orderAmount);
    }

    @DomainEvents
    protected List<Object> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    @AfterDomainEventPublication
    protected void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
