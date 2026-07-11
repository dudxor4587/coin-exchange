package com.coinexchange.order.domain;

import com.coinexchange.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "`order`")
public class Order extends BaseTimeEntity implements Persistable<Long> {

    // id는 DB auto-increment가 아니라 Redis INCR로 애플리케이션이 채번한다.
    // 주문 저장(DB)이 hot path에서 비동기로 빠지면서, OrderBook(Redis) 등록 시점에
    // DB 없이 id가 필요해졌기 때문이다.
    @Id
    private Long id;

    private Long userId;

    private Long coinId;

    private BigDecimal price;

    private Long orderAmount;

    private Long filledAmount;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(nullable = true)
    private BigDecimal lockedFunds;

    @Column(nullable = true)
    private String failedReason;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        PENDING, PARTIAL, FILLED, CANCELLED, FAILED
    }

    public enum Type {
        BUY, SELL
    }

    @Builder
    public Order(Long id,
                 Long userId,
                 Long coinId,
                 BigDecimal price,
                 Long orderAmount,
                 Long filledAmount,
                 Type type,
                 BigDecimal lockedFunds,
                 Status status) {
        this.id = id;
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
        if (isFilled()) {
            this.status = Status.FILLED;
            return;
        }
        this.status = Status.PARTIAL;
    }

    private boolean isFilled() {
        return this.filledAmount.equals(this.orderAmount);
    }

    // 채번된 id를 가진 새 엔티티를 save()할 때 Spring Data JPA가 merge(SELECT 후 INSERT)로
    // 빠지지 않도록, 아직 auditing이 찍히지 않은 엔티티를 신규로 판별한다.
    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }
}
