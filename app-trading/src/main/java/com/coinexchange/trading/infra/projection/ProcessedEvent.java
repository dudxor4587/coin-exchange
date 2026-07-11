package com.coinexchange.trading.infra.projection;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 이미 projection한 로그 이벤트의 id. at-least-once 재소비 시 중복 반영을 막는다.
 * 이벤트 처리와 같은 트랜잭션에서 이 행이 INSERT되므로, "DB 반영은 됐는데
 * dedup 마커는 없음" 같은 어긋난 상태가 생기지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor
public class ProcessedEvent {

    @Id
    private UUID eventId;

    public ProcessedEvent(UUID eventId) {
        this.eventId = eventId;
    }
}
